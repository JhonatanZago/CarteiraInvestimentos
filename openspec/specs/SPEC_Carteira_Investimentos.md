# SPEC — Carteira de Investimentos

## 1. Visão Geral

Aplicação acadêmica para gerenciamento de corretoras, ações e carteira de investimentos, desenvolvida com:

### Backend
- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Bean Validation
- Lombok
- RestClient ou WebClient
- Springdoc OpenAPI / Swagger
- H2
- PostgreSQL
- JUnit
- Mockito

### Frontend
- Angular
- TypeScript
- HTML
- SCSS
- Angular Router
- Reactive Forms
- HttpClient

### Integrações externas
- BRAPI — ações brasileiras
- Alpha Vantage — ações americanas
- Twelve Data — integração alternativa
- BrasilAPI — consulta de CNPJ
- ViaCEP — consulta e validação de CEP
- CVM ou fonte pública equivalente — validação de instituição financeira

---

# 2. Objetivo

Desenvolver uma aplicação em Spring Boot com frontend Angular para:

- cadastrar corretoras;
- validar CNPJ;
- consultar dados cadastrais externos;
- validar participação da instituição no mercado financeiro;
- validar CEP;
- cadastrar ações brasileiras e americanas;
- consultar cotações externas;
- atualizar cotações;
- manter histórico das cotações consultadas;
- criar carteiras de investimentos;
- associar posições financeiras às carteiras;
- apresentar indicadores em dashboard.

---

# 3. Arquitetura

```text
Angular
   ↓
Resource
   ↓
Service
   ├── Repository → Database
   │
   └── Facade
          ↓
       Adapter
          ↓
       API Externa
```

## 3.1 Responsabilidades

### Resource
Responsável pela camada HTTP.

Pode:
- receber Request DTOs;
- acionar validações;
- chamar Services;
- retornar Response DTOs;
- definir status HTTP.

Não pode:
- acessar Repository diretamente;
- acessar APIs externas;
- aplicar regras de negócio;
- realizar cálculos de domínio.

### Service
Responsável pelas regras de negócio.

Exemplos:
- normalizar dados;
- verificar duplicidade;
- validar regras;
- coordenar persistência;
- atualizar cotação;
- registrar histórico;
- calcular operações do domínio.

### Facade
Responsável por orquestrar integrações externas.

Exemplos:
- escolher Adapter por mercado;
- selecionar integração adequada;
- fornecer uma interface simplificada ao Service.

A Facade não deve substituir o Service.

### Adapter
Responsável pela comunicação direta com APIs externas.

Exemplos:
- montar URL;
- enviar parâmetros;
- autenticar;
- receber JSON;
- converter respostas;
- tratar erros externos;
- devolver DTO interno.

### Repository
Responsável pela persistência.

### Mapper
Responsável exclusivamente pela conversão:

```text
Domain ↔ DTO
```

Não deve:
- chamar Repository;
- chamar Facade;
- chamar Adapter;
- executar regras de negócio.

---

# 4. Estrutura de Pacotes

