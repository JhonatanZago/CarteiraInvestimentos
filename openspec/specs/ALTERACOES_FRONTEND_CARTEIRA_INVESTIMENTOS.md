# Alterações do Frontend — Carteira de Investimentos

Este documento descreve as mudanças realizadas no frontend Angular para modernizar a interface, corrigir o dashboard vazio e melhorar a integração com o backend Spring Boot executado na porta `8081`.

## 1. Objetivo

O frontend anterior apresentava três problemas principais:

1. interface muito simples, semelhante a um CRUD administrativo;
2. dashboard sem informações quando uma integração opcional falhava;
3. ausência de gráficos, feedback visual, seleção de carteira e estados de carregamento adequados.

A nova versão mantém os dados calculados pelo backend e não cria valores financeiros fictícios.

## 2. Arquivos alterados

| Arquivo | Alteração |
|---|---|
| `frontend/src/app/features/insights-dashboard.page.ts` | Dashboard refeito e carregamento tolerante a falhas |
| `frontend/src/app/app.html` | Nova estrutura do menu lateral |
| `frontend/src/app/app.scss` | Estilos do layout principal e navegação |
| `frontend/src/styles.scss` | Design system e estilos completos do dashboard |
| `frontend/src/app/features/insights-dashboard.page.spec.ts` | Teste para falhas parciais dos endpoints |

## 3. Correção principal: dashboard vazio

### Problema encontrado

O dashboard utilizava um único `forkJoin`:

```typescript
forkJoin({
  dashboard: this.dashboardApi.getPortfolio(id),
  indicators: this.insightsApi.indicators(),
  evolution: this.insightsApi.evolution(id),
  income: this.insightsApi.income(id)
})
```

O `forkJoin` cancela o resultado completo se qualquer Observable produzir erro. Dessa forma, se indicadores ou proventos estivessem indisponíveis, o Angular também deixava de exibir patrimônio, rentabilidade e posições, mesmo quando o endpoint principal havia respondido corretamente.

### Solução implementada

Cada chamada opcional passou a tratar seu próprio erro:

```typescript
forkJoin({
  dashboard: this.dashboardApi.getPortfolio(id).pipe(
    catchError(error => of({ __error: error } as never))
  ),
  indicators: this.insightsApi.indicators().pipe(
    catchError(() => of([]))
  ),
  evolution: this.insightsApi.evolution(id).pipe(
    catchError(() => of([]))
  ),
  income: this.insightsApi.income(id).pipe(
    catchError(() => of(null))
  )
})
```

Com isso:

- o resumo da carteira continua aparecendo;
- indicadores indisponíveis mostram uma mensagem específica;
- proventos indisponíveis não quebram a página;
- ausência de histórico exibe um estado vazio;
- erros do endpoint principal exibem a opção **Tentar novamente**.

## 4. Novo dashboard

O arquivo `insights-dashboard.page.ts` foi reorganizado para incluir:

- seletor de carteira;
- botão para atualizar os dados;
- patrimônio atual em destaque;
- resultado acumulado;
- rentabilidade percentual;
- total investido;
- quantidade de ativos;
- resumo de proventos;
- evolução patrimonial;
- gráfico de distribuição por ativo;
- tabela consolidada das posições;
- indicadores de mercado;
- carregamento com skeleton;
- mensagens de erro e indisponibilidade;
- estado específico para carteira sem posições.

### Novos imports

```typescript
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
```

Também foram adicionados `CarteirasApiService` e a interface `Carteira`, pois o dashboard agora lista as carteiras disponíveis.

### Formatação monetária

```typescript
const money = (value: number | null | undefined) =>
  new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value ?? 0);
```

### Formatação percentual

```typescript
const percent = (value: number | null | undefined) =>
  new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value ?? 0) + '%';
```

### Seleção da carteira

```typescript
readonly selectedId = signal(
  Number(this.route.snapshot.paramMap.get('carteiraId')) || 1
);

changePortfolio(id: number): void {
  if (id && id !== this.selectedId()) {
    this.selectedId.set(id);
    this.router.navigate(['/dashboard', id]);
    this.load(id);
  }
}
```

### Listagem das carteiras

```typescript
this.portfoliosApi.list(0, 100)
  .pipe(
    catchError(() => of({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0
    }))
  )
  .subscribe(page => this.portfolios.set(page.content));
```

## 5. Gráfico de evolução

O gráfico foi implementado com SVG, sem instalar biblioteca externa.

Os pontos são calculados proporcionalmente:

```typescript
private buildChartPoints(): string {
  const points = this.evolution();

  if (!points.length) {
    return '';
  }

  const values = points.map(point => point.valorAtual);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = Math.max(max - min, 1);

  return points.map((point, index) => {
    const x = points.length === 1
      ? 300
      : index / (points.length - 1) * 600;

    const y = 190 -
      (point.valorAtual - min) / range * 150;

    return `${x},${y}`;
  }).join(' ');
}
```

No HTML:

```html
<svg viewBox="0 0 600 220" preserveAspectRatio="none">
  <polygon
    [attr.points]="areaPoints()"
    fill="url(#area)"
  />

  <polyline
    [attr.points]="chartPoints()"
    fill="none"
    stroke="#6c5ce7"
    stroke-width="4"
  />
</svg>
```

## 6. Gráfico de distribuição

A distribuição utiliza `conic-gradient`, evitando dependências adicionais.

```typescript
readonly donutBackground = computed(() => {
  const items = this.topAllocation();
  const total = items.reduce(
    (sum, item) => sum + item.valorAtual,
    0
  );

  if (!total) {
    return '#edf0f7';
  }

  let start = 0;

  const stops = items.map((item, index) => {
    const end = start + item.valorAtual / total * 100;
    const stop =
      `${this.colors[index % this.colors.length]} ${start}% ${end}%`;

    start = end;
    return stop;
  });

  return `conic-gradient(${stops.join(',')})`;
});
```

