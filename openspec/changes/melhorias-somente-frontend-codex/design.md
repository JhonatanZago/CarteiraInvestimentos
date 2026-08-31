## Context

Consulte a especificação existente `openspec/specs/MELHORIAS_SOMENTE_FRONTEND_CODEX.md`, que restringe o trabalho ao frontend e proíbe alterações de backend, adapters e novas infraestruturas de smoke test.

## Goals / Non-Goals

**Goals:** Melhorar telas existentes, feedback, acessibilidade básica e CSS responsivo.

**Non-Goals:** Criar entidades, alterar APIs, reescrever backend, mudar adapters ou criar testes end-to-end.

## Decisions

- Reutilizar serviços, rotas e modelos atuais.
- Preferir componentes e estilos já presentes, com alterações incrementais.
- Normalizar apenas valores de entrada no frontend e manter valores em falhas.
- Validar com `npm run build` e `npm test -- --run`.

## Risks / Trade-offs

- [Regressão visual] → validar todas as rotas existentes após cada alteração.
- [Codificação legada em templates] → preservar arquivos e alterar somente trechos necessários.