```text
src/main/java/com/carteirainvestimentos
│
├── CarteiraInvestimentosApplication.java
│
├── config
│   ├── CorsConfig.java
│   ├── OpenApiConfig.java
│   ├── RestClientConfig.java
│   └── properties
│       ├── BrapiProperties.java
│       ├── AlphaVantageProperties.java
│       ├── BrasilApiProperties.java
│       └── ViaCepProperties.java
│
├── domains
│   ├── Acao.java
│   ├── Corretora.java
│   ├── Carteira.java
│   ├── AtivoCarteira.java
│   └── HistoricoCotacao.java
│
├── dtos
│   ├── acao
│   ├── corretora
│   ├── carteira
│   ├── dashboard
│   └── external
│
├── enums
│   ├── Mercado.java
│   ├── Moeda.java
│   ├── TipoAtivo.java
│   ├── PeriodoHistorico.java
│   └── FonteCotacao.java
│
├── repositories
│   ├── AcaoRepository.java
│   ├── CorretoraRepository.java
│   ├── CarteiraRepository.java
│   ├── AtivoCarteiraRepository.java
│   └── HistoricoCotacaoRepository.java
│
├── mappers
│   ├── AcaoMapper.java
│   ├── CorretoraMapper.java
│   ├── CarteiraMapper.java
│   └── AtivoCarteiraMapper.java
│
├── services
│   ├── AcaoService.java
│   ├── CorretoraService.java
│   ├── CarteiraService.java
│   ├── DashboardService.java
│   └── HistoricoCotacaoService.java
│
├── facades
│   ├── CotacaoFacade.java
│   ├── EmpresaFacade.java
│   ├── EnderecoFacade.java
│   └── InstituicaoFinanceiraFacade.java
│
├── adapters
│   ├── cotacao
│   │   ├── CotacaoAdapter.java
│   │   ├── BrapiAdapter.java
│   │   ├── AlphaVantageAdapter.java
│   │   └── TwelveDataAdapter.java
│   │
│   ├── empresa
│   │   ├── EmpresaAdapter.java
│   │   └── BrasilApiAdapter.java
│   │
│   ├── endereco
│   │   ├── EnderecoAdapter.java
│   │   └── ViaCepAdapter.java
│   │
│   └── instituicao
│       ├── InstituicaoFinanceiraAdapter.java
│       └── CvmAdapter.java
│
├── resources
│   ├── AcaoResource.java
│   ├── CorretoraResource.java
│   ├── CarteiraResource.java
│   └── DashboardResource.java
│
└── exceptions
    ├── StandardError.java
    ├── ValidationError.java
    ├── ResourceExceptionHandler.java
    │
    ├── business
    │   ├── BusinessException.java
    │   ├── ObjectNotFoundException.java
    │   ├── DuplicateResourceException.java
    │   ├── InvalidTickerException.java
    │   ├── TickerNotFoundException.java
    │   ├── InvalidCnpjException.java
    │   ├── CnpjNotFoundException.java
    │   ├── InvalidCepException.java
    │   ├── CepNotFoundException.java
    │   └── InstitutionNotAuthorizedException.java
    │
    └── external
        ├── ExternalApiException.java
        ├── ExternalApiUnavailableException.java
        ├── ExternalApiTimeoutException.java
        ├── ExternalApiRateLimitException.java
        └── ExternalApiUnexpectedResponseException.java
```

---

# 5. Domínios

## 5.1 Acao

Representa o ativo financeiro global no sistema.

```text
Acao
--------------------------------
id                  Long
ticker              String
nomeEmpresa         String
mercado             Mercado
moeda               Moeda
cotacaoAtual        BigDecimal
dataHoraCotacao     OffsetDateTime
```

Ação não representa a posição do usuário.

---

## 5.2 Corretora

```text
Corretora
--------------------------------
id                          Long
cnpj                        String
razaoSocial                 String
nomeFantasia                String
email                       String
telefone                    String

cep                         String
logradouro                  String
numero                      String
complemento                 String
bairro                      String
cidade                      String
uf                          String

situacaoCadastral           String
validadaMercadoFinanceiro   boolean
dataValidacaoMercado        OffsetDateTime
fonteValidacaoMercado       String
dataCadastro                OffsetDateTime
```

---

## 5.3 Carteira

```text
Carteira
--------------------------------
id
nome
descricao
dataCriacao
```

---

## 5.4 AtivoCarteira

Representa uma posição patrimonial do usuário.

```text
AtivoCarteira
--------------------------------
id
carteira
acao
corretora
quantidade
precoMedio
dataPrimeiraCompra
```

---

## 5.5 HistoricoCotacao

```text
HistoricoCotacao
--------------------------------
id
acao
valor
dataHoraCotacao
dataHoraRegistro
fonte
```

---

# 6. Enums

## Mercado

```java
public enum Mercado {
    BRASIL,
    EUA
}
```

## Moeda

```java
public enum Moeda {
    BRL,
    USD
}
```

## FonteCotacao

```java
public enum FonteCotacao {
    BRAPI,
    ALPHA_VANTAGE,
    TWELVE_DATA
}
```

---

# 7. Regras de Negócio

## Corretoras

### RN01 — Validação e existência do CNPJ
Uma corretora somente poderá ser cadastrada quando:

