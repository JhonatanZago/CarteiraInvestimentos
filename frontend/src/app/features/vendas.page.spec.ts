import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it } from 'vitest';
import { CarteirasApiService } from '../core/api/api.services';
import { VendasPage } from './vendas.page';

describe('VendasPage visual history state', () => {
  it('keeps chart, indicators and history visible without an open position', async () => {
    const sales = [
      { id: 1, carteiraId: 1, posicaoId: 4, ticker: 'AAPL', nomeEmpresa: 'Apple Inc.', quantidade: 100, precoMedio: 100, precoVenda: 333, taxas: 20, valorBruto: 33300, custoPosicao: 10000, resultadoRealizado: 23280, rentabilidadePercentual: 232.8, moeda: 'USD', dataVenda: '2026-09-10' },
      { id: 2, carteiraId: 1, posicaoId: 7, ticker: 'BB', nomeEmpresa: 'BlackBerry Limited', quantidade: 20, precoMedio: 60, precoVenda: 3, taxas: 0, valorBruto: 60, custoPosicao: 1200, resultadoRealizado: -1140, rentabilidadePercentual: -95, moeda: 'USD', dataVenda: '2026-09-10' },
    ];
    await TestBed.configureTestingModule({
      imports: [VendasPage],
      providers: [{ provide: CarteirasApiService, useValue: { list: () => of({ content: [{ id: 1, nome: 'Jhonatan', dataCriacao: '' }] }), listSales: () => of(sales), listPositions: () => of([]) } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(VendasPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.bar-chart')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.metrics')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.history-card')).not.toBeNull();
    expect(fixture.nativeElement.textContent.replace(/\u00a0/g, ' ')).toContain('US$ 22.140,00');
    expect(fixture.nativeElement.textContent).toContain('Selecione uma posição');
  });
});
