'use strict';

// Executa somente as APIs pm usadas pela coleção do projeto, com HTTP real e sem dependências npm.
const fs = require('fs');
const path = require('path');
const http = require('http');
const vm = require('vm');
const crypto = require('crypto');
const assert = require('assert');
const URL = require('url').URL;

const configuracao = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const destino = path.resolve(process.argv[3] || 'target/validacao-etapa21/equivalencia.json');
const arquivoColecao = path.resolve(__dirname, '../documentacao/postman/precificacao-rica.postman_collection.json');
const colecao = JSON.parse(fs.readFileSync(arquivoColecao, 'utf8'));
const relatorio = {inicio: new Date().toISOString(), executor: 'Node.js: HTTP e subconjunto pm usado na coleção', colecao: arquivoColecao, execucoes: [], comparacoes: [], diferencasErro: []};

function numeroExato(valor) {
  const partes = /^(-?)(\d+)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/.exec(valor);
  assert(partes, 'Número JSON inválido: ' + valor);
  let digitos = (partes[2] + (partes[3] || '')).replace(/^0+/, '');
  let expoente = Number(partes[4] || 0) - (partes[3] || '').length;
  if (!digitos) { return '0'; }
  while (digitos.endsWith('0')) { digitos = digitos.slice(0, -1); expoente++; }
  return partes[1] + digitos + 'e' + expoente;
}

function jsonExato(texto) {
  return JSON.parse(texto.replace(/"(?:\\.|[^"\\])*"|(-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?)/g,
    (token, numero) => numero ? JSON.stringify({_numero: numeroExato(numero)}) : token));
}

function valorComum(valor) {
  return valor === undefined ? undefined : JSON.parse(JSON.stringify(valor));
}

function expectativa(valor, negar) {
  function conferir(verdade, mensagem) { assert(negar ? !verdade : verdade, mensagem); }
  return {
    get to() { return this; }, get have() { return this; },
    get not() { return expectativa(valor, !negar); },
    get exist() { conferir(valor !== null && valor !== undefined, 'Valor ausente'); return this; },
    eql(esperado) {
      let iguais = true;
      try { assert.deepStrictEqual(valorComum(valor), valorComum(esperado)); } catch (_) { iguais = false; }
      conferir(iguais, 'Esperado ' + JSON.stringify(esperado) + ', recebido ' + JSON.stringify(valor));
      return this;
    },
    lengthOf(tamanho) { conferir(valor && valor.length === tamanho, 'Tamanho esperado: ' + tamanho); return this; },
    property(nome) { conferir(valor != null && Object.prototype.hasOwnProperty.call(valor, nome), 'Propriedade: ' + nome); return this; }
  };
}

function requisicao(url, metodo, cabecalhos, corpo) {
  return new Promise((resolve, reject) => {
    const chamada = http.request(url, {method: metodo, headers: cabecalhos}, resposta => {
      const partes = [];
      resposta.on('data', parte => partes.push(parte));
      resposta.on('error', reject);
      resposta.on('end', () => resolve({codigo: resposta.statusCode, cabecalhos: resposta.headers, texto: Buffer.concat(partes).toString('utf8')}));
    });
    chamada.setTimeout(15000, () => chamada.destroy(new Error('Tempo excedido: ' + metodo + ' ' + url)));
    chamada.on('error', reject);
    if (corpo) { chamada.write(corpo); }
    chamada.end();
  });
}

function itens(itensColecao, herdados) {
  const resultado = [];
  itensColecao.forEach(item => {
    const eventos = herdados.concat(item.event || []);
    if (item.item) { resultado.push.apply(resultado, itens(item.item, eventos)); }
    else { resultado.push({item, eventos}); }
  });
  return resultado;
}

function ambiente(nome, endereco) {
  const base = new URL(endereco);
  assert(base.protocol === 'http:' && ['localhost', '127.0.0.1'].includes(base.hostname), 'Use uma API local de teste.');
  return {nome, base: base.origin, aliases: {cidades: {}, categorias: {}, clientes: {}, produtos: {}, faturas: {}}};
}

function registrarIdentidade(ambienteAtual, recurso, corpo, rodada, indice) {
  if (ambienteAtual.aliases[recurso] && corpo.codigo != null) {
    ambienteAtual.aliases[recurso][numeroExato(String(corpo.codigo))] = rodada + ':' + recurso + ':' + indice;
  }
}

