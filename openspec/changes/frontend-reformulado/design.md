## Context

A especificação `openspec/specs/FRONTEND_REFORMULADO.md` restringe a entrega ao frontend Angular e exige preservação dos contratos backend.

## Goals / Non-Goals

**Goals:** Reorganizar componentes existentes, melhorar formulários, feedback, navegação e responsividade.

**Non-Goals:** Alterar Java, banco, endpoints, adapters ou regras de negócio.

## Decisions

- Reutilizar serviços e modelos HTTP existentes.
- Separar páginas por domínio quando isso reduzir complexidade.
- Usar CSS responsivo, labels persistentes e estados explícitos.
- Normalizar entradas no frontend e manter valores após falhas.

## Risks / Trade-offs

- [Templates legados] → migrar incrementalmente e validar build a cada grupo.
- [APIs indisponíveis] → exibir estados de erro sem dados fictícios.