1. o CNPJ for informado;
2. o CNPJ for normalizado;
3. possuir 14 dígitos;
4. passar pela validação dos dígitos verificadores;
5. existir na API cadastral consultada.

Um CNPJ matematicamente válido, mas não encontrado pela fonte externa, não será suficiente para realizar o cadastro.

---

### RN02 — Normalização do CNPJ
Antes de qualquer validação, consulta ou verificação de duplicidade, o CNPJ deverá ser normalizado, removendo pontos, barras, hífens e espaços.

Exemplo:

```text
12.345.678/0001-90
↓
12345678000190
```

Ordem obrigatória:

```text
recebe
↓
normaliza
↓
valida
↓
verifica duplicidade
↓
consulta API
```

---

### RN03 — Origem dos dados cadastrais
Os principais dados cadastrais da corretora deverão ser obtidos da integração externa.

O usuário não poderá substituir livremente dados oficiais retornados pela API.

Dados externos podem incluir:
- razão social;
- nome fantasia;
- situação cadastral;
- demais dados cadastrais disponíveis.

---

### RN04 — Situação cadastral
Uma empresa encontrada deverá possuir situação cadastral compatível com atividade vigente para prosseguir com a validação como corretora.

Empresas com situação incompatível com atividade operacional não deverão ser cadastradas.

---

### RN05 — Validação da instituição financeira
Depois da validação cadastral, o sistema deverá consultar a CVM ou fonte pública equivalente.

Somente instituições compatíveis/autorizadas segundo o critério adotado pelo projeto poderão ser cadastradas.

Nesta versão:

```text
instituição não validada
↓
cadastro recusado
```

---

### RN06 — Auditoria da validação financeira
Ao cadastrar uma corretora validada, o sistema deverá registrar:

- status da validação;
- data/hora da validação;
- fonte utilizada.

---

### RN07 — Validação do CEP
O CEP deverá ser normalizado e validado por API externa antes do cadastro.

Um CEP com formato válido, mas inexistente na fonte consultada, deverá impedir o cadastro.

---

### RN08 — Origem do endereço
Dados retornados pela API:

- CEP;
- logradouro;
- bairro;
- cidade;
- UF.

Dados complementares informados pelo usuário:

- número;
- complemento.

Os dados oficiais retornados pela API não devem ser sobrescritos arbitrariamente.

---

### RN09 — Unicidade da corretora
Não poderá existir mais de uma corretora com o mesmo CNPJ normalizado.

A unicidade deverá ser garantida:

1. pelo Service;
2. por constraint `UNIQUE` no banco.

---

# 8. Regras de Ações e Cotações

### RN10 — Normalização do ticker
Antes de qualquer validação:

- remover espaços externos;
- converter para maiúsculas.

Exemplo:

```text
" petr4 "
↓
"PETR4"
```

---

### RN11 — Mercado obrigatório
Toda ação deverá informar explicitamente seu mercado.

Mercados suportados inicialmente:

```text
BRASIL
EUA
```

O backend não deverá adivinhar silenciosamente o mercado.

---

### RN12 — Compatibilidade ticker × mercado
O ticker só será válido quando existir na integração correspondente ao mercado informado.

```text
BRASIL → API brasileira
EUA → API americana
```

---

### RN13 — Existência do ativo
Uma ação só poderá ser cadastrada quando seu ticker existir na API correspondente.

Não será permitido cadastrar ativos fictícios.

---

### RN14 — Seleção da integração de cotação
Integrações principais:

```text
BRASIL → BRAPI
EUA → Alpha Vantage
```

Twelve Data poderá existir como integração alternativa, sem fallback automático na primeira versão.

---

### RN15 — Dados externos do ativo
Os dados disponíveis retornados pela integração deverão enriquecer o cadastro:

- ticker;
- nome da empresa;
- mercado;
- moeda;
- cotação;
- data/hora da cotação.

---

### RN16 — Unicidade do ticker
Não poderá existir mais de uma ação com o mesmo ticker normalizado.

Nesta versão:

```text
UNIQUE(ticker)
```

Essa decisão segue a regra original do trabalho.