## 7. Novo menu lateral

O `app.html` passou a possuir:

- logotipo Carteira+;
- menu com destaque da rota ativa;
- ícones de navegação;
- área de ajuda;
- identificação do usuário;
- menu recolhível em dispositivos móveis.

Estrutura resumida:

```html
<div class="app-shell">
  <header class="topbar">
    <!-- Cabeçalho mobile -->
  </header>

  <aside class="sidebar" [class.open]="menuOpen()">
    <a class="brand" routerLink="/carteiras">Carteira+</a>

    <nav>
      <a routerLink="/dashboard/1" routerLinkActive="active">
        Visão geral
      </a>
      <a routerLink="/carteiras" routerLinkActive="active">
        Minhas carteiras
      </a>
      <a routerLink="/acoes" routerLinkActive="active">
        Ações
      </a>
      <a routerLink="/corretoras" routerLinkActive="active">
        Corretoras
      </a>
    </nav>
  </aside>

  <main class="content">
    <router-outlet />
  </main>
</div>
```

## 8. Novo padrão visual

As principais cores adicionadas em `styles.scss` foram:

```scss
:root {
  --purple: #6c5ce7;
  --ink: #202037;
  --muted: #7b7c91;
  --border: #e8e9f1;
  --surface: #ffffff;
  --background: #f6f7fb;
}
```

O layout passou a utilizar:

- fundo cinza muito claro;
- menu lateral azul-escuro/roxo;
- roxo como cor principal;
- verde para resultados positivos;
- vermelho para resultados negativos;
- cartões brancos com bordas suaves;
- cantos arredondados;
- sombras discretas;
- tabela com rolagem horizontal em telas menores.

## 9. Estados da interface

### Carregamento

Durante a primeira requisição, são mostrados cartões skeleton:

```html
@if (loading() && !dashboard()) {
  <section class="skeleton-grid">
    @for (item of [1, 2, 3, 4]; track item) {
      <div class="skeleton-card">
        <i></i><i></i><i></i>
      </div>
    }
  </section>
}
```

### Carteira vazia

A interface informa que é necessário cadastrar uma ação e adicionar uma posição, disponibilizando um link para `/carteiras`.

### Falha principal

Quando o dashboard da carteira não responde, é exibido:

```text
Não foi possível carregar esta carteira.
Verifique se a carteira existe e se o backend está respondendo na porta 8081.
```

### Falha parcial

Se indicadores ou proventos falharem, o patrimônio continua sendo exibido e aparece um aviso amarelo.

## 10. Proxy para o backend

Confirme o arquivo `frontend/proxy.conf.json`:

```json
{
  "/api/v1/**": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}
```

No `frontend/angular.json`, a configuração `serve` deve conter:

```json
"serve": {
  "builder": "@angular/build:dev-server",
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```

No `environment.ts`:

```typescript
export const environment = {
  production: false,
  apiBaseUrl: '/api/v1'
};
```

## 11. Endpoints utilizados

| Informação | Endpoint |
|---|---|
| Carteiras disponíveis | `GET /api/v1/carteiras` |
| Resumo financeiro | `GET /api/v1/dashboard/carteiras/{id}` |
| Evolução patrimonial | `GET /api/v1/carteiras/{id}/evolucao` |
| Proventos | `GET /api/v1/carteiras/{id}/proventos` |
| Indicadores de mercado | `GET /api/v1/mercado/indicadores` |
| Posições da carteira | `GET /api/v1/carteiras/{id}/posicoes` |

## 12. Teste adicionado

Foi criado `insights-dashboard.page.spec.ts` para garantir que falhas opcionais não escondam os valores principais.

Cenário testado:

1. o endpoint do dashboard retorna patrimônio normalmente;
2. os endpoints de indicadores e proventos retornam erro;
3. o patrimônio permanece visível;
4. o aviso de indisponibilidade é apresentado.

## 13. Ordem recomendada para implementação manual

1. Faça uma cópia de segurança do frontend atual.
2. Confirme o proxy para a porta `8081`.
3. Substitua `app.html`.
4. Substitua `app.scss`.
5. Substitua `styles.scss`.
6. Substitua `insights-dashboard.page.ts`.
7. Adicione `insights-dashboard.page.spec.ts`.
8. Instale novamente as dependências.
9. Compile o frontend.
10. Execute os testes.
11. Inicie backend e frontend.
12. Cadastre carteira, ação, corretora e posição para visualizar dados reais.

## 14. Comandos para executar

No terminal do backend:

```powershell
.\gradlew.bat bootRun
```

O console precisa informar:

```text
Tomcat started on port 8081
```

No terminal do frontend:

```powershell
cd frontend
npm install
npm start
```

Acesse:

```text
http://localhost:4200/dashboard/1
```

## 15. Comandos de validação

```powershell
cd frontend
npm run build
npm test -- --run
```

Resultado esperado:

- build sem erros;
- testes aprovados;
- nenhuma mensagem de `ERR_CONNECTION_REFUSED`;
- resumo da carteira visível mesmo sem indicadores externos;
- layout adaptado em desktop e celular.

## 16. Observação sobre dados

O frontend não deve criar números fictícios. Para visualizar valores reais, o banco precisa conter:

1. uma corretora válida;
2. uma ação cadastrada;
3. uma carteira;
4. uma posição associando carteira, ação e corretora;
5. uma cotação armazenada para a ação.

Os indicadores de mercado e os proventos podem permanecer como **Indisponível** enquanto nenhuma fonte externa estiver configurada. Essa indisponibilidade não deve impedir a exibição do patrimônio, das posições e da rentabilidade já calculados pelo backend.
