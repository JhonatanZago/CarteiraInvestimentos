## Why

O frontend atual precisa consumir de ponta a ponta os dados e erros do backend para que o produto possa ser usado como uma plataforma de investimentos, e não apenas como telas isoladas. A interface também precisa de feedback, responsividade e visualização clara dos indicadores que já são calculados pelo backend.

## What Changes

- Integrar todas as funcionalidades Angular aos endpoints REST versionados do Spring Boot por meio de serviços HTTP tipados.
- Centralizar configuração da API, tratamento de erros, notificações e estados de carregamento no frontend.
- Conectar as telas de ações, corretoras, carteiras, posições, histórico e dashboard aos dados reais do backend.
- Evoluir a experiência visual com navegação responsiva, cards de indicadores, tabelas interativas, filtros, paginação, confirmações e estados vazios.
- Exibir gráficos e indicadores exclusivamente a partir de dados fornecidos pela API, sem transferir regras financeiras ou integrações externas para o navegador.
- Adicionar testes de serviços, componentes, interceptadores e fluxos integrados, além do build final do Angular.

## Capabilities

### New Capabilities

- `investment-frontend-experience`: consumo tipado e seguro da API, fluxos de gerenciamento de investimentos e experiência visual responsiva para a aplicação Angular.

### Modified Capabilities

Nenhuma. As regras de negócio, cálculos financeiros e integrações externas permanecem nos contratos existentes do backend.

## Impact

Afeta o projeto Angular em `frontend/`, sua configuração de ambiente, rotas, componentes e testes. O backend mantém os endpoints `/api/v1` e a configuração CORS centralizada; poderão ser necessários apenas ajustes de contrato documentados caso algum dado essencial de visualização ainda não esteja exposto pela API.
