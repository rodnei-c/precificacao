ALTER TABLE public.produto
    ADD CONSTRAINT ck_produto_precificacao_sem_nan CHECK (
        custo <> 'NaN'::numeric
        AND preco_venda <> 'NaN'::numeric
        AND percentual <> 'NaN'::numeric
    );
