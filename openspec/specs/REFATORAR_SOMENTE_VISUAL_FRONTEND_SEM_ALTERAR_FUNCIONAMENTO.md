# TASK EXECUTÁVEL — REFATORAR SOMENTE O VISUAL DO FRONTEND

## Objetivo

Modernizar somente a aparência do frontend Angular da Carteira de Investimentos, mantendo integralmente o funcionamento restaurado.

O fluxo atual de cadastro está aprovado pelo usuário. Não modificar a forma de adicionar carteiras, posições, ativos ou corretoras. Não reconstruir a arquitetura e não criar páginas substitutas.

O resultado visual deve se aproximar de uma plataforma financeira moderna:

- azul-petróleo;
- verde-esmeralda;
- branco e cinzas claros;
- navegação superior profissional;
- cards financeiros;
- formulários visualmente organizados;
- tabelas modernas;
- boa hierarquia tipográfica;
- responsividade;
- microinterações discretas.

## Regra principal

Esta é uma tarefa exclusivamente visual.

É proibido alterar comportamento, arquitetura, contratos ou fluxo de uso.

Não responder apenas com análise, plano, diagnóstico ou outra spec. Modificar os arquivos existentes, executar as validações e corrigir erros.

## 1. Criar proteção antes de editar

Na raiz do projeto, crie uma cópia apenas do frontend atual:

```powershell
$origem = "frontend"
$backup = "frontend-backup-visual-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Copy-Item $origem $backup -Recurse
Write-Host "Backup criado em $backup"
```

Não apagar esse backup nesta tarefa.

Antes das alterações, execute:

```powershell
cd frontend
npx tsc -p tsconfig.app.json --noEmit
npm test -- --run
npm run build
```

Se algum comando já falhar antes da mudança visual, registre a falha preexistente e não altere lógica para escondê-la.

## 2. Arquivos completamente protegidos

Não modificar, criar substitutos, renomear ou excluir:

```text
frontend/src/app/app.routes.ts
frontend/src/app/core/api/api.services.ts
frontend/src/app/core/api/api.models.ts
frontend/src/app/core/api/api-error.interceptor.ts
frontend/src/app/core/api/silent-http-error.ts
frontend/src/app/core/feedback/notification.service.ts
frontend/proxy.conf.json
frontend/angular.json
frontend/package.json
frontend/package-lock.json
```

Também é proibido modificar qualquer arquivo do backend.

Não alterar:

- endpoints;
- proxy;
- porta 8081;
- CORS;
- requests e responses;
- interfaces e models;
- services;
- interceptors;
- rotas;
- testes funcionais.

## 3. Componentes funcionais protegidos

Manter os componentes existentes. Não criar versões `Premium*`, aliases, wrappers ou reexports.

Não usar:

```text
NgComponentOutlet
ngComponentOutlet
export { ClasseAntiga as ClasseNova }
```

Nos arquivos abaixo, é permitido alterar somente o HTML definido em `template` e, se necessário, adicionar ou ajustar referência a um arquivo SCSS próprio:

```text
frontend/src/app/features/pages.ts
frontend/src/app/features/insights-dashboard.page.ts
frontend/src/app/features/position-table.page.ts
frontend/src/app/shared/top-navigation.component.ts
```

Não alterar nas classes:

- imports funcionais;
- services injetados;
- signals;
- computed;
- formulários;
- validators;
- construtores;
- métodos;
- parâmetros;
- tipos;
- chamadas HTTP;
- tratamento de erro;
- navegação;
- lógica de seleção;
- lógica de CRUD.

Não alterar nomes de:

- `formControlName`;
- propriedades usadas em bindings;
- eventos `(click)`, `(ngSubmit)` e `(ngModelChange)`;
- valores de `name`;
- rotas usadas em `routerLink`;
- condições `@if`;
- iterações `@for`;
- métodos chamados pelo template.

É permitido reorganizar visualmente elementos existentes, desde que todos os bindings, campos, botões e ações permaneçam presentes e funcionais.

## 4. Arquivos visuais permitidos

