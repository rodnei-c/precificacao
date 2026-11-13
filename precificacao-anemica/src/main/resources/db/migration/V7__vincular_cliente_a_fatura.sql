ALTER TABLE fatura ADD COLUMN cliente_codigo BIGINT REFERENCES cliente (codigo);

CREATE INDEX ix_fatura_cliente ON fatura (cliente_codigo);
