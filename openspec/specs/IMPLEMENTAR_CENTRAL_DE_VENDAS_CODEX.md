# Implementar Central de Vendas — Carteira+

## Material de referência

Use a imagem `TEMPLATE_CENTRAL_DE_VENDAS.png` anexada junto com este arquivo como referência visual obrigatória.

Implemente a **Opção B — Central de vendas**. A aparência deve seguir o template, mas deve reutilizar o tema, as cores, a tipografia, os componentes e a responsividade já existentes no projeto. Não substitua telas funcionais e não desfaça alterações anteriores.

## Instrução principal para o Codex

Analise completamente o backend Spring Boot e o frontend Angular antes de alterar qualquer arquivo. Localize as entidades de carteira, ação, corretora e posição, os serviços financeiros, os endpoints REST, os modelos Angular, as rotas e os componentes em uso.

Implemente uma área completa de venda de ações. Uma venda representa uma operação financeira e **não** deve ser tratada como simples exclusão da posição.

Antes de finalizar:

1. Compile backend e frontend.
2. Execute os testes existentes.
3. Crie testes para todas as regras novas.
4. Corrija erros e regressões encontrados.
5. Não declare a implementação concluída enquanto houver teste falhando por causa do código alterado.

## Interface da Central de vendas

Crie uma rota própria, preferencialmente `/vendas`, e adicione **Vendas** ao menu principal.

A tela deve possuir:

- seletor de carteira;
- seletor contendo apenas posições com quantidade disponível;
- ticker, nome da empresa, logotipo e quantidade disponível;
- preço médio atual da posição;
- cotação atual preenchida automaticamente;
- seleção entre `Usar cotação atual` e `Informar preço de venda`;
- campo editável para preço de venda quando escolhida a segunda opção;
- quantidade a vender;
- data da venda;
- taxas da operação, aceitando zero;
- resumo com valor bruto, custo proporcional, taxas, resultado estimado e rentabilidade;
- informação da quantidade restante após a venda;
- botão `Confirmar venda`;
- estado de carregamento que impeça clique duplo;
- mensagens claras de sucesso e erro.

O cálculo mostrado na interface deve ser atualizado quando o usuário alterar quantidade, preço ou taxas. O backend deverá recalcular e validar tudo; nunca confie apenas nos cálculos do frontend.

## Regras financeiras

Utilize `BigDecimal` no backend. Não use `double` para persistir ou calcular valores monetários.

```text
valorBruto = quantidadeVendida × precoVenda
custoDaPosicao = quantidadeVendida × precoMedio
resultadoRealizado = valorBruto - custoDaPosicao - taxas
rentabilidadePercentual = resultadoRealizado ÷ custoDaPosicao × 100
quantidadeRestante = quantidadeDisponivel - quantidadeVendida
```

- Venda parcial: reduza a quantidade e preserve o preço médio das ações restantes.
- Venda total: encerre a posição aberta, mas preserve todo o histórico da operação.
- Nunca altere o preço médio com base no preço de venda.
- Não permita saldo negativo.
- Faça a confirmação dentro de uma transação.
- Bloqueie a posição durante a confirmação ou use controle de versão para impedir duas vendas simultâneas sobre o mesmo saldo.
- A simulação não pode modificar nem persistir a posição.

## Validações obrigatórias

Rejeite a operação quando:

- a carteira não existir;
- a posição não existir ou pertencer a outra carteira;
- a quantidade for nula, zero ou negativa;
- a quantidade ultrapassar o saldo disponível;
- o preço de venda for nulo, zero ou negativo;
- as taxas forem negativas;
- a data estiver no futuro;
- a data da venda for anterior à primeira compra;
- a moeda informada pelo cliente não corresponder à moeda do ativo.

Retorne mensagens específicas. Exemplo:

```text
Não foi possível realizar a venda. A quantidade informada (25) ultrapassa o saldo disponível de 20 ações de AAPL.
```

## Brasil, Estados Unidos e moedas

A moeda deve vir do cadastro validado da ação, nunca de uma escolha livre feita durante a venda:

| Mercado do ativo | Moeda da operação | Formatação |
|---|---:|---:|
| Brasil/B3 | BRL | R$ 1.234,56 |
| Estados Unidos | USD | US$ 1.234,56 |

