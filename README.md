# 📈 CarteiraInvestimentos

Aplicação web para **gerenciamento de carteiras de investimentos**, desenvolvida com **Java 21 + Spring Boot** no backend e **Angular** no frontend.

O projeto permite cadastrar e acompanhar **carteiras, ativos, corretoras, compras e vendas**, além de integrar informações provenientes de APIs externas para enriquecer os dados financeiros e cadastrais utilizados pelo sistema.

O objetivo é centralizar o gerenciamento dos investimentos em uma interface moderna, permitindo visualizar patrimônio, posições, resultados e histórico de operações de forma simples e organizada.

---

## 📌 Sobre o projeto

O **CarteiraInvestimentos** foi desenvolvido inicialmente como um projeto acadêmico voltado ao consumo de APIs externas e ao gerenciamento de ações e corretoras.

A proposta foi evoluída para uma aplicação mais completa de acompanhamento de investimentos, mantendo como princípios:

* Separação de responsabilidades;
* Arquitetura em camadas;
* Integração com APIs externas;
* Validação de dados;
* Persistência em banco de dados;
* Tratamento centralizado de erros;
* Interface web integrada ao backend;
* Organização e manutenção do código.

---

## ✨ Funcionalidades

### 👤 Usuários

A aplicação possui autenticação para separar os dados de cada usuário.

O fluxo permite:

* Criar uma conta;
* Realizar login;
* Acessar os dados da conta autenticada;
* Sair da conta;
* Manter carteiras e operações separadas entre usuários.

---

### 💼 Carteiras

Permite criar carteiras para organizar os investimentos.

Cada carteira pode apresentar:

* Valor investido;
* Valor atual;
* Resultado;
* Quantidade de ativos;
* Posições;
* Ativos vinculados;
* Corretoras utilizadas.

O usuário pode selecionar uma carteira e acessar sua visão detalhada.

---

### 🛒 Compras e posições

O sistema permite registrar compras de ativos dentro das carteiras.

Um mesmo ativo pode possuir **múltiplas operações de compra**, possibilitando o cálculo correto da posição e do preço médio.

Exemplo:

```text
Compra 1
10 PETR4 × R$ 30,00 = R$ 300,00

Compra 2
10 PETR4 × R$ 34,00 = R$ 340,00

Total investido = R$ 640,00
Quantidade = 20 ações
Preço médio = R$ 32,00
```

As compras permanecem registradas como operações independentes, enquanto a posição consolidada representa o total daquele ativo.

---

### 📈 Ativos

Os ativos são cadastrados utilizando seus respectivos **tickers**.

Exemplos:

```text
PETR4
VALE3
ITUB4
WEGE3
AAPL
MSFT
```

Dependendo do mercado e da integração disponível, podem ser obtidas informações como:

* Ticker;
* Nome da empresa;
* Mercado;
* Moeda;
* Cotação;
* Data/hora da cotação;
* Logo do ativo;
* Informações disponibilizadas pela API externa.

O projeto contempla ativos brasileiros e internacionais.

---

### 🏦 Corretoras

O sistema permite cadastrar corretoras e consultar informações empresariais utilizando o **CNPJ**.

Entre os dados tratados estão:

* CNPJ;
* Razão social;
* Nome fantasia;
* CEP;
* Logradouro;
* Número;
* Complemento;
* Bairro;
* Cidade;
* UF;
* Situação cadastral;
* Informações de validação.

O projeto mantém separada a consulta cadastral do CNPJ da validação regulatória da instituição.

Também existe suporte para revalidar informações empresariais previamente cadastradas.

---

### 💰 Vendas

A área de vendas registra as operações realizadas sobre os ativos.

O histórico permite acompanhar informações como:

* Ativo;
* Quantidade;
* Preço médio;
* Preço de venda;
* Valor da operação;
* Resultado;
* Data da operação.

Os resultados são destacados visualmente:

```text
🟢 Lucro
🔴 Prejuízo
```

