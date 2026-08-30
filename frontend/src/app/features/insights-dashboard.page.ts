import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DashboardApiService, InsightsApiService } from '../core/api/api.services';
import { DashboardCarteira, IncomeSummary, MarketIndicator, PortfolioEvolutionPoint } from '../core/api/api.models';
import { EmptyStateComponent, LoadingComponent } from '../shared/feedback.component';

const money = (value: number | null | undefined) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value ?? 0);

@Component({ standalone: true, imports: [EmptyStateComponent, LoadingComponent, RouterLink], template: `
<section class="page"><header class="page-header"><div><h1>Visão geral</h1><p class="muted">Dados financeiros calculados e normalizados pelo backend.</p></div><a routerLink="/carteiras">Gerenciar carteira</a></header>
@if (loading()) { <app-loading/> } @else if (dashboard(); as data) {
<section class="grid metrics" aria-label="Indicadores de mercado">@for (item of indicators(); track item.codigo) { <article class="panel"><small>{{ item.descricao }}</small><strong>{{ item.valor === null ? 'Indisponível' : item.valor }}</strong><span class="muted">{{ item.disponibilidade === 'STALE' ? 'Dado desatualizado' : item.disponibilidade === 'UNAVAILABLE' ? 'Fonte indisponível' : item.referenciaEm }}</span></article> }</section>
<section class="metrics grid"><article class="metric"><span>Investido</span><strong>{{ money(data.valorInvestido) }}</strong></article><article class="metric"><span>Atual</span><strong>{{ money(data.valorAtual) }}</strong></article><article class="metric"><span>Resultado</span><strong [class.positive]="data.resultado >= 0" [class.negative]="data.resultado < 0">{{ money(data.resultado) }}</strong></article><article class="metric"><span>Rentabilidade</span><strong>{{ data.rentabilidadePercentual }}%</strong></article></section>
<section class="grid"><article class="panel"><h2>Alocação</h2>@if (!data.composicao.length) { <app-empty-state title="Carteira sem posições"/> } @else { <table><thead><tr><th>Ativo</th><th>Classe</th><th>Atual</th></tr></thead><tbody>@for (item of data.composicao; track item.posicaoId) { <tr><td>{{ item.ticker }}</td><td>{{ item.classificacaoAlocacao }}</td><td>{{ money(item.valorAtual) }}</td></tr>}</tbody></table> }</article><article class="panel"><h2>Evolução</h2>@if (!evolution().length) { <app-empty-state title="Sem histórico disponível" detail="Novos pontos serão exibidos quando houver dados armazenados."/> } @else { <div class="chart" aria-label="Evolução da carteira">@for (point of evolution(); track point.referenciaEm) { <div class="bar" [style.height.%]="height(point.valorAtual)"></div> }</div> }</article></section>
<section class="panel"><h2>Proventos</h2>@if (income()?.disponibilidade === 'UNAVAILABLE') { <p class="muted">Dados de proventos indisponíveis para esta carteira.</p> } @else { <p>Recebidos em 12 meses: {{ money(income()?.recebidosUltimosDozeMeses) }}</p><p>Próximos: {{ money(income()?.proximosProventos) }}</p> }</section>
} @else { <app-empty-state title="Carteira não encontrada" detail="Crie ou selecione uma carteira antes de consultar o dashboard."/> }</section>` })
export class InsightsDashboardPage {
  private readonly dashboardApi = inject(DashboardApiService); private readonly insightsApi = inject(InsightsApiService); private readonly route = inject(ActivatedRoute);
  readonly money = money; readonly dashboard = signal<DashboardCarteira | null>(null); readonly indicators = signal<MarketIndicator[]>([]); readonly evolution = signal<PortfolioEvolutionPoint[]>([]); readonly income = signal<IncomeSummary | null>(null); readonly loading = signal(true);
  constructor() { const id = Number(this.route.snapshot.paramMap.get('carteiraId')); if (id > 0) this.load(id); else this.loading.set(false); }
  height(value: number): number { const max = Math.max(...this.evolution().map(p => p.valorAtual), 1); return Math.max(5, value / max * 100); }
  private load(id: number): void { forkJoin({ dashboard: this.dashboardApi.getPortfolio(id), indicators: this.insightsApi.indicators(), evolution: this.insightsApi.evolution(id), income: this.insightsApi.income(id) }).subscribe({ next: data => { this.dashboard.set(data.dashboard); this.indicators.set(data.indicators); this.evolution.set(data.evolution); this.income.set(data.income); this.loading.set(false); }, error: () => this.loading.set(false) }); }
}
