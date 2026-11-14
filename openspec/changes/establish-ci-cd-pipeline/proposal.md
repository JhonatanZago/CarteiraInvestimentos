# Proposal

## Why

O projeto possui comandos reproduzíveis de backend, frontend e análise de segurança, mas ainda não possui uma execução automatizada no GitHub Actions. Isso permite que falhas de compilação, testes, análise estática ou validação OpenSpec cheguem à branch principal sem uma evidência consistente; o planejamento do Módulo 12 define essa barreira antes de qualquer implementação ou deploy.

## What Changes

- Criar uma pipeline GitHub Actions para Pull Requests e para a branch principal.
- Executar backend com Java 21, Gradle Wrapper 9.5.1, testes, build e SpotBugs/FindSecBugs.
- Executar frontend com Node/npm compatíveis, `npm ci`, testes sem watch e build de produção.
- Validar a change OpenSpec e publicar relatórios de testes e análise como artefatos.
- Configurar caches nativos do GitHub Actions para Gradle e npm, sem depender dos caches locais `.gradle-user-home-*`.
- Aplicar permissões mínimas, ausência de deploy automático e nenhuma credencial real no workflow.
- Preservar o comportamento fail-closed do Security Gate; riscos ZAP e a pendência BRAPI não serão aprovados pela pipeline.

## Capabilities

### New Capabilities

- `ci-cd-pipeline`: Execução automatizada, reproduzível e fail-closed de build, testes, análise estática e validação OpenSpec para backend e frontend.

### Modified Capabilities

Nenhuma capacidade funcional existente tem seus requisitos alterados; a mudança adiciona automação de verificação do repositório.

## Impact

O escopo de implementação futuro incluirá `.github/workflows/`, configurações de cache e publicação de artefatos, sem alterar código de domínio, APIs, autenticação, migrations, regras financeiras ou integração BRAPI. A pipeline usará Maven Central e o Gradle Wrapper para dependências autorizadas, npm registry via `npm ci`, Java 21 e Node compatível com Angular 22. Nenhum job realizará deploy ou receberá token real por padrão.