Isso facilita a identificação das operações positivas e negativas.

---

### 📊 Dashboard

O sistema possui uma área de visão geral para acompanhar as principais informações das carteiras.

Entre os dados apresentados pela interface estão:

* Patrimônio;
* Total investido;
* Resultado;
* Rentabilidade;
* Quantidade de ativos;
* Distribuição dos investimentos;
* Evolução patrimonial;
* Posições consolidadas;
* Histórico de operações.

---

## 🌎 Mercados e moedas

O projeto foi estruturado para trabalhar com ativos de diferentes mercados.

### 🇧🇷 Brasil

Ativos brasileiros utilizam principalmente:

```text
BRL — Real Brasileiro
```

Exemplos:

```text
PETR4
VALE3
ITUB4
WEGE3
```

### 🇺🇸 Estados Unidos

Ativos americanos podem utilizar:

```text
USD — Dólar Americano
```

Exemplos:

```text
AAPL
MSFT
GOOGL
NVDA
```

Quando necessário, valores em USD podem ser convertidos para BRL para permitir a consolidação do patrimônio.

---

# 🛠️ Tecnologias

## Backend

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* Jakarta Validation
* Gradle
* H2 Database

## Frontend

* Angular
* TypeScript
* HTML
* CSS

## Desenvolvimento e testes

* Git
* GitHub
* Postman
* IntelliJ IDEA
* Angular CLI

---

# 🏗️ Arquitetura

O backend utiliza uma arquitetura organizada em camadas.

```text
┌───────────────────────┐
│      Controller       │
│       Resource        │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│        Service        │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│        Facade         │
└───────────┬───────────┘
            │
       ┌────┴────┐
       ▼         ▼
 Repository    Adapter
       │         │
       ▼         ▼
   Database   APIs externas
```

A ideia é impedir que as regras de negócio fiquem diretamente dependentes das implementações de serviços externos.

---

## 🔌 Adapter

Os adapters isolam a comunicação com serviços externos.

Exemplo:

```text
Service
   │
   ▼
Adapter
   │
   ▼
API externa
```

Caso uma API seja substituída futuramente, a alteração pode ficar concentrada na camada de integração.

---

## 🎯 Facade

As facades podem coordenar operações que dependem de múltiplos componentes.

Exemplo:

```text
Cadastro de corretora
        │
        ▼
CorretoraFacade
        │
        ├── Consulta CNPJ
        ├── Consulta endereço
        ├── Valida informações
        │
        ▼
     Service
        │
        ▼
    Repository
```

---

# 📂 Estrutura do projeto

Estrutura simplificada:

```text
CarteiraInvestimentos/
│
├── frontend/
│   ├── src/
│   ├── angular.json
│   ├── package.json
│   └── ...
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ...
│   │   │       ├── config/
│   │   │       ├── domain/
│   │   │       ├── dto/
│   │   │       ├── enums/
│   │   │       ├── exception/
│   │   │       ├── integration/
│   │   │       ├── mapper/
│   │   │       ├── repository/
│   │   │       ├── resource/
│   │   │       └── service/
│   │   │
│   │   └── resources/
│   │
│   └── test/
│
├── openspec/
├── docker-compose.yml
├── env.example
├── build.gradle
├── CONFIGURACAO_INTEGRACOES.md
└── README.md
```

---

# 🔗 Integrações externas

O projeto utiliza APIs externas para evitar depender exclusivamente de dados cadastrados manualmente.

As integrações previstas/documentadas no projeto incluem:

### 📈 BRAPI

Utilizada para dados relacionados ao mercado financeiro brasileiro.

Pode fornecer informações como:

* Ticker;
* Cotação;
* Empresa;
* Dados de mercado;
* Logo do ativo.

Dependendo do endpoint utilizado, pode ser necessário configurar um token.

---

### 🏢 BrasilAPI

Utilizada para consultas de dados brasileiros.