- Preço médio, cotação, preço de venda, taxas, valor bruto, custo e resultado devem permanecer na moeda nativa da ação.
- Nunca apresente uma ação americana com símbolo `R$`.
- Nunca apresente uma ação brasileira com símbolo `US$`.
- Não some BRL e USD no mesmo total ou gráfico.
- Quando houver operações nas duas moedas, crie filtros `R$` e `US$` para o gráfico e os indicadores.
- Uma conversão secundária para reais só pode ser exibida quando existir uma cotação USD/BRL válida, com fonte e data. Identifique-a como `Equivalente em R$` e não substitua o valor principal em dólares.

## Persistência e API

Crie uma entidade própria e imutável para a venda, mantendo pelo menos:

- identificador;
- carteira;
- ação;
- corretora;
- quantidade vendida;
- preço médio utilizado como custo;
- preço de venda;
- taxas;
- valor bruto;
- custo proporcional;
- resultado realizado;
- moeda;
- data da venda;
- instante de criação.

Endpoints sugeridos:

```http
POST /api/v1/carteiras/{carteiraId}/vendas/simulacao
POST /api/v1/carteiras/{carteiraId}/vendas
GET  /api/v1/carteiras/{carteiraId}/vendas
```

Exemplo de requisição:

```json
{
  "posicaoId": 12,
  "quantidade": 10,
  "precoVenda": 215.30,
  "taxas": 10.77,
  "dataVenda": "2026-09-10"
}
```

Exemplo de resposta da simulação:

```json
{
  "carteiraId": 1,
  "posicaoId": 12,
  "ticker": "AAPL",
  "quantidade": 10,
  "quantidadeDisponivelAntes": 20,
  "quantidadeRestante": 10,
  "precoMedio": 180.00,
  "precoVenda": 215.30,
  "taxas": 10.77,
  "valorBruto": 2153.00,
  "custoPosicao": 1800.00,
  "resultadoRealizado": 342.23,
  "rentabilidadePercentual": 19.0128,
  "moeda": "USD",
  "dataVenda": "2026-09-10"
}
```

## Gráfico e histórico

Depois da primeira venda, apresente:

- gráfico de resultado realizado por mês;
- verde para lucro;
- vermelho para prejuízo;
- linha de referência em zero;
- filtros separados para BRL e USD;
- tooltip estável com período, moeda e valor;
- cards de lucro realizado, prejuízo realizado e saldo líquido;
- estado vazio explicando que o gráfico aparecerá depois da primeira venda.

A tabela deverá conter:

| Data | Ativo | Quantidade | Preço médio | Preço de venda | Taxas | Resultado |
|---|---|---:|---:|---:|---:|---:|

Utilize barra de rolagem horizontal em telas pequenas, sem cortar valores ou botões.

## Testes obrigatórios

### Backend

- simulação não altera a posição;
- cálculo de lucro;
- cálculo de prejuízo;
- desconto das taxas;
- venda parcial;
- venda total;
- rejeição de quantidade superior ao saldo;
- rejeição de preço inválido;
- rejeição de taxas negativas;
- rejeição de data inválida;
- posição pertencente a outra carteira;
- preservação da moeda BRL;
- preservação da moeda USD;
- duas vendas simultâneas não podem produzir saldo negativo;
- endpoints de simulação, confirmação e histórico.

### Frontend

- preenchimento da cotação ao selecionar a posição;
- alternância entre cotação atual e preço informado;
- atualização da prévia ao editar os campos;
- formatação correta de BRL e USD;
- bloqueio de confirmação para quantidade inválida;
- recarregamento da posição após venda;
- gráfico e tabela usando apenas vendas confirmadas;
- separação dos resultados por moeda;
- estados de carregamento, vazio e erro.

## Critérios de aceite

- O usuário consegue vender parcial ou totalmente uma posição existente.
- A quantidade da carteira é atualizada imediatamente após a confirmação.
- O preço médio restante não é alterado por uma venda parcial.
- O histórico permanece disponível depois de uma venda total.
- Ações americanas aparecem em dólares e brasileiras em reais.
- BRL e USD nunca são somados diretamente.
- Lucros aparecem em verde e prejuízos em vermelho.
- O gráfico não apresenta falhas ao passar o mouse.
- O layout é responsivo e segue a imagem de referência.
- Os testes novos e antigos passam.
- A documentação OpenAPI e a alteração OpenSpec são atualizadas.

## Entrega esperada do Codex

Ao terminar, informe:

1. arquivos criados e modificados;
2. regras implementadas;
3. endpoints disponíveis;
4. testes executados e resultados;
5. comandos para iniciar backend e frontend;
6. limitações reais que permaneceram, sem esconder falhas.

Não use valores falsos em produção, não remova funcionalidades anteriores e não modifique outras telas além do necessário para integrar a Central de vendas.
