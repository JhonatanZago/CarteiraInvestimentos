import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AcoesApiService, CarteirasApiService, CorretorasApiService } from '../core/api/api.services';
import { Acao, Corretora } from '../core/api/api.models';
import { AcoesPage, CarteirasPage, CorretorasPage } from './pages';

const acao = { id: 12, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'BRASIL' as const, moeda: 'BRL', cotacaoAtual: 30, dataHoraCotacao: '2026-09-01T12:00:00Z' };
const corretora = { id: 34, cnpj: '12345678000195', razaoSocial: 'XP Investimentos', nomeFantasia: 'XP', validadaMercadoFinanceiro: true, dataCadastro: '2026-09-01T12:00:00Z' };
const carteira = { id: 56, nome: 'Longo prazo', descricao: '', dataCriacao: '2026-09-01T12:00:00Z' };

function carteiraApi() {
  return {
    list: () => of({ content: [] }),
    listPositions: () => of([]),
    createPosition: vi.fn(() => of({})),
    updatePosition: vi.fn(() => of({})),
    deletePosition: vi.fn(() => of({})),
  };
}

async function createCarteirasPage(acoes: Acao[] = [acao], corretoras: Corretora[] = [corretora]) {
  const api = carteiraApi();
  await TestBed.configureTestingModule({
    imports: [CarteirasPage],
    providers: [
      { provide: CarteirasApiService, useValue: api },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } },
      { provide: AcoesApiService, useValue: { list: () => of({ content: acoes }), getById: (id: number) => of(acoes.find((item) => item.id === id)!) } },
      { provide: CorretorasApiService, useValue: { list: () => of({ content: corretoras }) } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(CarteirasPage);
  const page = fixture.componentInstance;
  page.select(carteira);
  fixture.detectChanges();
  return { api, fixture, page };
}

describe('identificadores internos nas listagens', () => {
  it('exibe o ID da corretora na tabela', async () => {
    await TestBed.configureTestingModule({
      imports: [CorretorasPage],
      providers: [{ provide: CorretorasApiService, useValue: { list: () => of({ content: [corretora] }) } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(CorretorasPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('#34');
    expect(fixture.nativeElement.querySelector('.entity-id-badge[title="Identificador interno: 34"]')).not.toBeNull();
  });

  it('exibe o ID do ativo na tabela', async () => {
    await TestBed.configureTestingModule({
      imports: [AcoesPage],
      providers: [
        { provide: AcoesApiService, useValue: { list: () => of({ content: [acao] }) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(AcoesPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('#12');
    expect(fixture.nativeElement.querySelector('.entity-id-badge[title="Identificador interno: 12"]')).not.toBeNull();
  });
});

describe('seleção de posição na carteira', () => {
  it('lista ID, ticker e empresa no select de ação', async () => {
    const { fixture } = await createCarteirasPage();
    expect((fixture.nativeElement.querySelector('#acaoId') as HTMLSelectElement).textContent).toContain('#12 — PETR4 — Petrobras');
  });

  it('lista ID e instituição no select de corretora', async () => {
    const { fixture } = await createCarteirasPage();
    expect((fixture.nativeElement.querySelector('#corretoraId') as HTMLSelectElement).textContent).toContain('#34 — XP');
  });

  it('atribui um ID numérico ao selecionar uma ação', async () => {
    const { fixture, page } = await createCarteirasPage();
    const select = fixture.nativeElement.querySelector('#acaoId') as HTMLSelectElement;
    select.selectedIndex = 1;
    select.dispatchEvent(new Event('change'));
    expect(page.positionForm.controls.acaoId.value).toBe(12);
  });

  it('mostra a cotação da ação e a usa como preço automático', async () => {
    const { fixture, page } = await createCarteirasPage();
    page.positionForm.controls.acaoId.setValue(12);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Cotação atual');
    expect(fixture.nativeElement.textContent).toContain('R$ 30,00');
    expect(page.positionForm.controls.precoMedio.value).toBe(30);
  });

  it('deixa o preço automático somente leitura', async () => {
    const { fixture, page } = await createCarteirasPage();
    page.positionForm.controls.acaoId.setValue(12);
    fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('[formControlName="precoMedio"]') as HTMLInputElement).readOnly).toBe(true);
  });

  it('permite preço informado menor que a cotação', async () => {
    const { fixture, page } = await createCarteirasPage();
    page.positionForm.controls.acaoId.setValue(12);
    page.setTipoPreco('PRECO_INFORMADO');
    page.positionForm.controls.precoMedio.setValue(25);
    fixture.detectChanges();
    expect(page.positionForm.valid).toBe(false);
    expect(page.positionForm.controls.precoMedio.value).toBe(25);
    expect(fixture.nativeElement.textContent).toContain('16.67% abaixo da cotação atual');
  });

  it('atribui um ID numérico ao selecionar uma corretora', async () => {
    const { fixture, page } = await createCarteirasPage();
    const select = fixture.nativeElement.querySelector('#corretoraId') as HTMLSelectElement;
    select.selectedIndex = 1;
    select.dispatchEvent(new Event('change'));
    expect(page.positionForm.controls.corretoraId.value).toBe(34);
  });

  it('mantém o payload com IDs numéricos ao salvar', async () => {
    const { api, page } = await createCarteirasPage();
    page.positionForm.setValue({ acaoId: 12, corretoraId: 34, quantidade: 2, precoMedio: 30, dataPrimeiraCompra: '2026-09-01' });
    page.savePosition();
    expect(api.createPosition).toHaveBeenCalledWith(56, {
      acaoId: 12, corretoraId: 34, quantidade: 2, precoMedio: 30, dataPrimeiraCompra: '2026-09-01',
    });
  });

  it('calcula total investido e valor atual a partir de preço médio e cotação', async () => {
    const { page } = await createCarteirasPage();
    page.positionForm.setValue({ acaoId: 12, corretoraId: 34, quantidade: 2, precoMedio: 25, dataPrimeiraCompra: '2026-09-01' });
    expect(page.resumoPosicao()?.totalInvestido).toBe(50);
    expect(page.resumoPosicao()?.valorAtual).toBe(60);
    expect(page.resumoPosicao()?.resultado).toBe(10);
  });

  it('seleciona automaticamente os IDs atuais ao editar uma posição', async () => {
    const { fixture, page } = await createCarteirasPage();
    page.editPosition({ id: 1, carteiraId: 56, acaoId: 12, corretoraId: 34, quantidade: 2, precoMedio: 30, dataPrimeiraCompra: '2026-09-01', mercado: 'BRASIL', classificacaoAlocacao: 'ACOES_BRASIL', ticker: 'PETR4', nomeEmpresa: 'Petrobras', cotacaoAtual: 30, dataHoraCotacao: null, valorInvestido: 60, valorAtual: 60, resultado: 0, rentabilidadePercentual: 0 });
    fixture.detectChanges();
    expect(page.positionForm.controls.acaoId.value).toBe(12);
    expect(page.positionForm.controls.corretoraId.value).toBe(34);
    expect((fixture.nativeElement.querySelector('#acaoId') as HTMLSelectElement).selectedOptions[0].textContent).toContain('PETR4');
    expect((fixture.nativeElement.querySelector('#corretoraId') as HTMLSelectElement).selectedOptions[0].textContent).toContain('XP');
  });

  it('desabilita Adicionar posição e mostra orientações quando as listas estão vazias', async () => {
    const { fixture } = await createCarteirasPage([], []);
    const button = Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button')).find((element) => element.textContent?.includes('Adicionar posição')) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Nenhum ativo disponível. Cadastre um ativo antes de adicionar uma posição.');
    expect(fixture.nativeElement.textContent).toContain('Nenhuma corretora disponível. Cadastre uma corretora antes de adicionar uma posição.');
    expect(fixture.nativeElement.querySelector('a[href="/acoes"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/corretoras"]')).not.toBeNull();
  });

  it('impede cotação automática quando não há cotação disponível', async () => {
    const indisponivel = { ...acao, cotacaoAtual: null, dataHoraCotacao: null };
    const { fixture, page } = await createCarteirasPage([indisponivel]);
    page.positionForm.controls.acaoId.setValue(12);
    fixture.detectChanges();
    const automatico = fixture.nativeElement.querySelector('input[name="tipoPreco"]') as HTMLInputElement;
    expect(page.cotacaoDisponivel()).toBe(false);
    expect(automatico.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Não foi possível calcular o valor atual.');
  });
});