No projeto, é utilizada principalmente na consulta de informações empresariais através do CNPJ.

Fluxo simplificado:

```text
CNPJ informado
      │
      ▼
Normalização
      │
      ▼
BrasilAPI
      │
      ▼
Dados empresariais
      │
      ▼
Validação
      │
      ▼
Persistência
```

---

### 📍 ViaCEP

Pode ser utilizado para consultar e validar informações de endereço através do CEP.

```text
CEP
 │
 ▼
ViaCEP
 │
 ├── Logradouro
 ├── Bairro
 ├── Cidade
 └── UF
```

---

### 🇺🇸 Alpha Vantage / Twelve Data

Serviços considerados no projeto para consulta de informações relacionadas ao mercado internacional.

Esses serviços podem exigir API Key e possuir limites de requisições dependendo do plano utilizado.

---

# ⚙️ Configuração do ambiente

## Pré-requisitos

Antes de executar o projeto, tenha instalado:

* Java 21 ou superior;
* Node.js;
* npm;
* Angular CLI;
* Git.

Verifique as instalações:

```bash
java --version
node --version
npm --version
ng version
git --version
```

---

# 📥 Clonando o projeto

Utilizando SSH:

```bash
git clone git@github.com:JhonatanZago/CarteiraInvestimentos.git
```

Entre na pasta:

```bash
cd CarteiraInvestimentos
```

---

# ☕ Executando o backend

Na raiz do projeto:

### Windows

```bash
gradlew.bat bootRun
```

### Linux / macOS

```bash
./gradlew bootRun
```

Por padrão, o backend do projeto é executado em:

```text
http://localhost:8081
```

---

# 🅰️ Executando o frontend

Abra outro terminal e entre na pasta:

```bash
cd frontend
```

Instale as dependências:

```bash
npm install
```

Execute:

```bash
ng serve
```

ou:

```bash
npm start
```

Depois acesse:

```text
http://localhost:4200
```

---

# 🔑 Variáveis de ambiente

Algumas integrações podem exigir tokens ou chaves.

Exemplo:

```env
BRAPI_TOKEN=
ALPHA_VANTAGE_API_KEY=
TWELVE_DATA_API_KEY=
```

> ⚠️ Nunca envie tokens ou API Keys reais para o GitHub.

Utilize um arquivo de exemplo:

```text
.env.example
```

e mantenha arquivos contendo credenciais reais no `.gitignore`.

---

# 🗄️ Banco de dados

Durante o desenvolvimento, o projeto utiliza **H2 Database**.

O H2 facilita:

* Desenvolvimento local;
* Testes;
* Inicialização rápida;
* Validação das entidades.

A arquitetura também permite evolução para bancos relacionais como PostgreSQL ou MySQL mediante configuração.

---

# 🌐 API REST

A API utiliza o prefixo:

```text
/api/v1
```

## Corretoras

Exemplos de rotas:

```http
POST /api/v1/corretoras
GET  /api/v1/corretoras
GET  /api/v1/corretoras/{id}
```

Revalidação empresarial:

```http
POST /api/v1/corretoras/{id}/revalidacao-empresarial
```

---

## Ações

Exemplos:

```http
POST /api/v1/acoes
GET  /api/v1/acoes
GET  /api/v1/acoes/{id}
```

Atualização/revalidação:

```http
POST /api/v1/acoes/revalidacao-em-lote
```

---

## Carteiras

Exemplos:

```http
GET  /api/v1/carteiras
POST /api/v1/carteiras
GET  /api/v1/carteiras/{id}
```

> A relação completa de endpoints deve ser consultada diretamente na versão atual da API do projeto.

---

# ⚠️ Tratamento de erros

O backend utiliza tratamento centralizado de exceções para evitar respostas inconsistentes.

Entre os cenários considerados estão:

