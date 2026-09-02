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
  it('normalizes CNPJ and CEP before sending', async () => {
    let payload: any;
    await TestBed.configureTestingModule({ imports: [CorretorasPage], providers: [{ provide: CorretorasApiService, useValue: { list: () => of({ content: [] }), create: (value: any) => { payload = value; return of(value); } } }] }).compileComponents();
    const page = TestBed.createComponent(CorretorasPage).componentInstance;
    page.form.setValue({ cnpj: '12.345.678/0001-90', cep: '12345-678', numero: '10', complemento: '' }); page.save();
    expect(payload.cnpj).toBe('12345678000190'); expect(payload.cep).toBe('12345678');
  });
  it('preserves broker values when the request fails', async () => {
    await TestBed.configureTestingModule({ imports: [CorretorasPage], providers: [{ provide: CorretorasApiService, useValue: { list: () => of({ content: [] }), create: () => { throw new Error('fail'); } } }] }).compileComponents();
    const page = TestBed.createComponent(CorretorasPage).componentInstance; page.form.setValue({ cnpj: '123', cep: '456', numero: '7', complemento: '' });
    expect(() => page.save()).toThrow(); expect(page.form.controls.cnpj.value).toBe('123');
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

describe('frontend form usability', () => {
  it('keeps labels and validation affordances visible for broker fields', async () => {
    await TestBed.configureTestingModule({ imports: [CorretorasPage], providers: [{ provide: CorretorasApiService, useValue: { list: () => of({ content: [] }) } }] }).compileComponents();
    const fixture = TestBed.createComponent(CorretorasPage); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-label="CNPJ"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="CEP"]')).not.toBeNull();
    const button = fixture.nativeElement.querySelector('button'); expect(button).toBeTruthy();
  });
});
