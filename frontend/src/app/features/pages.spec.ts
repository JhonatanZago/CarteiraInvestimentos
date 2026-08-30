import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it } from 'vitest';
import { ActivatedRoute } from '@angular/router';
import { CorretorasApiService, DashboardApiService } from '../core/api/api.services';
import { CorretorasPage, DashboardPage } from './pages';

describe('CorretorasPage', () => {
  it('renders the reactive broker registration form', async () => {
    await TestBed.configureTestingModule({
      imports: [CorretorasPage],
      providers: [{ provide: CorretorasApiService, useValue: { list: () => of({ content: [] }) } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(CorretorasPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input[formControlName="cnpj"]')).not.toBeNull();
  });
});

describe('DashboardPage', () => {
  it('renders indicators and the latest update from the mocked backend response', async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '7' } } } },
        { provide: DashboardApiService, useValue: { getPortfolio: () => of({ carteiraId: 7, valorInvestido: 100, valorAtual: 125, resultado: 25, rentabilidadePercentual: 25, ultimaAtualizacao: '2026-08-26T12:00:00Z', quantidadeAtivos: 0, composicao: [] }) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('125');
    expect(fixture.nativeElement.textContent).toContain('2026-08-26T12:00:00Z');
  });
});
