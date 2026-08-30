## Why

O dashboard atual ainda falha como uma unidade: a indisponibilidade de indicadores, proventos ou evolução pode ocultar os dados financeiros principais que o backend já calculou. A experiência também não oferece a seleção de carteira, visualizações e estados de feedback descritos para tornar os dados reais compreensíveis em desktop e celular.

## What Changes

- Tornar o carregamento do dashboard tolerante a falhas parciais: o resumo financeiro e as posições permanecem visíveis quando fontes opcionais estiverem indisponíveis.
- Adicionar seletor de carteira, atualização manual e estados claros de carregamento, carteira vazia, erro principal e indisponibilidade parcial.
- Apresentar evolução patrimonial em SVG e distribuição por ativo com CSS, sem adicionar bibliotecas de gráficos.
- Modernizar a estrutura de navegação e o design system para um layout responsivo, com menu móvel e tabelas utilizáveis em telas estreitas.
- Cobrir a degradação parcial do dashboard com testes automatizados e manter o frontend consumindo exclusivamente dados financeiros retornados pelo backend.

## Capabilities

### New Capabilities

- `portfolio-dashboard-experience`: experiência Angular resiliente para visualizar e navegar pelos dados de uma carteira.

### Modified Capabilities

Nenhuma. Os contratos e os cálculos do backend em `portfolio-dashboard` permanecem inalterados; este change define como o frontend consome e apresenta esses dados.

## Impact

Afeta `frontend/src/app/features/insights-dashboard.page.ts`, a estrutura e estilos globais da aplicação, os contratos e serviços Angular de carteiras, e os testes do dashboard. Continua usando os endpoints versionados existentes em `/api/v1`, o proxy de desenvolvimento para o backend na porta 8081 e não adiciona dependências de gráficos nem cálculos financeiros no navegador.