---

### RN17 — Cotação atual
`cotacaoAtual` representa a última cotação válida conhecida pela aplicação.

Não significa obrigatoriamente cotação em tempo real.

No frontend, preferir:

```text
Última cotação
```

ou:

```text
Cotação mais recente
```

---

### RN18 — Momento da cotação
Quando a API fornecer o horário da cotação, ele deverá ser preservado.

Não confundir:

```text
dataHoraCotacao
```

com:

```text
dataHoraRegistro
```

---

### RN19 — Atualização da cotação
Uma ação cadastrada poderá ter sua cotação atualizada.

Fluxo:

1. localizar a ação;
2. obter o mercado já armazenado;
3. chamar a integração correta;
4. validar a resposta;
5. registrar histórico;
6. atualizar `cotacaoAtual`;
7. atualizar `dataHoraCotacao`.

---

### RN20 — Atomicidade da atualização
Atualização da ação e gravação do histórico deverão ocorrer na mesma transação.

Ou tudo é salvo ou nada é salvo.

---

### RN21 — Significado do histórico
O histórico local representa as cotações efetivamente consultadas e persistidas pela aplicação.

Ele não representa necessariamente todo o histórico de negociação da bolsa.

---

### RN22 — Unicidade do ponto histórico
Uma observação histórica será identificada por:

```text
acao + dataHoraCotacao
```

O mesmo ponto temporal não deverá ser persistido duas vezes.

Constraint sugerida:

```text
UNIQUE(acao_id, data_hora_cotacao)
```

---

### RN23 — Fonte da cotação
Cada histórico deverá registrar a fonte utilizada:

```text
BRAPI
ALPHA_VANTAGE
TWELVE_DATA
```

---

# 9. Regras da Carteira

### RN24 — Conceito de carteira
Uma carteira representa um agrupamento de posições financeiras.

Pode possuir nenhuma ou várias posições.

---

### RN25 — Ação ≠ posição
`Acao` representa o ativo financeiro.

`AtivoCarteira` representa a posição do usuário.

Pertencem ao `AtivoCarteira`:

- quantidade;
- preço médio;
- corretora.

---

### RN26 — Inclusão de posição
Somente uma ação previamente cadastrada e validada poderá ser adicionada à carteira.

---

### RN27 — Quantidade
A quantidade deverá ser maior que zero.

Tipo:

```java
BigDecimal
```

---

### RN28 — Preço médio
Na primeira versão, o preço médio será informado pelo usuário.

Regras:

```text
precoMedio > 0
```

A cotação atual nunca deverá ser usada automaticamente como preço médio.

---

### RN29 — Data da primeira compra
A data da primeira compra não poderá ser futura.

```text
dataPrimeiraCompra <= data atual
```

---

### RN30 — Unicidade da posição
Uma posição consolidada será única pela combinação:

```text
carteira + ação + corretora
```

Permitido:

```text
Carteira Principal
├── PETR4 — XP
└── PETR4 — Rico
```

Não permitido:

```text
Carteira Principal
├── PETR4 — XP
└── PETR4 — XP
```

---

### RN31 — Relação com corretora
A corretora pertence à posição patrimonial, não à ação global.

---

### RN32 — Exclusão da carteira
Uma carteira com posições não poderá ser excluída diretamente.

As posições deverão ser removidas primeiro.

Resposta sugerida:

```text
409 Conflict
```

---

### RN33 — Exclusão da posição
Excluir um `AtivoCarteira` não poderá excluir:

- Acao;
- Corretora;
- Carteira;
- HistoricoCotacao.

Evitar `CascadeType.ALL` indiscriminadamente.

---

# 10. Dashboard

### RN34 — Indicadores calculados
O Dashboard não será uma entidade persistida.

Os valores serão calculados com base nos dados existentes.

#### Valor investido

```text
quantidade × precoMedio
```

#### Valor atual

```text
quantidade × cotacaoAtual
```

#### Resultado

```text
valorAtual - valorInvestido
```

#### Rentabilidade

```text
(resultado / valorInvestido) × 100
```

---

