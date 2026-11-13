ALTER TABLE fatura
  ADD COLUMN forma_pagamento VARCHAR(20),
  ADD CONSTRAINT ck_fatura_forma_pagamento CHECK (
    forma_pagamento IN ('PIX', 'DINHEIRO', 'CARTAO_DEBITO', 'CARTAO_CREDITO', 'BOLETO')
  );