function normalizar(valor, recurso, ambienteAtual, indice) {
  if (Array.isArray(valor)) { return valor.map((item, posicao) => normalizar(item, recurso, ambienteAtual, posicao)); }
  if (valor === null || typeof valor !== 'object' || valor._numero !== undefined) { return valor; }
  const referencia = {codigoProduto: 'produtos', codigoCategoria: 'categorias', codigoCliente: 'clientes', codigoCidade: 'cidades'};
  const resultado = {};
  Object.keys(valor).sort().forEach(chave => {
    const atual = valor[chave];
    if (chave === 'dataCriacao' || chave === 'dataAlteracao') {
      assert(typeof atual === 'string' && /^\d{4}-\d\d-\d\dT\d\d:\d\d/.test(atual), 'Data gerada não está em ISO.');
      resultado[chave] = 'data-gerada';
    } else if ((chave === 'codigo' || referencia[chave]) && atual && atual._numero !== undefined) {
      const tipo = referencia[chave] || recurso;
      if (tipo === 'historico') { resultado[chave] = 'historico:' + indice; }
      else {
        const alias = ambienteAtual.aliases[tipo] && ambienteAtual.aliases[tipo][atual._numero];
        assert(alias, 'Identidade sem origem na execução: ' + tipo + '/' + atual._numero);
        resultado[chave] = alias;
      }
    } else { resultado[chave] = normalizar(atual, recurso, ambienteAtual, indice); }
  });
  return resultado;
}

function problemaComparavel(problema, ambienteAtual) {
  const partes = problema.instance.split('/');
  const aliases = ambienteAtual.aliases[partes[2]];
  if (aliases && /^\d+$/.test(partes[3] || '')) {
    partes[3] = aliases[numeroExato(partes[3])] || partes[3];
  }
  const resultado = {};
  Object.keys(problema).sort().forEach(chave => {
    resultado[chave] = chave === 'instance' ? partes.join('/') : problema[chave];
  });
  return resultado;
}

async function executar(ambienteAtual, rodada, identificador) {
  const variaveis = new Map(colecao.variable.map(variavel => [variavel.key, variavel.value]));
  variaveis.set('baseUrl', ambienteAtual.base);
  let aleatorio = crypto.createHash('sha256').update(identificador).digest().readUInt32LE(0);
  const matematica = Object.create(Math);
  matematica.random = () => { aleatorio = (Math.imul(aleatorio, 1664525) + 1013904223) >>> 0; return aleatorio / 4294967296; };
  function substituir(texto) {
    return String(texto).replace(/\{\{([^}]+)\}\}/g, (_, chave) => {
      if (chave === '$guid') { return identificador; }
      assert(variaveis.has(chave), 'Variável não definida: ' + chave);
      return String(variaveis.get(chave));
    });
  }
  const execucao = {modelo: ambienteAtual.nome, rodada, identificador, requisicoes: [], assercoes: 0};
  relatorio.execucoes.push(execucao);
  const fluxos = itens(colecao.item, colecao.event || []);
  for (let indice = 0; indice < fluxos.length; indice++) {
    const fluxo = fluxos[indice];
    const verificacoes = [];
    const pm = {
      collectionVariables: {get: chave => variaveis.get(chave), set: (chave, valor) => variaveis.set(chave, valor)},
      variables: {replaceIn: substituir},
      expect: valor => expectativa(valor, false),
      test: (nome, teste) => {
        try { teste(); verificacoes.push({nome, passou: true}); }
        catch (falha) { verificacoes.push({nome, passou: false, detalhe: falha.message}); }
      }
    };
    const contexto = vm.createContext({pm, Math: matematica});
    function scripts(tipo) {
      fluxo.eventos.filter(evento => evento.listen === tipo).forEach(evento => {
        vm.runInContext(evento.script.exec.join('\n'), contexto, {timeout: 3000, filename: fluxo.item.name});
      });
    }
    scripts('prerequest');
    const pedido = fluxo.item.request;
    const url = new URL(substituir(typeof pedido.url === 'string' ? pedido.url : pedido.url.raw));
    assert(url.origin === ambienteAtual.base, 'A coleção deve permanecer na API local configurada.');
    const cabecalhos = {};
    (pedido.header || []).filter(header => !header.disabled).forEach(header => { cabecalhos[header.key] = substituir(header.value); });
    const corpo = pedido.body && pedido.body.raw ? substituir(pedido.body.raw) : '';
    if (corpo) { cabecalhos['Content-Length'] = Buffer.byteLength(corpo, 'utf8'); }
    const resposta = await requisicao(url, pedido.method, cabecalhos, corpo);
    const dados = JSON.parse(resposta.texto);
    pm.response = {code: resposta.codigo, json: () => dados,
      to: {have: {status: esperado => assert.strictEqual(resposta.codigo, esperado, resposta.texto)}}};
    scripts('test');
    execucao.assercoes += verificacoes.length;
    const recurso = url.pathname.includes('/historico-precificacao') ? 'historico' : url.pathname.split('/')[2];
    if (pedido.method === 'POST' && resposta.codigo === 201) { registrarIdentidade(ambienteAtual, recurso, dados, rodada, indice); }
    const registro = {nome: fluxo.item.name, metodo: pedido.method, rota: url.pathname, codigo: resposta.codigo, verificacoes};
    if (resposta.codigo >= 400) {
      ['title', 'detail', 'instance'].forEach(campo => assert(typeof dados[campo] === 'string' && dados[campo].length, 'Problema sem ' + campo));
      assert.strictEqual(dados.status, resposta.codigo);
      assert(String(resposta.cabecalhos['content-type']).includes('application/problem+json'));
      registro.normalizado = {status: resposta.codigo};
      registro.problema = dados;
    } else {
      registro.normalizado = normalizar(jsonExato(resposta.texto), recurso, ambienteAtual, 0);
    }
    execucao.requisicoes.push(registro);
    const falhas = verificacoes.filter(verificacao => !verificacao.passou);
    assert(!falhas.length, ambienteAtual.nome + ': ' + fluxo.item.name + ': ' + JSON.stringify(falhas));
  }
  execucao.variaveis = Object.fromEntries(variaveis);
  process.stdout.write(JSON.stringify({modelo: ambienteAtual.nome, rodada, requisicoes: execucao.requisicoes.length, assercoes: execucao.assercoes}) + '\n');
  return execucao;
}