### RN35 — Cálculo seguro
Quando `valorInvestido` for zero, o sistema não poderá realizar divisão por zero.

A rentabilidade deverá ser zero ou não calculada.

---

### RN36 — Atualidade dos indicadores
O Dashboard deverá usar a última cotação válida armazenada.

O frontend deverá poder exibir:

```text
Última atualização:
25/08/2026 17:05
```

---

# 11. Regras de Integrações e Falhas

### RN37 — Falha externa ≠ dado inexistente
Falha na API externa não significa que o dado consultado não existe.

Exemplos:

```text
BRAPI offline ≠ ticker inexistente
BrasilAPI offline ≠ CNPJ inexistente
ViaCEP offline ≠ CEP inexistente
```

---

### RN38 — Cadastro depende da validação
Quando uma integração obrigatória estiver indisponível, o cadastro não deverá ser concluído com dados fictícios ou não confirmados.

---

### RN39 — Rate limit externo
Quando uma API externa atingir seu limite de requisições, o sistema deverá tratar a situação como indisponibilidade temporária.

Não deve invalidar o recurso consultado.

---

### RN40 — Resposta externa inconsistente
Uma resposta HTTP bem-sucedida, mas sem dados mínimos obrigatórios, deverá ser considerada inválida.

A aplicação não deverá persistir dados parciais.

---

### RN41 — Preservação da última cotação
Se uma atualização externa falhar, a última cotação válida existente deverá ser preservada.

Nunca:

```text
falha externa
↓
cotacaoAtual = null
```

---

# 12. Exceções

## Regras internas

```text
INVALID_CNPJ
CNPJ_NOT_FOUND
INSTITUTION_NOT_AUTHORIZED

INVALID_CEP
CEP_NOT_FOUND

INVALID_TICKER
TICKER_NOT_FOUND

DUPLICATE_RESOURCE
OBJECT_NOT_FOUND
```

## Integrações externas

```text
EXTERNAL_API_UNAVAILABLE
EXTERNAL_API_TIMEOUT
EXTERNAL_API_RATE_LIMIT
EXTERNAL_API_UNEXPECTED_RESPONSE
```

---

# 13. StandardError

Formato sugerido:

```json
{
  "timestamp": "2026-08-25T20:00:00-03:00",
  "status": 404,
  "error": "Recurso não encontrado",
  "code": "TICKER_NOT_FOUND",
  "message": "Ticker PETR99 não encontrado.",
  "path": "/api/v1/acoes"
}
```

O frontend deverá usar `code` para interpretar erros, não comparar mensagens.

---

# 14. Regras dos Adapters

Nenhum modelo específico de API externa deverá escapar do Adapter.

Errado:

```text
BrapiResponse
↓
AcaoService
```

Correto:

```text
BRAPI JSON
↓
BrapiResponse interno
↓
BrapiAdapter
↓
CotacaoExternaDTO
↓
Facade
↓
Service
```

O Adapter também deverá converter erros técnicos para exceções da aplicação.

---

# 15. Contrato de Cotação

```java
public interface CotacaoAdapter {

    boolean suporta(Mercado mercado);

    CotacaoExternaDTO buscarCotacao(String ticker);
}
```

A Facade poderá receber:

```java
List<CotacaoAdapter>
```

e selecionar o Adapter utilizando:

```java
adapter.suporta(mercado)
```

---

# 16. Fluxo de Cadastro da Corretora

```text
POST /corretoras
       ↓
normaliza CNPJ
       ↓
valida dígitos
       ↓
verifica duplicidade
       ↓
EmpresaFacade
       ↓
BrasilApiAdapter
       ↓
empresa existe?
       ↓
situação válida?
       ↓
InstituicaoFinanceiraFacade
       ↓
CvmAdapter
       ↓
instituição autorizada?
       ↓
normaliza CEP
       ↓
EnderecoFacade
       ↓
ViaCepAdapter
       ↓
CEP existe?
       ↓
monta Corretora
       ↓
Repository
       ↓
Database
```

Qualquer falha de validação obrigatória impede o salvamento.

---

# 17. Fluxo de Cadastro da Ação