* CNPJ inválido;
* CEP inválido;
* Ticker inexistente;
* Ativo não encontrado;
* Corretora não encontrada;
* Dados inválidos;
* Falha em API externa;
* Limite de requisições;
* Recurso inexistente.

Exemplo de resposta:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Ativo não encontrado"
}
```

---

# 📐 Principais regras de negócio

Algumas das regras consideradas no desenvolvimento são:

1. O CNPJ deve ser validado antes do cadastro da corretora.
2. Dados empresariais devem ser consultados em uma fonte externa quando aplicável.
3. CEPs devem ser validados antes da persistência quando a integração estiver habilitada.
4. O ticker informado deve corresponder a um ativo válido na fonte de mercado utilizada.
5. Ativos brasileiros e americanos devem ser diferenciados.
6. Cada ativo deve manter sua moeda e mercado de origem.
7. Uma carteira pode possuir diversas posições.
8. O mesmo ativo pode receber múltiplas operações de compra.
9. Compras sucessivas devem participar corretamente do cálculo do preço médio.
10. As vendas devem registrar os dados necessários para determinar o resultado da operação.
11. Lucros e prejuízos devem ser apresentados de maneira visualmente distinta.
12. Falhas de serviços externos devem ser tratadas pela aplicação.

---

# 🧪 Testes

## Backend

No Windows:

```bash
gradlew.bat test
```

Linux/macOS:

```bash
./gradlew test
```

---

## Frontend

Entre na pasta:

```bash
cd frontend
```

Execute os testes disponíveis:

```bash
npm test
```

Valide também o build:

```bash
npm run build
```

---

# 🔄 Fluxo da aplicação

```text
                    ┌─────────────────┐
                    │     Usuário     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │     Angular     │
                    │    Frontend     │
                    └────────┬────────┘
                             │
                          HTTP/JSON
                             │
                             ▼
                    ┌─────────────────┐
                    │   Spring Boot   │
                    │    REST API     │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
          Services        Facades        Adapters
              │                              │
              ▼                         APIs externas
         Repositories                         │
              │                ┌──────────────┼────────────┐
              ▼                ▼              ▼            ▼
        Banco de dados      BRAPI        BrasilAPI      ViaCEP
```

---

# 🚧 Status do projeto

O projeto continua em desenvolvimento e pode receber novas funcionalidades, melhorias visuais, otimizações e ajustes nas integrações externas.

Entre as evoluções possíveis estão:

* Indicadores fundamentalistas;
* Proventos e dividendos;
* Histórico mais completo de cotações;
* Notificações;
* Comparação com índices;
* Histórico cambial;
* Exportação de relatórios;
* Maior cobertura de testes;
* Dockerização;
* Deploy em nuvem.

---

# 🎓 Contexto acadêmico

O projeto permite aplicar na prática conceitos como:

* Programação Orientada a Objetos;
* APIs REST;
* Spring Boot;
* Angular;
* Arquitetura em camadas;
* DTOs;
* Mappers;
* Services;
* Repositories;
* Facade;
* Adapter;
* Persistência;
* Banco de dados relacional;
* Consumo de APIs;
* Validação;
* Tratamento de exceções;
* Integração frontend/backend;
* Git e GitHub.

---

# 👨‍💻 Autor

**Jhonatan Zago**

Estudante de **Sistemas de Informação**.

Projeto desenvolvido para fins acadêmicos, estudos e aplicação prática de conhecimentos em desenvolvimento de software.

### Stack principal

`Java 21` `Spring Boot` `Angular` `TypeScript` `REST API` `JPA` `Hibernate` `H2` `Gradle` `Git`

---

## ⚠️ Aviso

As informações financeiras exibidas pela aplicação dependem das APIs externas configuradas.

A disponibilidade, atualização, precisão e limites das informações podem variar de acordo com cada serviço utilizado.

Este projeto possui finalidade **acadêmica e educacional** e não constitui recomendação de investimento.

---

## 📄 Licença

Projeto desenvolvido para fins acadêmicos e educacionais.

---
