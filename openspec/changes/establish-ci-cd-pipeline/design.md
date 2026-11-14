# Design

## Context

O repositório já possui comandos locais para Gradle, SpotBugs/FindSecBugs, Angular, npm e OpenSpec, mas não possui `.github/workflows/`. O wrapper fixa Gradle 9.5.1, o backend usa Java 21 e o frontend exige instalação determinística pelo lockfile. O desenho deve preservar a pendência do Módulo 11 e não pode introduzir deploy ou credenciais reais.

## Goals / Non-Goals

**Goals:**

- Criar uma unidade de CI previsível para Pull Requests e para a branch protegida.
- Executar backend, frontend, análise estática e OpenSpec de forma independente quando possível.
- Reter evidências de testes e análise mesmo em falhas.
- Aplicar permissões mínimas, limites de tempo, cancelamento de duplicatas e caches gerenciados pelo runner.

**Non-Goals:**

- Deploy, publicação de artefatos de produção ou alteração do Security Gate.
- Alteração de autenticação, regras financeiras, migrations, integração BRAPI ou código do Módulo 11.
- Aprovação automática de alertas ZAP ou de riscos residuais.

## Decisions

### Workflow único com jobs independentes

Será usado um workflow de CI com jobs `backend`, `frontend` e `openspec`. Esses jobs podem iniciar em paralelo porque não dependem dos artefatos de compilação uns dos outros; a proteção de branch exigirá todos os checks. Um job posterior de agregação poderá apenas consolidar status, sem executar deploy. Um workflow único facilita auditoria e evita divergência entre PR e branch principal.

Alternativa considerada: workflows separados por tecnologia. Foi rejeitada nesta etapa por duplicar triggers, permissões e política de artefatos.

### Backend com wrapper e Java 21

O job de backend usará Java 21 Temurin, o Gradle Wrapper do repositório e Maven Central conforme a configuração existente. A sequência será `clean test`, `build` e `spotbugsMain --rerun-tasks`, preservando o comportamento fail-closed já configurado. O cache será o cache suportado pela action do Gradle; não serão usados `.gradle-user-home-*` locais nem cópias de JARs.

Alternativa considerada: instalar Gradle globalmente. Foi rejeitada porque contorna a versão fixada pelo wrapper.

### Frontend com Node/npm fixados pelo projeto

O job usará uma versão de Node compatível com Angular 22 e o `packageManager` declarado, validando a escolha contra a matriz de suporte no momento da implementação. Executará `npm ci`, `npm test -- --watch=false` e `npm run build`. O cache será o cache de npm suportado pelo runner e terá a chave derivada do lockfile.

Alternativa considerada: `npm install`. Foi rejeitada por permitir resolver uma árvore diferente da lockfile.

### Segurança de workflow

O workflow declarará `permissions: contents: read`, não terá jobs de deploy e não receberá `BRAPI_TOKEN`, `AUTH_JWT_SECRET` real ou tokens de provedor. O perfil de testes deverá continuar suficiente para testes isolados; qualquer teste externo será explicitamente separado e não fará parte do gate padrão. Saídas de comandos e artefatos serão revisados para impedir impressão de segredos.

### Evidências e política de ignorados

Cada job fará upload condicional de resultados de teste, relatórios HTML/XML do SpotBugs e saídas frontend disponíveis, inclusive após falhas. Testes falhos bloqueiam o job. Testes ignorados não serão convertidos em sucesso silencioso: a contagem será visível nos relatórios e o caso conhecido de smoke PostgreSQL continuará condicionado à configuração de smoke, sem credenciais reais.

### Triggers, concorrência e limites

O workflow será acionado em Pull Requests e em pushes para a branch principal real do repositório. Será aplicado um grupo de concorrência por workflow/ref com cancelamento de execução anterior e timeout por job. O nome da branch principal será confirmado durante a implementação, sem presumir `main` se o repositório indicar outro nome.

### OpenSpec strict

Um job dedicado executará a validação strict da change em revisão usando o CLI OpenSpec disponível e publicará a saída. A implementação deverá definir como o CLI é disponibilizado de forma reproduzível; não será permitido depender de uma instalação pessoal invisível no runner.

## Risks / Trade-offs

- [Risco] A versão de Node compatível com Angular pode mudar com a atualização do projeto → fixar a versão no workflow após confirmar `package.json` e a matriz oficial do Angular, atualizando-a junto com dependências.
- [Risco] Dependências offline não estarão disponíveis no runner → usar resolução HTTPS apenas para repositórios autorizados e deixar falhas de resolução visíveis; não fabricar repositório nem alterar metadados.
- [Risco] Relatórios podem conter dados de teste inesperados → revisar dados e caminhos publicados, excluir arquivos de configuração e nunca publicar `.env`, caches ou variáveis de ambiente.
- [Risco] O smoke PostgreSQL ser ignorado pode reduzir cobertura → manter o skip explícito e documentado, sem mascarar falhas de testes executados; habilitar smoke somente em job/configuração futura aprovada.
- [Risco] A validação OpenSpec depender de CLI não pré-instalado → escolher uma distribuição versionada e verificável na tarefa de implementação, com falha explícita se a versão não puder ser obtida.

## Migration Plan

1. Implementar o workflow conforme esta change e validar a sintaxe localmente.
2. Executar os jobs em uma Pull Request de teste, revisar logs e artefatos sem segredos.
3. Configurar os nomes dos checks como requisitos da branch protegida, sem habilitar deploy.
4. Em caso de regressão, remover ou ajustar apenas o workflow; nenhum dado de aplicação ou migration precisa de rollback.

## Open Questions

- Qual é o nome exato da branch protegida principal no momento da implementação?
- Qual mecanismo versionado e autorizado será usado para disponibilizar o CLI OpenSpec no runner?