É permitido editar ou criar somente:

```text
frontend/src/styles.scss
frontend/src/app/app.html
frontend/src/app/app.scss
frontend/src/app/shared/top-navigation.component.scss
frontend/src/app/features/pages.scss
frontend/src/app/features/insights-dashboard.page.scss
frontend/src/app/features/position-table.page.scss
```

Se os componentes usam templates inline e ainda não referenciam esses arquivos SCSS, pode-se adicionar apenas `styleUrl` ou `styleUrls` ao metadata de `@Component`. Não alterar a classe.

Não apagar um SCSS enquanto ele estiver referenciado.

## 5. Identidade visual

Definir em `styles.scss` tokens semelhantes a:

```scss
:root {
  --brand-950: #062a30;
  --brand-900: #082f35;
  --brand-800: #0d4148;
  --accent-600: #059669;
  --accent-500: #10b981;
  --accent-100: #d1fae5;
  --ink-950: #102a2e;
  --ink-700: #405b5f;
  --ink-500: #71868a;
  --border: #dfe8e5;
  --surface: #ffffff;
  --surface-soft: #f4f8f6;
  --background: #eef4f1;
  --positive: #05875f;
  --negative: #dc4c55;
  --warning: #b7791f;
  --shadow-sm: 0 8px 24px rgba(8, 47, 53, 0.08);
  --shadow-md: 0 18px 45px rgba(8, 47, 53, 0.12);
}
```

Remover visualmente a identidade roxa antiga:

```text
--purple
#6c5ce7
#413b85
#a29bfe
```

Usar bordas suaves, sombras discretas, cantos arredondados e espaçamento consistente.

## 6. Navegação superior

Manter o componente e os links atuais. Não mudar destinos.

Aplicar no SCSS encapsulado próprio:

- cabeçalho sticky;
- fundo `#082f35`;
- marca branca com `+` verde;
- links claros;
- link ativo com indicador verde;
- área interna alinhada à largura do conteúdo;
- avatar circular;
- botão mobile;
- menu responsivo abaixo de 720 px;
- nenhuma sobreposição.

As regras `.topbar`, `.brand`, `.main-nav`, `.topbar-actions`, `.notification`, `.avatar` e `.menu-button` devem ficar em `top-navigation.component.scss`, não depender de `app.scss`.

Corrigir somente textos visuais corrompidos em UTF-8, como:

```text
VisÃ£o → Visão
NavegaÃ§Ã£o → Navegação
â˜° → ☰
```

## 7. Dashboard

Preservar exatamente a lógica atual do dashboard.

Alterar somente a apresentação para incluir:

- cabeçalho “Visão geral”;
- seletor de carteira no mesmo fluxo atual;
- hero azul-petróleo para patrimônio;
- quatro cards financeiros;
- painéis claros para evolução e distribuição;
- tabela moderna de posições;
- indicadores e proventos organizados;
- skeletons e estados vazios estilizados;
- verde para resultados positivos;
- vermelho moderado para negativos;
- nenhum dado inventado.

Não alterar chamadas para DashboardApiService, InsightsApiService ou CarteirasApiService.

## 8. Carteiras e posições

Manter exatamente a maneira atual de adicionar carteira e posição.

Não transformar os formulários em drawers ou modais se isso mudar o fluxo aprovado.

Apenas melhorar visualmente:

- formulário de carteira em card bem organizado;
- lista de carteiras com seleção visual clara;
- detalhes da carteira em painel principal;
- campos de posição no mesmo local e com os mesmos controles;
- botões criar, salvar, limpar, editar e excluir;
- tabela de posições;
- estados vazios;
- responsividade.

Não remover campos, não trocar selects por IDs ou IDs por outra lógica, não alterar payloads e não mudar os métodos de salvar.

## 9. Ativos

Manter exatamente o cadastro atual de ativos e seu formulário.

Melhorar somente:

- cabeçalho;
- busca;
- card do formulário;
- labels e textos de ajuda;
- tabela ou apresentação atual dos registros;
- badges de mercado;
- cotação;
- botão Atualizar;
- acesso ao histórico;
- estados vazio, loading e erro.

