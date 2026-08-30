# Especificação — Frontend da Carteira de Investimentos

## 1. Objetivo

Refazer completamente o frontend Angular do sistema de gestão de ações para que tenha aparência, organização e interatividade semelhantes às plataformas modernas de acompanhamento de investimentos, utilizando como referências funcionais:

- [Status Invest — Carteira de Investimentos](https://statusinvest.com.br/produtos/carteira-de-investimentos)
- [Investidor10](https://investidor10.com.br)

As referências devem orientar a qualidade visual, a organização das informações e a experiência de uso. Não devem ser copiados código-fonte, identidade visual, logotipos ou conteúdo proprietário.

O frontend deve continuar respeitando o objetivo acadêmico original: gerenciar corretoras e ações brasileiras e americanas, consumindo uma API REST desenvolvida em Java 21 com Spring Boot.

---

## 2. Problema identificado

O frontend anterior apresentava características de um CRUD administrativo simples. Embora permitisse trabalhar com registros, ele não transmitia a aparência de uma plataforma de investimentos.

Os principais problemas identificados foram:

- ausência de um dashboard financeiro;
- pouco destaque para patrimônio, lucro e rentabilidade;
- inexistência de gráficos de evolução e composição;
- tabela de ativos com poucas informações financeiras;
- falta de indicadores do mercado;
- ausência de uma identidade visual relacionada ao setor financeiro;
- pouca interatividade nas operações;
- falta de estados visuais de carregamento, erro e ausência de dados;
- integração excessivamente ligada ao formato das APIs externas;
- possibilidade de expor tokens das APIs no navegador;
- pouca semelhança funcional com Status Invest e Investidor10.

---

## 3. Tecnologias do frontend

- Angular 20 ou versão compatível com o projeto existente;
- TypeScript com modo estrito;
- componentes standalone;
- SCSS;
- Angular HttpClient;
- Reactive Forms ou Forms, conforme o formulário;
- RxJS;
- layout responsivo;
- interceptador HTTP para padronização dos erros;
- variáveis de ambiente para o endereço do backend.

O frontend deve funcionar em:

```text
http://localhost:4200
```

O backend será acessado inicialmente em:

```text
http://localhost:8080/api
```

---

## 4. Arquitetura de integração

O Angular não deve acessar diretamente serviços externos que utilizem tokens ou chaves privadas.

O fluxo correto será:

```text
Angular
   ↓
API REST Spring Boot
   ├── Adapter brapi
   ├── Adapter Alpha Vantage ou Twelve Data
   ├── Adapter BrasilAPI
   ├── Adapter ViaCEP
   └── Adapter de validação da CVM
```

Responsabilidades:

### Angular

- apresentar dados;
- enviar comandos para o backend;
- validar campos básicos dos formulários;
- tratar carregamento, sucesso e erro;
- atualizar a interface após operações;
- nunca armazenar chaves privadas.

### Spring Boot

- aplicar regras de negócio;
- validar ticker, CNPJ e CEP;
- selecionar o provedor correto;
- proteger tokens;
- normalizar as respostas externas;
- persistir as informações;
- tratar limites e indisponibilidade dos provedores;
- devolver DTOs estáveis ao Angular.

---

## 5. Revisão das APIs externas

### 5.1. brapi

Documentação: [brapi.dev/docs](https://brapi.dev/docs)

A brapi deve ser a principal fonte para ativos listados na B3, incluindo ações, ETFs, BDRs, FIIs e índices suportados.

Para novas integrações, devem ser priorizados os endpoints da versão 2:

| Necessidade | Endpoint recomendado |
|---|---|
| Cotação e variação atual | `/api/v2/stocks/quote` |
| Histórico de preços | `/api/v2/stocks/historical` |
| Perfil da empresa | `/api/v2/stocks/profile` |
| Indicadores fundamentalistas | `/api/v2/stocks/statistics` |
| Dividendos e JCP | `/api/v2/stocks/dividends` |
| Dados financeiros | `/api/v2/stocks/financial-data` |
| Busca e validação de ticker | `/api/v2/tickers` |

Exemplo de consulta:

```http
GET https://brapi.dev/api/v2/stocks/quote?symbols=PETR4
Authorization: Bearer TOKEN
```

Cuidados:

- o token deve ficar no backend;
- não considerar um ticker válido apenas porque passou em uma expressão regular;
- confirmar a existência do ticker na API;
- tratar HTTP `401`, `403`, `404`, `429` e `500`;
- salvar separadamente o horário da cotação e o horário da consulta;
- comparar o ticker solicitado com o ticker resolvido pela API;
- aplicar cache para evitar consumo desnecessário do limite.

### 5.2. Alpha Vantage

Documentação: [Alpha Vantage](https://www.alphavantage.co/documentation/)

Pode ser utilizada para ações americanas e séries históricas internacionais.

Principais recursos:

- `GLOBAL_QUOTE` para cotação de um ticker;
- `TIME_SERIES_DAILY` para histórico diário;
- busca de símbolos;
- informações cadastrais e financeiras, dependendo do plano.

Limitações:

- exige chave de API;
- possui limites por minuto e por dia conforme o plano;
- alguns recursos em tempo real são pagos;
- a resposta precisa ser convertida para DTO próprio.

### 5.3. Twelve Data

Documentação: [Twelve Data](https://twelvedata.com/docs)

Também pode fornecer cotações e históricos de ativos internacionais.

Recomendação:

- escolher Alpha Vantage ou Twelve Data como provedor principal para o mercado americano;
- não consultar as duas APIs em todas as requisições;
- usar a segunda como fallback apenas quando permitido pelos limites e termos do serviço;
- manter a escolha encapsulada em adapters do backend.

### 5.4. BrasilAPI

Documentação: [BrasilAPI](https://brasilapi.com.br/docs)

Deve ser utilizada para consultar dados cadastrais de uma empresa pelo CNPJ.

A existência do CNPJ na BrasilAPI não comprova que a instituição esteja autorizada a operar no mercado financeiro.

O backend deve:

- remover a máscara antes da consulta;
- validar os 14 dígitos;
- consultar os dados cadastrais;
- analisar a situação cadastral;
- armazenar os dados normalizados;
- realizar uma segunda validação em fonte oficial da CVM.

### 5.5. ViaCEP

Documentação: [ViaCEP](https://viacep.com.br/)

Formato:

```http
GET https://viacep.com.br/ws/01001000/json/
```

Regras importantes:

- o CEP deve possuir exatamente oito dígitos;
- CEP com formato inválido pode gerar HTTP `400`;
- CEP inexistente pode retornar HTTP `200` com `erro: true`;
- a resposta deve ser validada antes do salvamento.

### 5.6. Validação na CVM

A validação como participante do mercado financeiro precisa utilizar uma fonte oficial ou uma base pública equivalente da CVM.

Não se deve concluir que uma empresa é uma corretora autorizada apenas porque:

- possui CNPJ válido;
- está com situação cadastral ativa;
- possui determinado CNAE;
- contém palavras como “investimentos” ou “corretora” em sua razão social.

O sistema deve armazenar o resultado da validação, a fonte utilizada e a data da consulta.

---

## 6. Identidade visual

O frontend deve adotar uma identidade visual financeira moderna:

- fundo principal cinza muito claro;
- menu lateral verde-escuro;
- verde como cor principal de ação e valorização;
- vermelho apenas para prejuízo ou desvalorização;
- cards brancos com bordas discretas;
- espaçamento consistente;
- tipografia limpa;
- ícones simples;
- sombras leves;
- cantos arredondados moderados;
- valores financeiros com maior destaque que os textos auxiliares.

Paleta sugerida:

```scss
$primary: #0d9f63;
$primary-dark: #10271e;
$positive: #16a66a;
$negative: #d55757;
$background: #f4f6f5;
$surface: #ffffff;
$text: #17221e;
$muted: #6c7873;
$border: #e4e9e7;
```

---

## 7. Estrutura principal da aplicação

### 7.1. Menu lateral

Itens sugeridos:

- Visão geral;
- Minha carteira;
- Movimentações;
- Proventos;
- Buscar ativos;
- Comparador;
- Corretoras;
- Configurações.

O menu deve:

- destacar a página ativa;
- recolher ou transformar-se em menu móvel em telas pequenas;
- apresentar nome e perfil do usuário;
- manter navegação simples.

### 7.2. Cabeçalho

Deve conter:

- situação do mercado;
- horário da última atualização;
- pesquisa global;
- notificações;
- botão “Adicionar ativo”.

### 7.3. Área de boas-vindas

Exemplo:

```text
Olá, Jhonatan 👋
Acompanhe o desempenho dos seus investimentos.
```

Também deve apresentar um seletor de carteira para permitir evolução futura com múltiplas carteiras.

---

## 8. Dashboard

### 8.1. Indicadores de mercado

Exibir uma faixa com indicadores, como:

- Ibovespa;
- CDI;
- dólar comercial;
- IPCA;
- Selic, caso esteja disponível.

Cada indicador deve apresentar:

- código;
- descrição;
- valor atual;
- variação;
- cor coerente com a direção da variação;
- data ou horário de referência.

### 8.2. Cards de resumo

Exibir pelo menos:

1. Patrimônio atual;
2. Total investido;
3. Lucro ou prejuízo acumulado;
4. Rentabilidade percentual;
5. Proventos recebidos nos últimos 12 meses;
6. Próximos proventos, quando houver dados.

Fórmulas básicas:

```text
posição do ativo = quantidade × cotação atual
capital investido = quantidade × preço médio
resultado = posição atual − capital investido
rentabilidade (%) = resultado ÷ capital investido × 100
```

Para ativos em moeda estrangeira, a conversão deve utilizar uma cotação cambial informada pelo backend, com horário de referência.

### 8.3. Evolução patrimonial

Adicionar gráfico com:

- evolução do patrimônio;
- evolução do capital investido;
- filtros de período: 1 mês, 6 meses, 12 meses, 2 anos e tudo;
- possibilidade futura de comparação com IBOV e CDI;
- tooltip com data e valor;
- estado vazio quando não existir histórico.

### 8.4. Composição da carteira

Adicionar gráfico de rosca ou barras para distribuir o patrimônio por:

- ações brasileiras;
- fundos imobiliários;
- ETFs;
- BDRs;
- stocks;
- outras classes futuras.

O gráfico deve apresentar legenda, percentual e valor financeiro.

---

## 9. Tabela de ativos

A tabela deve conter:

| Coluna | Descrição |
|---|---|
| Ativo | Ticker, nome e classe |
| Mercado | Brasil ou Estados Unidos |
| Quantidade | Quantidade em carteira |
| Preço médio | Preço médio de aquisição |
| Cotação | Último preço disponível |
| Variação diária | Alteração percentual do dia |
| Posição | Quantidade multiplicada pela cotação |
| Resultado | Lucro ou prejuízo financeiro |
| Rentabilidade | Resultado percentual |
| Atualização | Horário da última cotação |
| Ações | Atualizar, visualizar, editar ou excluir |

Recursos necessários:

- pesquisa por ticker ou nome;
- filtro por mercado;
- filtro por classe;
- filtro por resultado positivo ou negativo;
- ordenação por patrimônio, rentabilidade e ticker;
- paginação quando necessário;
- destaque verde para valorização;
- destaque vermelho para desvalorização;
- confirmação antes de excluir;
- atualização visual sem recarregar a página inteira.

---

## 10. Cadastro de ativo

O formulário deve solicitar inicialmente:

- ticker;
- mercado: Brasil ou Estados Unidos;
- quantidade, quando a aplicação controlar posições;
- preço de aquisição ou preço médio;
- corretora associada, quando aplicável;
- data da operação.

Fluxo:

1. O usuário informa o ticker e o mercado.
2. O Angular faz validações básicas.
3. O Spring Boot consulta o provedor adequado.
4. O backend confirma a existência do ticker.
5. O backend impede duplicidade conforme a regra definida.
6. O backend normaliza nome, moeda, cotação e horário.
7. O ativo é salvo.
8. O Angular atualiza a tabela e os indicadores.

Estados da interface:

- campo inválido;
- ticker não encontrado;
- ticker duplicado;
- limite do provedor atingido;
- provedor temporariamente indisponível;
- cadastro concluído.

---

## 11. Cadastro de corretora

O formulário deve conter:

- CNPJ;
- CEP;
- campos complementares de endereço permitidos;
- botão para consultar e validar;
- exibição dos dados retornados;
- indicador de validação na CVM.

Fluxo:

1. Validar formato do CNPJ.
2. Consultar os dados empresariais.
3. Validar a situação cadastral.
4. Consultar a fonte de participantes do mercado.
5. Consultar o CEP.
6. Exibir os dados encontrados.
7. Solicitar confirmação do usuário.
8. Salvar a corretora.

O usuário não deve preencher manualmente razão social e demais dados principais antes da tentativa de consulta externa.

---

## 12. Contrato esperado do backend

Os caminhos podem ser adaptados ao projeto existente, mas o frontend deve concentrar essa adaptação nos services.

| Método | Endpoint | Finalidade |
|---|---|---|
| `GET` | `/api/acoes` | Listar ativos e posições |
| `GET` | `/api/acoes/{id}` | Consultar um ativo |
| `GET` | `/api/acoes/ticker/{ticker}` | Buscar pelo ticker |
| `POST` | `/api/acoes` | Cadastrar ativo |
| `PATCH` | `/api/acoes/{id}/cotacao` | Atualizar cotação |
| `PUT` | `/api/acoes/{id}` | Atualizar dados permitidos |
| `DELETE` | `/api/acoes/{id}` | Excluir ativo |
| `GET` | `/api/corretoras` | Listar corretoras |
| `GET` | `/api/corretoras/{id}` | Consultar corretora |
| `GET` | `/api/corretoras/cnpj/{cnpj}` | Buscar por CNPJ |
| `POST` | `/api/corretoras` | Validar e cadastrar corretora |
| `PUT` | `/api/corretoras/{id}` | Atualizar dados permitidos |
| `DELETE` | `/api/corretoras/{id}` | Excluir corretora |
| `GET` | `/api/dashboard/resumo` | Resumo financeiro |
| `GET` | `/api/dashboard/evolucao` | Histórico patrimonial |
| `GET` | `/api/dashboard/composicao` | Distribuição da carteira |
| `GET` | `/api/mercado/indicadores` | Índices e indicadores |

---

## 13. DTO sugerido para ativo

```json
{
  "id": 1,
  "ticker": "PETR4",
  "name": "Petrobras PN",
  "market": "BR",
  "type": "ACAO",
  "currency": "BRL",
  "quantity": 120,
  "averagePrice": 31.42,
  "currentPrice": 38.50,
  "dailyChangePercent": 0.78,
  "positionValue": 4620.00,
  "resultValue": 849.60,
  "returnPercent": 22.53,
  "logoUrl": null,
  "quoteProvider": "BRAPI",
  "quotedAt": "2026-08-30T16:00:00Z",
  "fetchedAt": "2026-08-30T16:01:02Z"
}
```

O backend deve enviar números como valores numéricos, não como strings já formatadas.

---

## 14. Tratamento padronizado de erros

Preferir respostas no padrão Problem Details:

```json
{
  "type": "https://api.exemplo.com/errors/ticker-not-found",
  "title": "Ativo não encontrado",
  "status": 404,
  "detail": "O ticker ABCD99 não foi encontrado no mercado informado.",
  "instance": "/api/acoes",
  "timestamp": "2026-08-30T16:05:00Z"
}
```

O Angular deve tratar:

- `400`: dados inválidos;
- `404`: registro ou ticker não encontrado;
- `409`: cadastro duplicado;
- `422`: regra de negócio não atendida;
- `429`: limite de requisições;
- `500`: erro interno;
- `502` ou `503`: provedor externo indisponível.

Não mostrar stack trace ao usuário.

---

## 15. Estados visuais obrigatórios

Cada tela deve possuir:

- skeleton ou indicador de carregamento;
- mensagem de lista vazia;
- botão para tentar novamente;
- mensagem clara de erro;
- feedback de operação concluída;
- bloqueio contra duplo envio;
- confirmação de exclusão;
- indicação de dados desatualizados;
- layout funcional em celular, tablet e computador.

---

## 16. Organização sugerida do Angular

```text
src/app/
├── core/
│   ├── interceptors/
│   ├── guards/
│   ├── layout/
│   └── services/
├── shared/
│   ├── components/
│   ├── pipes/
│   ├── directives/
│   └── models/
├── features/
│   ├── dashboard/
│   ├── assets/
│   ├── brokers/
│   ├── transactions/
│   └── dividends/
├── app.component.ts
├── app.config.ts
└── app.routes.ts
```

Cada feature pode conter:

```text
assets/
├── pages/
├── components/
├── models/
├── services/
└── validators/
```

---

## 17. Segurança

- não armazenar tokens de APIs externas no Angular;
- não colocar segredos em `environment.ts`;
- não confiar apenas nas validações do frontend;
- configurar CORS no Spring Boot somente para origens permitidas;
- escapar e validar entradas;
- não expor stack traces;
- aplicar autenticação quando o escopo do projeto permitir;
- não registrar tokens ou respostas sensíveis em logs;
- utilizar HTTPS em produção.

---

## 18. Desempenho e confiabilidade

O backend deve implementar:

- timeout nas integrações;
- retry apenas para erros transitórios;
- circuit breaker;
- cache curto para cotações;
- cache longo para CNPJ e CEP;
- tratamento de `429 Too Many Requests`;
- fallback controlado;
- registro do provedor consultado;
- logs sem dados sensíveis.

O frontend deve:

- evitar chamadas duplicadas;
- utilizar carregamento sob demanda quando fizer sentido;
- manter componentes pequenos;
- utilizar `track` ou `trackBy` nas listas;
- evitar cálculos pesados diretamente no template;
- exibir o horário real de atualização.

---

## 19. Testes obrigatórios

### Frontend

- renderização do dashboard;
- cálculo e formatação dos valores;
- busca e filtro de ativos;
- cadastro com sucesso;
- ticker inválido;
- cadastro duplicado;
- atualização de cotação;
- exclusão com confirmação;
- erro do backend;
- backend indisponível;
- responsividade;
- acessibilidade básica por teclado.

### Integração

- Angular acessando o Spring Boot;
- CORS configurado corretamente;
- DTOs compatíveis;
- data e moeda interpretadas corretamente;
- tratamento do limite das APIs;
- API externa indisponível;
- cotação desatualizada;
- ticker brasileiro e americano.

### Comandos mínimos

```bash
npm install
npm run build
npm test -- --watch=false
```

O projeto não deve ser entregue enquanto o build apresentar erros.

---

## 20. Critérios de aceite

- O frontend não pode parecer apenas um CRUD genérico.
- O dashboard deve apresentar informações financeiras relevantes.
- A interface deve ser visualmente próxima de uma plataforma moderna de investimentos.
- Os valores devem vir do backend ou estar explicitamente identificados como demonstração.
- O sistema deve distinguir valorização e desvalorização.
- A tabela deve mostrar posição, resultado e rentabilidade.
- O usuário deve conseguir buscar e filtrar ativos.
- O cadastro deve validar o ticker no mercado correspondente.
- O frontend deve apresentar feedback para sucesso e erro.
- Tokens externos não podem aparecer no código do Angular.
- O layout deve funcionar em desktop e dispositivos móveis.
- O build de produção deve finalizar sem erros.
- A integração deve respeitar as regras de negócio já definidas para ações e corretoras.

---

## 21. Prompt final para implementação

```text
Revise o frontend Angular existente e refaça sua interface para transformá-lo em
uma plataforma moderna de acompanhamento de carteira de investimentos.

Use Status Invest e Investidor10 apenas como referências funcionais e visuais,
sem copiar código, marca ou identidade proprietária.

Antes de alterar os arquivos:
1. analise toda a estrutura atual do frontend;
2. analise os DTOs e endpoints do backend Spring Boot;
3. preserve as regras de negócio existentes;
4. identifique incompatibilidades entre frontend e backend;
5. revise os services e contratos HTTP.

Implemente:
- menu lateral responsivo;
- cabeçalho com situação do mercado e última atualização;
- dashboard com patrimônio, capital investido, resultado, rentabilidade e
  proventos;
- indicadores de mercado;
- gráfico de evolução patrimonial;
- gráfico de composição da carteira;
- tabela profissional de ativos;
- pesquisa, filtros e ordenação;
- cadastro de ativo em modal ou página dedicada;
- atualização de cotação;
- exclusão com confirmação;
- tela de corretoras com validação visual de CNPJ, CEP e CVM;
- estados de carregamento, vazio, sucesso e erro;
- tratamento centralizado dos erros HTTP;
- responsividade completa.

Utilize Angular, TypeScript estrito e SCSS. Não coloque chaves de brapi,
Alpha Vantage ou Twelve Data no navegador. Todas as APIs externas devem ser
consumidas pelo Spring Boot por meio de adapters.

Para ativos brasileiros, priorize os endpoints v2 da brapi. Para ativos
americanos, use o provedor configurado no backend. Use BrasilAPI para dados
cadastrais do CNPJ, ViaCEP para endereço e uma fonte oficial da CVM para
confirmar autorização da instituição.

Não use números fixos como se fossem dados reais. Caso seja necessário manter
dados demonstrativos, identifique claramente o modo de demonstração e deixe a
integração real preparada.

Ao finalizar:
1. execute npm install;
2. execute o build de produção;
3. execute os testes;
4. corrija todos os erros encontrados;
5. documente os endpoints esperados;
6. informe arquivos modificados e limitações restantes.
```

---

## 22. Resultado esperado

Ao final, o sistema deve apresentar uma experiência coerente com uma carteira de investimentos real, mantendo o escopo acadêmico e as regras originais do projeto. O frontend deve comunicar claramente:

- quanto o usuário investiu;
- quanto possui atualmente;
- quanto ganhou ou perdeu;
- como o patrimônio evoluiu;
- quais ativos formam a carteira;
- qual é a situação de cada investimento;
- quando as informações foram atualizadas;
- quais corretoras foram validadas.

Essa organização melhora a qualidade visual do projeto, facilita a apresentação acadêmica e aproxima a aplicação de um produto financeiro utilizável.
