#!/usr/bin/env bash
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
ACAO="${1:-iniciar}"
case "$ACAO" in
  configurar|banco|parar-banco|iniciar|testar) ;;
  *) echo "Uso: $0 {configurar|banco|parar-banco|iniciar|testar}" >&2; exit 2 ;;
esac

if [[ -z "${PG_BIN:-}" ]]; then
  if command -v pg_ctl >/dev/null 2>&1; then
    PG_BIN="$(dirname "$(command -v pg_ctl)")"
  elif command -v brew >/dev/null 2>&1; then
    PG_BIN="$(brew --prefix postgresql@16)/bin"
  else
    echo "Instale PostgreSQL 16 e configure PG_BIN com o diretório dos executáveis." >&2
    exit 1
  fi
fi
for ferramenta in initdb pg_ctl psql createdb; do
  [[ -x "$PG_BIN/$ferramenta" ]] || { echo "Executável ausente: $PG_BIN/$ferramenta" >&2; exit 1; }
done

PERFIL=desenvolvimento
PORTA=5433
if [[ "$ACAO" == testar ]]; then PERFIL=testes; PORTA=55433; fi
LOCAL="$RAIZ/.local/$PERFIL"
DADOS="$LOCAL/postgres"
umask 077
mkdir -p "$LOCAL/socket"
if [[ ! -f "$LOCAL/ambiente.env" ]]; then
  SENHA="$(openssl rand -hex 24)"
  cat > "$LOCAL/ambiente.env" <<EOF
export DB_USER=postgres
export DB_PASSWORD=$SENHA
export DB_URL=jdbc:postgresql://127.0.0.1:$PORTA/precificacao_anemica
export RICO_DB_USER=postgres
export RICO_DB_URL=jdbc:postgresql://127.0.0.1:$PORTA/precificacao_rica
export DB_CREATE_ENABLED=true
export RICO_DB_CRIAR_SE_AUSENTE=true
export SERVER_ADDRESS=127.0.0.1
export SERVER_PORT=8080
export RICO_SERVER_PORT=8081
EOF
fi
source "$LOCAL/ambiente.env"
export PGPASSWORD="$DB_PASSWORD"
export PGHOST=127.0.0.1 PGPORT="$PORTA" PGUSER="$DB_USER"

if [[ "$ACAO" == parar-banco ]]; then
  if "$PG_BIN/pg_ctl" -D "$DADOS" status >/dev/null 2>&1; then
    "$PG_BIN/pg_ctl" -D "$DADOS" -m fast -w stop
  else
    echo "Banco local já está parado."
  fi
  exit 0
fi

if [[ ! -f "$DADOS/PG_VERSION" ]]; then
  printf '%s\n' "$DB_PASSWORD" > "$LOCAL/senha-inicial"
  "$PG_BIN/initdb" -D "$DADOS" -U "$DB_USER" --encoding=UTF8 --locale=C \
    --locale-provider=icu --icu-locale=pt-BR \
    --auth-local=scram-sha-256 --auth-host=scram-sha-256 --pwfile="$LOCAL/senha-inicial"
  : > "$LOCAL/senha-inicial"
fi
BANCO_INICIADO=false
if ! "$PG_BIN/pg_ctl" -D "$DADOS" status >/dev/null 2>&1; then
  "$PG_BIN/pg_ctl" -D "$DADOS" -l "$LOCAL/postgres.log" \
    -o "-h 127.0.0.1 -p $PORTA -k '$LOCAL/socket'" -w start
  BANCO_INICIADO=true
fi
"$PG_BIN/psql" -X -d postgres -v ON_ERROR_STOP=1 -c 'SELECT 1' >/dev/null

if [[ "$ACAO" == testar ]]; then
  finalizar_testes() {
    if [[ "$BANCO_INICIADO" == true ]]; then
      "$PG_BIN/pg_ctl" -D "$DADOS" -m fast -w stop
    fi
  }
  trap finalizar_testes EXIT
  for banco in precificacao_anemica_teste precificacao_rica_teste; do
    if [[ "$("$PG_BIN/psql" -X -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname = '$banco'")" != 1 ]]; then
      "$PG_BIN/createdb" "$banco"
    fi
  done
  export BANCO_TESTE_URL="jdbc:postgresql://127.0.0.1:$PORTA/precificacao_anemica_teste"
  export BANCO_CRIACAO_TESTE_URL="jdbc:postgresql://127.0.0.1:$PORTA/postgres"
  export BANCO_TESTE_USUARIO="$DB_USER" BANCO_TESTE_SENHA="$DB_PASSWORD"
  export RICO_BANCO_TESTE_URL="jdbc:postgresql://127.0.0.1:$PORTA/precificacao_rica_teste"
  export RICO_BANCO_CRIACAO_TESTE_URL="$BANCO_CRIACAO_TESTE_URL"
  export RICO_BANCO_TESTE_USUARIO="$DB_USER" RICO_BANCO_TESTE_SENHA="$DB_PASSWORD"
  mvn -s "$RAIZ/configuracao-maven.xml" -B -ntp -fae -Pintegracao test
  exit 0
fi

for arquivo in precificacao-anemica/.env.anemico .env.rico; do
  if [[ ! -e "$arquivo" ]]; then
    sed 's/^export //' "$LOCAL/ambiente.env" > "$arquivo"
  fi
done
echo "PostgreSQL local disponível em 127.0.0.1:$PORTA; dados em .local/$PERFIL/postgres."
if [[ "$ACAO" != iniciar ]]; then exit 0; fi

mvn -s "$RAIZ/configuracao-maven.xml" -B -ntp package
PIDS=()
finalizar_apis() {
  trap - EXIT INT TERM
  for pid in "${PIDS[@]}"; do kill "$pid" 2>/dev/null || true; done
  for pid in "${PIDS[@]}"; do wait "$pid" 2>/dev/null || true; done
}
trap finalizar_apis EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
java -jar precificacao-anemica/target/precificacao-anemica-1.0.0-SNAPSHOT.jar > "$LOCAL/anemica.log" 2>&1 &
PIDS+=("$!")
java -jar precificacao-rica/target/precificacao-rica-1.0.0-SNAPSHOT.jar > "$LOCAL/rica.log" 2>&1 &
PIDS+=("$!")
for tentativa in {1..60}; do
  for pid in "${PIDS[@]}"; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "Uma API encerrou. Consulte .local/desenvolvimento/{anemica,rica}.log." >&2
      exit 1
    fi
  done
  if curl --silent --fail --max-time 2 "http://127.0.0.1:$SERVER_PORT/api/produtos" >/dev/null \
    && curl --silent --fail --max-time 2 "http://127.0.0.1:$RICO_SERVER_PORT/api/produtos" >/dev/null; then
    echo "APIs prontas: http://localhost:$SERVER_PORT/api/produtos e http://localhost:$RICO_SERVER_PORT/api/produtos"
    echo "Ctrl+C encerra as APIs. Para parar o banco: ./scripts/dev.sh parar-banco"
    while kill -0 "${PIDS[0]}" 2>/dev/null && kill -0 "${PIDS[1]}" 2>/dev/null; do sleep 2; done
    echo "Uma API encerrou inesperadamente; consulte os logs." >&2
    exit 1
  fi
  sleep 1
done
echo "As APIs não ficaram prontas em 60 tentativas. Consulte os logs em $LOCAL." >&2
exit 1