Não alterar `save`, `lookup`, `refresh`, ticker em maiúsculas, payload `{ ticker, mercado }` ou AcoesApiService.

## 10. Histórico

Manter a forma atual de consulta e a lógica existente.

Melhorar apenas:

- título;
- formulário atual, se existente;
- gráfico;
- tabela;
- datas, valores e fonte;
- estados vazio e carregando.

Não alterar ActivatedRoute, AcoesApiService ou método `load`.

## 11. Corretoras

Manter exatamente o formulário e a maneira atual de cadastrar corretoras.

Melhorar somente:

- cabeçalho;
- busca por CNPJ;
- card do formulário;
- organização de CNPJ, CEP, número e complemento;
- labels e ajuda;
- tabela/lista atual;
- status Validada/Pendente;
- loading, vazio e erro.

Não alterar:

- `save`;
- `lookup`;
- remoção das máscaras;
- payload `{ cnpj, cep, numero, complemento }`;
- CorretorasApiService;
- comportamento do backend ou provedores externos.

## 12. Componentes globais

Modernizar visualmente, sem alterar APIs públicas:

- loading;
- empty state;
- diálogo de confirmação;
- toasts;
- inputs;
- selects;
- botões;
- tabelas;
- mensagens de aviso e erro.

Não alterar Inputs, Outputs ou nomes de eventos dos componentes compartilhados.

## 13. Responsividade e acessibilidade

Garantir:

- desktop, tablet e celular;
- tabelas com scroll horizontal quando necessário;
- formulários em uma coluna no celular;
- navegação mobile funcional;
- foco visível;
- contraste adequado;
- botões com área clicável suficiente;
- labels e aria-labels existentes preservados.

## 14. Validação funcional obrigatória

Depois das alterações, executar na pasta `frontend`:

```powershell
npx tsc -p tsconfig.app.json --noEmit
npm test -- --run
npm run build
```

Corrigir erros visuais ou de template sem modificar lógica funcional.

Testar manualmente, com backend ativo:

1. Criar carteira.
2. Editar carteira.
3. Excluir carteira vazia.
4. Cadastrar ativo.
5. Buscar ativo.
6. Atualizar cotação.
7. Cadastrar corretora.
8. Buscar corretora.
9. Adicionar posição.
10. Editar posição.
11. Excluir posição.
12. Abrir dashboard.
13. Abrir histórico.

Se alguma dessas ações funcionava antes e parar depois, a tarefa não está concluída. Corrigir restaurando o binding ou markup necessário, sem alterar services, models ou métodos.

## 15. Inspeção final

Executar:

```powershell
Get-ChildItem src -Recurse -File -Include *.ts,*.html,*.scss |
  Select-String -Pattern "NgComponentOutlet|ngComponentOutlet|as Premium|#6c5ce7|--purple|VisÃ|AÃ§|NÃ£o"
```

Não devem existir aliases/wrappers novos, roxo antigo visível ou textos corrompidos nos arquivos alterados.

Confirmar que `app.routes.ts`, `api.services.ts` e `api.models.ts` não foram modificados.

## 16. Critérios de aceite

A tarefa só estará concluída quando:

- o fluxo de uso continuar igual ao restaurado;
- todos os cadastros continuarem funcionando;
- nenhuma rota tiver sido alterada;
- nenhum service, model, endpoint ou payload tiver sido alterado;
- nenhuma página substituta tiver sido criada;
- apenas templates e estilos tiverem sido modernizados;
- navegação estiver corretamente estilizada;
- identidade visual for azul-petróleo e verde;
- TypeScript, testes e build passarem;
- testes manuais forem realmente executados.

## 17. Resposta final

Informar:

- arquivos visuais alterados;
- confirmação de que rotas, services, models e backend não foram modificados;
- resultado de TypeScript;
- quantidade de testes aprovados;
- resultado do build;
- cadastros testados manualmente;
- qualquer limitação real.

Não afirmar que um teste manual foi feito se não houve ambiente disponível para executá-lo.
