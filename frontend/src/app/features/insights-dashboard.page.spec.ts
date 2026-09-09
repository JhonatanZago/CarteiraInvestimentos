import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { CarteirasApiService, DashboardApiService, InsightsApiService } from '../core/api/api.services';
import { InsightsDashboardPage } from './insights-dashboard.page';

const dashboard = { carteiraId: 7, valorInvestido: 100, valorAtual: 125, resultado: 25, rentabilidadePercentual: 25, ultimaAtualizacao: '2026-08-26T12:00:00Z', quantidadeAtivos: 1, composicao: [{ posicaoId: 1, acaoId: 2, mercado: 'BRASIL' as const, classificacaoAlocacao: 'ACOES_BRASIL' as const, ticker: 'ABCD3', nomeEmpresa: 'Empresa', quantidade: 1, precoMedio: 100, cotacaoAtual: 125, dataHoraCotacao: '2026-08-26T12:00:00Z', valorInvestido: 100, valorAtual: 125, resultado: 25, rentabilidadePercentual: 25, moeda: 'BRL' }] };
const portfolios = { list: () => of({ content: [{ id: 7, nome: 'Principal', dataCriacao: '2026-08-01' }] }) };

describe('InsightsDashboardPage', () => {
  const configure = async (dashboardApi: object, insightsApi: object) => {
    await TestBed.configureTestingModule({
      imports: [InsightsDashboardPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '7' } } } },
        { provide: CarteirasApiService, useValue: portfolios },
        { provide: DashboardApiService, useValue: dashboardApi },
        { provide: InsightsApiService, useValue: insightsApi },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(InsightsDashboardPage);
    fixture.detectChanges();
    return fixture;
  };

  it('keeps the financial summary visible when indicators and income fail', async () => {
    const fixture = await configure(
      { getPortfolio: () => of(dashboard) },
      { indicators: () => throwError(() => new Error('unavailable')), evolution: () => of([]), income: () => throwError(() => new Error('unavailable')) },
    );
    expect(fixture.nativeElement.textContent).toContain('R$ 125,00');
    expect(fixture.nativeElement.textContent).toContain('Alguns dados complementares estão indisponíveis');
    expect(fixture.nativeElement.textContent).toContain('Posições consolidadas');
  });

  it('offers retry when the primary dashboard request fails', async () => {
    const getPortfolio = vi.fn(() => throwError(() => new Error('backend unavailable')));
    const fixture = await configure(
      { getPortfolio },
      { indicators: () => of([]), evolution: () => of([]), income: () => of(null) },
    );
    const retry = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')).find(button => button.textContent?.includes('Tentar novamente'));
    expect(retry).toBeTruthy();
    if (!retry) throw new Error('Retry button was not rendered');
    retry.click();
    expect(getPortfolio).toHaveBeenCalledTimes(2);
  });

  it('renders the empty portfolio and history states from returned data', async () => {
    const fixture = await configure(
      { getPortfolio: () => of({ ...dashboard, quantidadeAtivos: 0, composicao: [] }) },
      { indicators: () => of([]), evolution: () => of([]), income: () => of(null) },
    );
    expect(fixture.nativeElement.textContent).toContain('Carteira sem posições');
    expect(fixture.nativeElement.textContent).toContain('Sem histórico patrimonial');
  });
  it('renders empty and single-point histories without invalid coordinates', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([]), income: () => of(null) });
    expect(fixture.nativeElement.textContent).toContain('A evolução aparecerá após as primeiras atualizações da carteira.');
    fixture.componentInstance.evolution.set([{ referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 110, disponibilidade: 'AVAILABLE' }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Histórico em formação');
    expect(fixture.nativeElement.querySelector('.evolution-chart')).toBeNull();
    expect(fixture.componentInstance.chartPoints().every(point => Number.isFinite(point.x) && Number.isFinite(point.y))).toBe(true);
  });

  it('sorts multiple and equal historical values chronologically', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([]), income: () => of(null) });
    fixture.componentInstance.evolution.set([
      { referenciaEm: '2026-03-01T00:00:00Z', valorInvestido: 100, valorAtual: 110, disponibilidade: 'AVAILABLE' },
      { referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 110, disponibilidade: 'AVAILABLE' },
      { referenciaEm: '2026-02-01T00:00:00Z', valorInvestido: 100, valorAtual: 110, disponibilidade: 'AVAILABLE' },
    ]);
    expect(fixture.componentInstance.sortedEvolution().map(point => point.date)).toEqual(['2026-01-01T00:00:00Z', '2026-02-01T00:00:00Z', '2026-03-01T00:00:00Z']);
    expect(fixture.componentInstance.linePath()).not.toContain('NaN');
  });

  it('renders one asset as 100%, preserves original amounts, and omits zero totals', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([]), income: () => of(null) });
    expect(fixture.componentInstance.allocation()[0].percentage).toBe(100);
    const composicao = Array.from({ length: 7 }, (_, index) => ({ ...dashboard.composicao[0], posicaoId: index + 1, acaoId: index + 1, ticker: `ATV${index}`, valorAtual: index === 6 ? 0 : (index + 1) * 10, valorAtualConvertidoBase: index === 6 ? 0 : (index + 1) * 10 }));
    fixture.componentInstance.dashboard.set({ ...dashboard, composicao, quantidadeAtivos: 7 });
    expect(fixture.componentInstance.allocation()).toHaveLength(6);
    expect(fixture.componentInstance.allocation().reduce((sum, item) => sum + item.percentage, 0)).toBeCloseTo(100, 8);
    fixture.componentInstance.dashboard.set({ ...dashboard, composicao: [{ ...dashboard.composicao[0], valorAtual: 0, valorAtualConvertidoBase: 0 }] });
    expect(fixture.componentInstance.allocation()).toEqual([]);
  });

  it('uses theme-compatible chart classes in light and dark themes', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([{ referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 110, disponibilidade: 'AVAILABLE' }, { referenciaEm: '2026-02-01T00:00:00Z', valorInvestido: 100, valorAtual: 115, disponibilidade: 'AVAILABLE' }]), income: () => of(null) });
    document.documentElement.dataset['theme'] = 'dark'; fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.chart-grid-line')).not.toBeNull();
    document.documentElement.dataset['theme'] = 'light';
  });

  it('uses ordered real points in the evolution chart and ignores invalid records', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([]), income: () => of(null) });
    fixture.componentInstance.evolution.set([
      { referenciaEm: '2026-02-01T00:00:00Z', valorInvestido: 100, valorAtual: 120, disponibilidade: 'AVAILABLE' },
      { referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 100, disponibilidade: 'AVAILABLE' },
      { referenciaEm: null, valorInvestido: 0, valorAtual: Number.NaN, disponibilidade: 'UNAVAILABLE' },
    ]);
    fixture.detectChanges();
    expect(fixture.componentInstance.chartPoints()).toHaveLength(2);
    expect(fixture.componentInstance.linePath()).not.toContain('NaN');
    expect(fixture.nativeElement.querySelector('.hero-sparkline')).toBeNull();
    expect(fixture.nativeElement.querySelector('.evolution-chart .chart-line')).not.toBeNull();
  });

  it('shows compact formation and empty states only in the evolution panel', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([]), income: () => of(null) });
    expect(fixture.nativeElement.textContent).toContain('Sem histórico patrimonial');
    fixture.componentInstance.evolution.set([{ referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 100, disponibilidade: 'AVAILABLE' }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Histórico em formação');
    expect(fixture.nativeElement.querySelector('.hero-sparkline')).toBeNull();
  });

  it('renders mini-chart empty, single-point and multi-point states without fabricating history', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([]), income: () => of(null) });
    expect(fixture.nativeElement.querySelector('.hero-chart-state')?.textContent).toContain('Sem histórico patrimonial');
    fixture.componentInstance.evolution.set([{ referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 100, disponibilidade: 'AVAILABLE' }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.hero-chart-state')?.textContent).toContain('Histórico em formação');
    fixture.componentInstance.evolution.set([
      { referenciaEm: '2026-02-01T00:00:00Z', valorInvestido: 100, valorAtual: 120, disponibilidade: 'AVAILABLE' },
      { referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 100, disponibilidade: 'AVAILABLE' },
    ]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.hero-chart .hero-line')).not.toBeNull();
    expect(fixture.componentInstance.heroLinePath()).not.toContain('NaN');
  });

  it('filters only existing hero points and indicates growth, loss and stability', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([]), income: () => of(null) });
    fixture.componentInstance.evolution.set([
      { referenciaEm: '2025-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 100, disponibilidade: 'AVAILABLE' },
      { referenciaEm: '2026-08-20T00:00:00Z', valorInvestido: 100, valorAtual: 80, disponibilidade: 'AVAILABLE' },
      { referenciaEm: '2026-09-01T00:00:00Z', valorInvestido: 100, valorAtual: 120, disponibilidade: 'AVAILABLE' },
    ]);
    fixture.componentInstance.heroPeriod.set('1M');
    expect(fixture.componentInstance.heroFilteredEvolution()).toHaveLength(2);
    expect(fixture.componentInstance.heroTrend().tone).toBe('positive');
    fixture.componentInstance.heroPeriod.set('TUDO');
    fixture.componentInstance.evolution.set([{ referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 120, disponibilidade: 'AVAILABLE' }, { referenciaEm: '2026-02-01T00:00:00Z', valorInvestido: 120, valorAtual: 100, disponibilidade: 'AVAILABLE' }]);
    expect(fixture.componentInstance.heroTrend().tone).toBe('negative');
    fixture.componentInstance.evolution.set([{ referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 100, disponibilidade: 'AVAILABLE' }, { referenciaEm: '2026-02-01T00:00:00Z', valorInvestido: 100, valorAtual: 100, disponibilidade: 'AVAILABLE' }]);
    expect(fixture.componentInstance.heroTrend().tone).toBe('neutral');
  });

  it('shows a tooltip with the variation relative to the previous selected point', async () => {
    const fixture = await configure({ getPortfolio: () => of(dashboard) }, { indicators: () => of([]), evolution: () => of([]), income: () => of(null) });
    fixture.componentInstance.evolution.set([{ referenciaEm: '2026-01-01T00:00:00Z', valorInvestido: 100, valorAtual: 100, disponibilidade: 'AVAILABLE' }, { referenciaEm: '2026-02-01T00:00:00Z', valorInvestido: 100, valorAtual: 110, disponibilidade: 'AVAILABLE' }]);
    fixture.componentInstance.hoveredHeroPoint.set(fixture.componentInstance.heroChartPoints()[1]);
    fixture.detectChanges();
    expect(fixture.componentInstance.heroTooltip()?.variation).toBe(10);
    expect(fixture.nativeElement.querySelector('.hero-chart-tooltip')?.textContent).toContain('R$');
  });

  it('renders the approved risk and performance panels without legacy market blocks', async () => {
    const fixture = await configure(
      { getPortfolio: () => of(dashboard) },
      { indicators: () => of([{ codigo: 'IBOV', descricao: 'Ibovespa', valor: 142350, variacaoPercentual: 1.2, referenciaEm: '2026-09-04T12:00:00Z', disponibilidade: 'AVAILABLE' }, { codigo: 'CDI', descricao: 'CDI', valor: null, variacaoPercentual: null, referenciaEm: null, disponibilidade: 'UNAVAILABLE' }]), evolution: () => of([]), income: () => of(null) },
    );
    expect(fixture.nativeElement.textContent).toContain('Análise de risco');
    expect(fixture.nativeElement.textContent).toContain('Desempenho dos ativos');
    expect(fixture.nativeElement.querySelector('.investor-central')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Indicadores de mercado');
    expect(fixture.nativeElement.textContent).not.toContain('Proventos');
  });

  it('does not render the removed income panel', async () => {
    const fixture = await configure(
      { getPortfolio: () => of(dashboard) },
      { indicators: () => of([]), evolution: () => of([]), income: () => of({ recebidosUltimosDozeMeses: 0, proximosProventos: 12.5, referenciaEm: '2026-09-04T12:00:00Z', disponibilidade: 'AVAILABLE' }) },
    );
    expect(fixture.nativeElement.textContent).not.toContain('Recebido confirmado');
    expect(fixture.nativeElement.textContent).not.toContain('Estimado em 12 meses');
  });
});
