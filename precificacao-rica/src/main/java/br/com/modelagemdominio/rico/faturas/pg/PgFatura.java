package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.Fatura;
import br.com.modelagemdominio.rico.faturas.ItemFatura;
import br.com.modelagemdominio.rico.faturas.ValoresFatura;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;

/** Referência à fatura no PostgreSQL; a construção não executa consultas. */
public final class PgFatura implements Fatura {

  private final DataSource banco;
  private final long codigo;

  /**
   * @param banco fonte de conexões
   * @param codigo identificação da fatura
   */
  public PgFatura(DataSource banco, long codigo) {
    this.banco = banco;
    this.codigo = codigo;
  }

  /**
   * @return fatura com cabeçalho, itens e preços da mesma consulta
   * @throws FalhaFatura quando o código for inválido, ausente ou a leitura falhar
   */
  public Fatura materializada() throws FalhaFatura {
    if (this.codigo <= 0) {
      throw new FalhaFatura(FalhaFatura.Motivo.CODIGO_INVALIDO);
    }
    try (var conexao = this.banco.getConnection()) {
      return new ConsultaSqlDeFaturas(this.banco).fatura(conexao, this.codigo);
    } catch (SQLException | IllegalArgumentException falha) {
      throw PgFaturas.falhaLeitura(falha);
    }
  }

  @Override
  public long codigo() {
    return this.codigo;
  }

  @Override
  public LocalDateTime dataCriacao() throws FalhaFatura {
    return this.materializada().dataCriacao();
  }

  @Override
  public List<ItemFatura> itens() throws FalhaFatura {
    return this.materializada().itens();
  }

  @Override
  public Desconto desconto() throws FalhaFatura {
    return this.materializada().desconto();
  }

  @Override
  public VinculosFatura vinculos() throws FalhaFatura {
    return this.materializada().vinculos();
  }

  @Override
  public ValoresFatura valores() throws FalhaFatura {
    return this.materializada().valores();
  }
}
