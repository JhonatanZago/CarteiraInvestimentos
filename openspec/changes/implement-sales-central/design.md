# Design

`Venda` é uma entidade imutável ligada a carteira, ação e corretora, com valores monetários calculados em `BigDecimal`. `VendaService` bloqueia a posição com lock pessimista durante confirmação. Simulação usa o mesmo cálculo sem persistir. A rota Angular `/vendas` consome simulação, confirmação e histórico.
