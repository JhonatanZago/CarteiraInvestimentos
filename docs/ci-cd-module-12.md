# Módulo 12 — CI/CD

## Checks obrigatórios

O workflow `.github/workflows/ci.yml` cria três checks independentes:

- `Backend validation`
- `Frontend validation`
- `OpenSpec validation`

Todos devem passar na proteção da branch principal. O workflow executa em Pull Requests e em pushes para `main`. Não existe job de deploy.

## Política de execução

- Backend: Java 21, Gradle Wrapper 9.5.1, `clean test`, build sem repetir testes e SpotBugs/FindSecBugs.
- Frontend: Node 22.22.3, npm 12.0.2, `npm ci`, testes headless e build Angular.
- OpenSpec: CLI `@fission-ai/openspec@1.14.3` e validação strict da change.
- Falhas de compilação, testes ou análise estática bloqueiam o check.
- Testes ignorados permanecem visíveis nos relatórios; o smoke PostgreSQL só executa quando `smoke.db.url` está configurado.

## Artefatos

- `backend-validation-reports`: resultados JUnit, relatórios de testes Gradle e SpotBugs.
- `frontend-validation-reports`: build `frontend/dist/frontend` e relatórios disponíveis de testes/cobertura.
- `openspec-validation-report`: saída de `openspec validate ... --strict`.

Uploads usam condição equivalente a `always()` e `if-no-files-found: warn`. Não são publicados `.env`, tokens, caches ou `node_modules`.

## Cache e validação local

O CI usa caches gerenciados pelas actions oficiais para Gradle e npm. Os caches locais `.gradle-user-home-*` não são usados pelo GitHub Actions.

Para reproduzir localmente:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\validar-ci-local.ps1
```

O script usa `GRADLE_USER_HOME` apenas no processo e grava evidências em `.ci-local-reports/`, diretório ignorado.

## Segurança e limites

- Permissão do workflow: `contents: read`.
- Nenhum segredo real de BRAPI, JWT ou deploy é fornecido.
- O trigger é `pull_request`, não `pull_request_target`.
- Não há merge automático, deploy ou aprovação automática de riscos ZAP.
