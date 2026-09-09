# Baseline — TASK 0.3

Data da execução: 2026-09-06.

## Backend

- Comando: `./gradlew test --no-daemon --console=plain`
- Resultado: aprovado (`BUILD SUCCESSFUL`)
- Relatórios: 25 suítes XML, 59 testes, 0 falhas/erros e 1 teste ignorado.
- Observação: a primeira tentativa sem `GRADLE_USER_HOME` falhou porque o wrapper tentou criar `C:\\.gradle`; a execução foi repetida com `.gradle-user-home` dentro do projeto.

## Frontend

- Comando: `npx tsc -p tsconfig.app.json --noEmit`
- Resultado: aprovado.
- Comando: `npm test -- --watch=false`
- Resultado: falhou durante o bundle de testes antes da execução. O Angular não conseguiu resolver arquivos locais (`src/styles.scss`, `app.scss`, SCSS dos logos e specs) e também reportou `Access is denied` ao ler `../../..`, além de não resolver pacotes Angular. Não houve contagem de testes executados nesta tentativa.
- Comando: `npm run build`
- Resultado: falhou durante o bundle pelos mesmos erros de resolução/permissão (`Access is denied` em `../../..`, `src/styles.scss`, `src/main.ts` e SCSS dos componentes).

## Classificação

- Problema confirmado: os comandos Angular de teste/build estão bloqueados no ambiente atual por erro de resolução/permissão do bundler, não por erro TypeScript.
- Risco: executar em outro ambiente/terminal pode produzir resultado diferente; a causa deve ser investigada antes da implementação das correções.
- Nenhuma correção de produção foi aplicada nesta task.