```text
POST /acoes
       ↓
normaliza ticker
       ↓
valida entrada
       ↓
verifica duplicidade
       ↓
valida mercado
       ↓
CotacaoFacade
       ↓
seleciona CotacaoAdapter
       ↓
BRASIL → BRAPI
EUA → Alpha Vantage
       ↓
ticker existe?
       ↓
obtém dados externos
       ↓
cria Acao
       ↓
cria HistoricoCotacao
       ↓
Repository
       ↓
Database
```

Cadastro de ação e histórico inicial deverão ocorrer de forma transacional.

---

# 18. Endpoints

## Ações

```http
POST /api/v1/acoes
GET /api/v1/acoes
GET /api/v1/acoes/{id}
GET /api/v1/acoes/ticker/{ticker}
PUT /api/v1/acoes/{id}/atualizar-cotacao
GET /api/v1/acoes/{id}/historico
```

---

## Corretoras

```http
POST /api/v1/corretoras
GET /api/v1/corretoras
GET /api/v1/corretoras/{id}
GET /api/v1/corretoras/cnpj/{cnpj}
```

---

## Carteiras

```http
POST /api/v1/carteiras
GET /api/v1/carteiras
GET /api/v1/carteiras/{id}
PUT /api/v1/carteiras/{id}
DELETE /api/v1/carteiras/{id}
```

---

## Ativos da Carteira

```http
POST /api/v1/carteiras/{carteiraId}/ativos
GET /api/v1/carteiras/{carteiraId}/ativos
PUT /api/v1/carteiras/{carteiraId}/ativos/{ativoId}
DELETE /api/v1/carteiras/{carteiraId}/ativos/{ativoId}
```

---

## Dashboard

```http
GET /api/v1/dashboard/carteiras/{carteiraId}
```

---

# 19. Paginação

Aplicar desde o início em listagens.

Exemplos:

```http
GET /api/v1/acoes?page=0&size=20
GET /api/v1/corretoras?page=0&size=20
GET /api/v1/carteiras?page=0&size=20
```

Ordenação padrão:

```text
Ações → ticker ASC
Corretoras → razaoSocial ASC
Histórico → dataHoraCotacao DESC
```

---

# 20. Persistência

Garantias mínimas no banco:

```text
acao.ticker UNIQUE
corretora.cnpj UNIQUE
historico (acao_id, data_hora_cotacao) UNIQUE
ativo_carteira (carteira_id, acao_id, corretora_id) UNIQUE
```

Valores financeiros devem usar:

```java
BigDecimal
```

Nunca usar `double`.

---

# 21. Transações

Operações de escrita:

```java
@Transactional
```

Consultas, quando apropriado:

```java
@Transactional(readOnly = true)
```

Operações críticas:

- cadastro da ação + histórico inicial;
- atualização da ação + histórico;
- criação de posição;
- alterações dependentes de múltiplas persistências.

---

# 22. Configuração das APIs

URLs e tokens nunca devem ficar hardcoded.

Exemplo:

```yaml
integrations:
  brapi:
    base-url: ${BRAPI_URL}
    token: ${BRAPI_TOKEN}

  alpha-vantage:
    base-url: ${ALPHA_VANTAGE_URL}
    api-key: ${ALPHA_VANTAGE_API_KEY}

  brasil-api:
    base-url: ${BRASIL_API_URL}

  viacep:
    base-url: ${VIACEP_URL}
```

---

# 23. Timeout

Toda integração deverá possuir:

```text
connection timeout
read timeout
```

Nenhuma requisição externa poderá aguardar indefinidamente.

---

# 24. Cache

Na primeira versão:

```text
CEP → permitido
CNPJ → permitido
cotação → sem cache inicialmente
```

Validação financeira poderá receber cache futuramente com TTL definido.

---

# 25. Swagger

Documentar desde o início:

- endpoints;
- Request DTOs;
- Response DTOs;
- status HTTP;
- códigos de erro;
- exemplos de requisição;
- exemplos de resposta.

---

# 26. CORS

Configuração centralizada.

Ambiente de desenvolvimento:

```text
http://localhost:4200
```

Evitar `@CrossOrigin` espalhado pelos Resources.

---

# 27. Testes

