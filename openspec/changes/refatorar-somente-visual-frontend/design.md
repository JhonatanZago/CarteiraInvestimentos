## Context

O frontend atual possui componentes funcionais estáveis com templates inline e estilos globais legados. A implementação deve limitar-se à camada visual, mantendo os bindings e a estrutura de execução existentes.

## Goals / Non-Goals

**Goals:**

- Consolidar tokens de identidade azul-petróleo e verde-esmeralda.
- Melhorar hierarquia visual, responsividade, estados e navegação.
- Manter os templates compatíveis com todos os bindings atuais.

**Non-Goals:**

- Alterar rotas, serviços, modelos, APIs, backend ou lógica de componentes.
- Criar páginas substitutas, wrappers ou novos contratos funcionais.

## Decisions

- Usar `styles.scss` para tokens e base global, reservando SCSS encapsulado para cada componente visual; isso evita vazamento de estilos e mantém a navegação independente do shell.
- Reorganizar somente marcação já existente, preservando nomes de controles, eventos, condições e iterações; essa abordagem mantém o fluxo aprovado e reduz risco de regressão.
- Aplicar media queries progressivas para tablet e celular, incluindo rolagem horizontal em tabelas; isso preserva todos os dados sem quebrar layouts estreitos.
- Validar com TypeScript, testes e build antes e depois das alterações; em caso de regressão, restaurar o backup visual criado na raiz.

## Risks / Trade-offs

- [Bindings podem ser removidos durante a reorganização] → comparar templates e executar a suíte completa antes do aceite.
- [Estilos globais podem afetar componentes existentes] → limitar seletores globais a tokens/base e usar estilos encapsulados quando possível.
- [Diferenças entre viewports] → validar desktop, tablet e viewport móvel conforme roteiro.

## Migration Plan

1. Criar backup somente da pasta `frontend`.
2. Executar validações baseline.
3. Aplicar tokens, shell, navegação e estilos/templates visuais permitidos.
4. Executar validações e testes manuais de fluxos existentes.
5. Em caso de regressão funcional, restaurar o backup e corrigir apenas a apresentação.
