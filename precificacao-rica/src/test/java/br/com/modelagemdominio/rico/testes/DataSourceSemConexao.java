package br.com.modelagemdominio.rico.testes;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class DataSourceSemConexao implements DataSource {

    @Override
    public Connection getConnection() {
        throw new AssertionError("O teste tentou abrir uma conexão com o banco.");
    }

    @Override
    public Connection getConnection(String usuario, String senha) {
        return this.getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter escritor) throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getLoginTimeout() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int segundos) throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> tipo) {
        return tipo.isInstance(this);
    }

    @Override
    public <T> T unwrap(Class<T> tipo) throws SQLFeatureNotSupportedException {
        if (this.isWrapperFor(tipo)) {
            return tipo.cast(this);
        }
        throw new SQLFeatureNotSupportedException();
    }
}