## Services
Testar:

- normalização;
- duplicidade;
- validações;
- persistência;
- regras da carteira;
- atualização da cotação;
- histórico.

## Facades
Testar:

```text
BRASIL → BrapiAdapter
EUA → AlphaVantageAdapter
mercado sem Adapter → erro
```

## Adapters
Testar:

- resposta correta;
- JSON inválido;
- 404;
- 429;
- 500;
- timeout;
- resposta incompleta.

## Integração
Utilizar:

```text
@SpringBootTest
MockMvc
H2
```

As APIs externas deverão ser mockadas na maioria dos testes.

---

# 28. Casos de Teste Obrigatórios

```text
CNPJ inválido
CNPJ inexistente
CNPJ duplicado
empresa inativa
instituição não autorizada

CEP inválido
CEP inexistente

ticker inválido
ticker inexistente
ticker duplicado
ticker incompatível com mercado

ação brasileira
ação americana

API indisponível
timeout
rate limit
resposta externa inválida

atualização de cotação
preservação da cotação anterior em caso de falha
histórico duplicado

posição duplicada
quantidade inválida
preço médio inválido
data futura

exclusão de carteira com posições
```

---

# 29. Angular

Estrutura sugerida:

```text
src/app
│
├── core
│   ├── services
│   ├── interceptors
│   └── guards
│
├── shared
│   ├── components
│   ├── models
│   └── utils
│
├── features
│   ├── dashboard
│   ├── acoes
│   ├── corretoras
│   ├── carteiras
│   └── historico
│
├── app.routes.ts
├── app.config.ts
└── app.component.ts
```

O Angular nunca deverá consumir APIs externas diretamente.

Correto:

```text
Angular
↓
Spring Boot
↓
Facade
↓
Adapter
↓
API externa
```

---

# 30. Ordem Recomendada de Desenvolvimento

```text
ETAPA 01 — Criação do projeto Spring Boot / Java 21
ETAPA 02 — Estrutura de packages
ETAPA 03 — Enums
ETAPA 04 — Domains
ETAPA 05 — Repositories
ETAPA 06 — DTOs
ETAPA 07 — Mappers
ETAPA 08 — Exceptions
ETAPA 09 — Services
ETAPA 10 — Facades
ETAPA 11 — BRAPI Adapter
ETAPA 12 — BrasilAPI Adapter
ETAPA 13 — ViaCEP Adapter
ETAPA 14 — CVM Adapter
ETAPA 15 — Alpha Vantage Adapter
ETAPA 16 — Ações
ETAPA 17 — Corretoras
ETAPA 18 — Carteira
ETAPA 19 — Histórico
ETAPA 20 — Dashboard
ETAPA 21 — Swagger
ETAPA 22 — Testes
ETAPA 23 — Angular
ETAPA 24 — Integração Angular ↔ Spring
```

---

# 31. Prioridade de Escopo

## Fase 1 — Núcleo obrigatório

```text
Corretora
Ação
CNPJ
CEP
Validação financeira
Cotações
Persistência
Tratamento de erros
Integrações externas
Swagger
Testes
```

## Fase 2 — Diferenciais

```text
Carteira
Histórico
Dashboard
Paginação
Cache
Frontend Angular
```

## Fase 3 — Evoluções futuras

```text
Movimentações de compra e venda
Cálculo automático de preço médio
Dividendos
FIIs
ETFs
Autenticação
Spring Security
JWT
Usuários
Comparação com índices
Mais mercados
Atualização automática de cotações
```

---

# 32. Observações Finais

A arquitetura principal adotada será:

```text
Resource
↓
Service
↓
Facade
↓
Adapter
↓
API externa
```

e:

```text
Service
↓
Repository
↓
Database
```

As regras de negócio devem permanecer concentradas nos Services.

As APIs externas deverão permanecer isoladas pelos Adapters.

As Facades deverão apenas orquestrar integrações.

Os DTOs externos não poderão vazar para o domínio.

O sistema deverá preservar dados válidos existentes quando uma integração externa falhar.

A primeira versão deverá priorizar os requisitos obrigatórios do projeto antes dos diferenciais.
