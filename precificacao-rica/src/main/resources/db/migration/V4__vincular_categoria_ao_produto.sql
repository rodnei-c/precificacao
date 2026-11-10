ALTER TABLE produto ADD COLUMN categoria_codigo BIGINT REFERENCES categoria (codigo);

CREATE INDEX ix_produto_categoria ON produto (categoria_codigo);
