import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { CarteirasApiService, DashboardApiService, InsightsApiService } from '../core/api/api.services';
import { InsightsDashboardPage } from './insights-dashboard.page';

const dashboard = { carteiraId: 7, valorInvestido: 100, valorAtual: 125, resultado: 25, rentabilidadePercentual: 25, ultimaAtualizacao: '2026-08-26T12:00:00Z', quantidadeAtivos: 1, composicao: [{ posicaoId: 1, acaoId: 2, mercado: 'BRASIL' as const, classificacaoAlocacao: 'ACOES_BRASIL' as const, ticker: 'ABCD3', nomeEmpresa: 'Empresa', quantidade: 1, cotacaoAtual: 125, dataHoraCotacao: '2026-08-26T12:00:00Z', valorInvestido: 100, valorAtual: 125, resultado: 25, rentabilidadePercentual: 25 }] };
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
    expect(fixture.nativeElement.textContent).toContain('Sem histórico disponível');
  });
});
