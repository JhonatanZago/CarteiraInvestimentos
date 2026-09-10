# Sales

## Requirements

- A simulação não persiste.
- A confirmação valida carteira, posição, quantidade, preço, taxas, data e moeda.
- Venda parcial preserva o preço médio e reduz quantidade.
- Venda total remove a projeção aberta, preservando a entidade `Venda`.
- Valores usam a moeda nativa do ativo.