async function principal() {
  const anemico = ambiente('anemico', configuracao.anemico);
  const rico = ambiente('rico', configuracao.rico);
  assert(anemico.base !== rico.base, 'Use aplicações separadas.');
  const rodadas = configuracao.rodadas || 2;
  assert(Number.isInteger(rodadas) && rodadas >= 2 && rodadas <= 10);
  for (let rodada = 1; rodada <= rodadas; rodada++) {
    const identificador = crypto.randomBytes(16).toString('hex');
    const primeiro = await executar(anemico, rodada, identificador);
    const segundo = await executar(rico, rodada, identificador);
    assert.strictEqual(primeiro.requisicoes.length, segundo.requisicoes.length);
    primeiro.requisicoes.forEach((resposta, indice) => {
      const outra = segundo.requisicoes[indice];
      assert.strictEqual(resposta.codigo, outra.codigo, resposta.nome);
      assert.deepStrictEqual(resposta.normalizado, outra.normalizado, resposta.nome);
      if (resposta.problema) {
        try {
          assert.deepStrictEqual(problemaComparavel(resposta.problema, anemico), problemaComparavel(outra.problema, rico));
        } catch (_) {
          relatorio.diferencasErro.push({rodada, nome: resposta.nome, anemico: resposta.problema, rico: outra.problema});
        }
      }
      relatorio.comparacoes.push({rodada, nome: resposta.nome, codigo: resposta.codigo, passou: true});
    });
  }
  relatorio.sucesso = true;
}

principal().catch(falha => {
  relatorio.sucesso = false;
  relatorio.falha = {mensagem: falha.message, stack: falha.stack};
  process.exitCode = 1;
}).then(() => {
  relatorio.fim = new Date().toISOString();
  fs.mkdirSync(path.dirname(destino), {recursive: true});
  fs.writeFileSync(destino, JSON.stringify(relatorio, null, 2) + '\n');
  process.stdout.write(JSON.stringify({sucesso: relatorio.sucesso, comparacoes: relatorio.comparacoes.length, relatorio: destino, falha: relatorio.falha && relatorio.falha.mensagem}) + '\n');
});
