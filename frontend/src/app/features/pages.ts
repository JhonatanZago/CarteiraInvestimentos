import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  AcoesApiService,
  CarteirasApiService,
  CorretorasApiService,
  DashboardApiService,
} from '../core/api/api.services';
import {
  Acao,
  Carteira,
  Corretora,
  DashboardCarteira,
  HistoricoCotacao,
  Posicao,
} from '../core/api/api.models';
import { NotificationService } from '../core/feedback/notification.service';
import {
  ConfirmDialogComponent,
  EmptyStateComponent,
  LoadingComponent,
} from '../shared/feedback.component';

const money = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, EmptyStateComponent, LoadingComponent],
  template: ` <section class="page">
    <header class="page-header">
      <div>
        <h1>Corretoras</h1>
        <p class="muted">Cadastro validado pelo backend.</p>
      </div>
      <input
        aria-label="Consultar CNPJ"
        #cnpj
        placeholder="Consultar CNPJ"
        (keyup.enter)="lookup(cnpj.value)"
      />
    </header>
    <section class="panel broker-form-card">
      <form class="broker-form-grid" [formGroup]="form" (ngSubmit)="save()">
        <label>CNPJ<input formControlName="cnpj" placeholder="00.000.000/0000-00" aria-label="CNPJ" inputmode="numeric" /><small class="help-text">Informe o CNPJ da instituição.</small></label><label>CEP<input
          formControlName="cep"
          placeholder="00000-000" inputmode="numeric"
          aria-label="CEP"
        /></label><label>Número<input formControlName="numero" placeholder="Número" aria-label="Número" /></label><label>Complemento<input
          formControlName="complemento"
          placeholder="Complemento"
          aria-label="Complemento"
        /></label><button [disabled]="form.invalid || saving()">
          {{ saving() ? 'Validando…' : 'Cadastrar' }}
        </button>
      </form>
    </section>
    @if (loading()) {
      <app-loading />
    } @else if (!items().length) {
      <app-empty-state title="Nenhuma corretora" detail="Cadastre uma instituição para começar." />
    } @else {
      <section class="panel">
        <table>
          <thead>
            <tr>
              <th>Instituição</th>
              <th>CNPJ</th>
              <th>Situação</th>
            </tr>
          </thead>
          <tbody>
            @for (item of items(); track item.id) {
              <tr>
                <td>{{ item.razaoSocial }}</td>
                <td>{{ item.cnpj }}</td>
                <td><span class="status-badge" [class.validated]="item.validadaMercadoFinanceiro" [class.pending]="!item.validadaMercadoFinanceiro">{{ item.validadaMercadoFinanceiro ? 'Validada' : 'Pendente' }}</span></td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  </section>`,
})
export class CorretorasPage {
  private readonly api = inject(CorretorasApiService);
  private readonly fb = inject(FormBuilder);
  private readonly notice = inject(NotificationService);
  readonly items = signal<Corretora[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({
    cnpj: ['', Validators.required],
    cep: ['', Validators.required],
    numero: ['', Validators.required],
    complemento: [''],
  });
  constructor() {
    this.load();
  }
  load(): void {
    this.loading.set(true);
    this.api
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((page) => this.items.set(page.content));
  }
  save(): void {
    if (!this.form.valid || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.api
      .create({ ...this.form.getRawValue(), cnpj: this.form.controls.cnpj.value.replace(/\D/g, ''), cep: this.form.controls.cep.value.replace(/\D/g, '') })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({ next: () => {
        this.notice.show('Corretora cadastrada.', 'success');
        this.form.reset({ cnpj: '', cep: '', numero: '', complemento: '' });
        this.load();
      }, error: () => this.error.set('Não foi possível cadastrar a corretora. Confira os dados e tente novamente.') });
  }
  lookup(cnpj: string): void {
    if (cnpj)
      this.api.getByCnpj(cnpj).subscribe((item) => {
        this.items.set([item]);
        this.notice.show('Corretora localizada.', 'success');
      });
  }
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, EmptyStateComponent, LoadingComponent],
  template: ` <section class="page">
    <header class="page-header">
      <div>
        <h1>Ações</h1>
        <p class="muted">Cotações atualizadas pelo serviço de mercado do backend.</p>
      </div>
      <input
        aria-label="Consultar ticker"
        #tickerLookup
        placeholder="Consultar ticker"
        (keyup.enter)="lookup(tickerLookup.value)"
      />
    </header>
    <section class="panel asset-form-card">
      <form class="asset-form-grid" [formGroup]="form" (ngSubmit)="save()">
        <label>Ticker<input formControlName="ticker" placeholder="Ex.: PETR4" aria-label="Ticker" /></label><small class="help-text">Código de negociação do ativo na bolsa.</small><select
          formControlName="mercado"
          aria-label="Mercado"
        >
          <option value="BRASIL">Brasil</option>
          <option value="EUA">Estados Unidos</option></select><small class="help-text">Brasil: B3. EUA: ações americanas.</small
        ><button [disabled]="form.invalid || saving()">
          {{ saving() ? 'Cadastrando…' : 'Cadastrar ação' }}
        </button>
      </form>
    </section>
    @if (loading()) {
      <app-loading />
    } @else if (!items().length) {
      <app-empty-state title="Nenhuma ação" detail="Cadastre uma ação para consultar cotações." />
    } @else {
      <section class="panel">
        <table>
          <thead>
            <tr>
              <th>Ticker</th>
              <th>Empresa</th>
              <th>Cotação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            @for (item of items(); track item.id) {
              <tr>
                <td>
                  <a class="ticker-badge" [routerLink]="['/historico', item.id]">{{ item.ticker }}</a>
                </td>
                <td>{{ item.nomeEmpresa }}</td>
                <td>{{ money(item.cotacaoAtual) }}</td>
                <td>
                  <button
                    class="secondary"
                    [disabled]="refreshing() === item.id"
                    (click)="refresh(item)"
                  >
                    Atualizar
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  </section>`,
})
export class AcoesPage {
  private readonly api = inject(AcoesApiService);
  private readonly fb = inject(FormBuilder);
  private readonly notice = inject(NotificationService);
  readonly money = money;
  readonly items = signal<Acao[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly refreshing = signal<number | null>(null);
  readonly form = this.fb.nonNullable.group({
    ticker: ['', Validators.required],
    mercado: ['BRASIL' as 'BRASIL' | 'EUA', Validators.required],
  });
  constructor() {
    this.load();
  }
  load(): void {
    this.loading.set(true);
    this.api
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((page) => this.items.set(page.content));
  }
  save(): void {
    if (!this.form.valid || this.saving()) return;
    this.saving.set(true);
    this.api
      .create({ ...this.form.getRawValue(), ticker: this.form.controls.ticker.value.trim().toUpperCase() })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe(() => {
        this.notice.show('Ação cadastrada.', 'success');
        this.form.reset({ ticker: '', mercado: 'BRASIL' });
        this.load();
      });
  }
  lookup(ticker: string): void {
    if (ticker) this.api.getByTicker(ticker).subscribe((item) => this.items.set([item]));
  }
  refresh(item: Acao): void {
    this.refreshing.set(item.id);
    this.api
      .refresh(item.id)
      .pipe(finalize(() => this.refreshing.set(null)))
      .subscribe(() => {
        this.notice.show('Cotação atualizada.', 'success');
        this.load();
      });
  }
}

@Component({
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    EmptyStateComponent,
    LoadingComponent,
    ConfirmDialogComponent,
  ],
  template: ` <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">SEU PATRIMÔNIO</p>
        <h1>Carteiras</h1>
        <p class="muted">Posições e resultados são calculados no backend.</p>
      </div>
    </header>
    <section class="panel portfolio-form-card">
      <div class="form-card-heading"><span class="form-icon" aria-hidden="true">▣</span><div><h2>{{ editing() ? 'Editar carteira' : 'Nova carteira' }}</h2><p class="muted">Defina um nome e uma descrição para identificar sua carteira.</p></div></div>
      <form class="portfolio-form-grid" [formGroup]="portfolioForm" (ngSubmit)="savePortfolio()">
        <label>Nome da carteira<input
          formControlName="nome"
          placeholder="Nome da carteira"
          aria-label="Nome da carteira"
        /></label><label class="description-field">Descrição<input
          formControlName="descricao"
          placeholder="Descrição"
          aria-label="Descrição"
        /></label><div class="form-actions"><button [disabled]="portfolioForm.invalid || saving()">
          {{ editing() ? 'Salvar alterações' : 'Criar carteira' }}</button
        ><button class="secondary" type="button" (click)="clearEditing()">Limpar</button></div>
      </form>
    </section>
    <section class="grid" style="grid-template-columns: minmax(280px, .7fr) minmax(0, 1.7fr); gap: 20px">
      <section class="panel portfolio-list-panel">
        <header class="collection-heading"><h2>Minhas carteiras</h2><span class="portfolio-count">{{ portfolios().length }}</span></header>
        @if (loading()) {
          <app-loading />
        } @else if (!portfolios().length) {
          <app-empty-state title="Nenhuma carteira" />
        } @else {
          @for (item of portfolios(); track item.id) {
            <article class="portfolio-card" [class.selected]="selected()?.id === item.id">
              <button class="portfolio-card-select" (click)="select(item)"><span class="portfolio-initial">{{ item.nome.charAt(0).toUpperCase() }}</span><span class="portfolio-card-copy"><strong>{{ item.nome }}</strong>@if (item.descricao) { <small>{{ item.descricao }}</small> }</span></button>
              <button class="portfolio-delete" aria-label="Excluir carteira" (click)="requestDelete(item)">Excluir</button>
            </article>
          }
        }
      </section>
      <section class="panel portfolio-detail-panel">
        @if (selected()) {
          <header class="portfolio-detail-header"><span class="portfolio-initial">{{ selected()!.nome.charAt(0).toUpperCase() }}</span><div class="portfolio-detail-copy"><h2>{{ selected()!.nome }}</h2><p class="muted">{{ selected()!.descricao || 'Carteira de investimentos' }}</p></div><span class="selected-badge">Selecionada</span><a class="overview-link" [routerLink]="['/dashboard', selected()!.id]">Abrir visão geral</a></header>
          <hr class="section-divider"><h2>Adicionar posição</h2>
          <form class="position-form-grid" [formGroup]="positionForm" (ngSubmit)="savePosition()">
            <label>ID da ação<input
              type="number"
              formControlName="acaoId"
              placeholder="ID ação"
              aria-label="ID ação"
            /></label><label>ID da corretora<input
              type="number"
              formControlName="corretoraId"
              placeholder="ID corretora"
              aria-label="ID corretora"
            /></label><label>Quantidade<input
              type="number"
              formControlName="quantidade"
              placeholder="Quantidade"
              aria-label="Quantidade"
            /></label><label>Preço médio<input
              type="number"
              formControlName="precoMedio"
              placeholder="Preço médio"
              aria-label="Preço médio"
            /></label><label>Data da primeira compra<input
              type="date"
              formControlName="dataPrimeiraCompra"
              aria-label="Data da compra"
            /></label><button [disabled]="positionForm.invalid || saving()">
              {{ editingPosition() ? 'Salvar posição' : 'Adicionar posição' }}
            </button>
          </form>
          @if (!positions().length) {
            <app-empty-state title="Sem posições" detail="Adicione uma posição a esta carteira." />
          } @else {
            <table>
              <thead>
                <tr>
                  <th>Ativo</th>
                  <th>Quantidade</th>
                  <th>Preço médio</th>
                  <th>Investido</th>
                  <th>Atual</th>
                  <th>Resultado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (position of positions(); track position.id) {
                  <tr>
                    <td><strong>{{ position.ticker }}</strong><small>{{ position.nomeEmpresa }}</small></td>
                    <td>{{ position.quantidade }}</td>
                    <td>{{ money(position.precoMedio) }}</td>
                    <td>{{ money(position.valorInvestido) }}</td>
                    <td>{{ money(position.valorAtual) }}</td>
                    <td
                      [class.positive]="position.resultado >= 0"
                      [class.negative]="position.resultado < 0"
                    >
                      {{ money(position.resultado) }}
                    </td>
                    <td>
                      <button class="secondary" (click)="editPosition(position)">Editar</button>
                      <button class="danger" (click)="removePosition(position)">Excluir</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        } @else {
          <app-empty-state
            title="Selecione uma carteira"
            detail="Escolha uma carteira para gerenciar as posições."
          />
        }
      </section>
    </section>
    <app-confirm-dialog
      [open]="!!pendingDelete()"
      title="Excluir carteira"
      detail="A carteira só pode ser removida sem posições."
      (cancelled)="pendingDelete.set(null)"
      (confirmed)="deletePortfolio()"
    />
  </section>`,
})
export class CarteirasPage {
  private readonly api = inject(CarteirasApiService);
  private readonly acoesApi = inject(AcoesApiService);
  private readonly corretorasApi = inject(CorretorasApiService);
  private readonly fb = inject(FormBuilder);
  private readonly notice = inject(NotificationService);
  readonly money = money;
  readonly acoes = signal<Acao[]>([]);
  readonly corretoras = signal<Corretora[]>([]);
  readonly portfolios = signal<Carteira[]>([]);
  readonly positions = signal<Posicao[]>([]);
  readonly selected = signal<Carteira | null>(null);
  readonly editing = signal<Carteira | null>(null);
  readonly editingPosition = signal<Posicao | null>(null);
  readonly pendingDelete = signal<Carteira | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly portfolioForm = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    descricao: [''],
  });
  readonly positionForm = this.fb.group({
    acaoId: this.fb.control<number | null>(null, Validators.required),
    corretoraId: this.fb.control<number | null>(null, Validators.required),
    quantidade: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.000001),
    ]),
    precoMedio: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    dataPrimeiraCompra: this.fb.control<string | null>(null, Validators.required),
  });
  constructor() {
    this.load();
    this.acoesApi.list(0, 100).subscribe((page) => this.acoes.set(page.content));
    this.corretorasApi.list(0, 100).subscribe((page) => this.corretoras.set(page.content));
  }
  load(): void {
    this.loading.set(true);
    this.api
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((page) => this.portfolios.set(page.content));
  }
  savePortfolio(): void {
    if (!this.portfolioForm.valid || this.saving()) return;
    const current = this.editing();
    this.saving.set(true);
    const request = current
      ? this.api.update(current.id, this.portfolioForm.getRawValue())
      : this.api.create(this.portfolioForm.getRawValue());
    request.pipe(finalize(() => this.saving.set(false))).subscribe(() => {
      this.notice.show('Carteira salva.', 'success');
      this.clearEditing();
      this.load();
    });
  }
  select(item: Carteira): void {
    this.selected.set(item);
    this.editing.set(item);
    this.portfolioForm.setValue({ nome: item.nome, descricao: item.descricao ?? '' });
    this.loadPositions();
  }
  clearEditing(): void {
    this.editing.set(null);
    this.portfolioForm.reset({ nome: '', descricao: '' });
  }
  loadPositions(): void {
    const item = this.selected();
    if (item)
      this.api.listPositions(item.id).subscribe((positions) => this.positions.set(positions));
  }
  savePosition(): void {
    const portfolio = this.selected();
    const value = this.positionForm.getRawValue();
    if (
      !portfolio ||
      !this.positionForm.valid ||
      this.saving() ||
      value.acaoId === null ||
      value.corretoraId === null ||
      value.quantidade === null ||
      value.precoMedio === null ||
      value.dataPrimeiraCompra === null
    )
      return;
    this.saving.set(true);
    const edit = this.editingPosition();
    const requestBody = {
      acaoId: value.acaoId,
      corretoraId: value.corretoraId,
      quantidade: value.quantidade,
      precoMedio: value.precoMedio,
      dataPrimeiraCompra: value.dataPrimeiraCompra,
    };
    const request = edit
      ? this.api.updatePosition(portfolio.id, edit.id, requestBody)
      : this.api.createPosition(portfolio.id, requestBody);
    request.pipe(finalize(() => this.saving.set(false))).subscribe(() => {
      this.notice.show('Posição salva.', 'success');
      this.editingPosition.set(null);
      this.positionForm.reset({
        acaoId: null,
        corretoraId: null,
        quantidade: null,
        precoMedio: null,
        dataPrimeiraCompra: null,
      });
      this.loadPositions();
    });
  }
  editPosition(item: Posicao): void {
    this.editingPosition.set(item);
    this.positionForm.setValue({
      acaoId: item.acaoId,
      corretoraId: item.corretoraId,
      quantidade: item.quantidade,
      precoMedio: item.precoMedio,
      dataPrimeiraCompra: item.dataPrimeiraCompra,
    });
  }
  removePosition(item: Posicao): void {
    const portfolio = this.selected();
    if (portfolio)
      this.api.deletePosition(portfolio.id, item.id).subscribe(() => {
        this.notice.show('Posição removida.', 'success');
        this.loadPositions();
      });
  }
  requestDelete(item: Carteira): void {
    this.pendingDelete.set(item);
  }
  deletePortfolio(): void {
    const item = this.pendingDelete();
    if (item)
      this.api.delete(item.id).subscribe(() => {
        this.notice.show('Carteira removida.', 'success');
        this.pendingDelete.set(null);
        if (this.selected()?.id === item.id) this.selected.set(null);
        this.load();
      });
  }
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, EmptyStateComponent, LoadingComponent],
  template: ` <section class="page">
    <header class="page-header">
      <div>
        <h1>Histórico de cotação</h1>
        <p class="muted">Pontos retornados pela API, do mais recente ao mais antigo.</p>
      </div>
    </header>
    <section class="panel portfolio-list-panel">
      <form [formGroup]="form" (ngSubmit)="load()">
        <input
          type="number"
          formControlName="acaoId"
          placeholder="ID da ação"
          aria-label="ID da ação"
        /><button [disabled]="form.invalid || loading()">Consultar</button>
      </form>
    </section>
    @if (loading()) {
      <app-loading />
    } @else if (!items().length) {
      <app-empty-state
        title="Sem histórico disponível"
        detail="A tabela será exibida quando houver cotações armazenadas."
      />
    } @else {
      <section class="panel">
        <div class="chart" aria-label="Gráfico de histórico de cotação">
          @for (item of items(); track item.id) {
            <div
              class="bar"
              [style.height.%]="height(item.valor)"
              [title]="item.dataHoraCotacao + ': ' + item.valor"
            ></div>
          }
        </div>
        <table>
          <thead>
            <tr>
              <th>Data</th>
              <th>Valor</th>
              <th>Fonte</th>
            </tr>
          </thead>
          <tbody>
            @for (item of items(); track item.id) {
              <tr>
                <td>{{ item.dataHoraCotacao }}</td>
                <td>{{ money(item.valor) }}</td>
                <td>{{ item.fonte }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  </section>`,
})
export class HistoricoPage {
  private readonly api = inject(AcoesApiService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  readonly money = money;
  readonly items = signal<HistoricoCotacao[]>([]);
  readonly loading = signal(false);
  readonly form = this.fb.nonNullable.group({ acaoId: [0, Validators.min(1)] });
  readonly max = computed(() => Math.max(...this.items().map((item) => item.valor), 1));
  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('acaoId'));
    if (id > 0) {
      this.form.controls.acaoId.setValue(id);
      this.load();
    }
  }
  height(value: number): number {
    return Math.max(5, (value / this.max()) * 100);
  }
  load(): void {
    if (!this.form.valid) return;
    this.loading.set(true);
    this.api
      .history(this.form.controls.acaoId.value)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((items) => this.items.set(items));
  }
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, EmptyStateComponent, LoadingComponent],
  template: ` <section class="page">
    <header class="page-header">
      <div>
        <h1>Dashboard</h1>
        <p class="muted">Indicadores consolidados calculados pelo backend.</p>
      </div>
    </header>
    <section class="panel">
      <form [formGroup]="form" (ngSubmit)="load()">
        <input
          type="number"
          formControlName="carteiraId"
          placeholder="ID da carteira"
          aria-label="ID da carteira"
        /><button [disabled]="form.invalid || loading()">Consultar</button>
      </form>
    </section>
    @if (loading()) {
      <app-loading />
    } @else if (data(); as dashboard) {
      <section class="metrics grid">
        <article class="metric">
          <span>Investido</span><strong>{{ money(dashboard.valorInvestido) }}</strong>
        </article>
        <article class="metric">
          <span>Atual</span><strong>{{ money(dashboard.valorAtual) }}</strong>
        </article>
        <article class="metric">
          <span>Resultado</span
          ><strong
            [class.positive]="dashboard.resultado >= 0"
            [class.negative]="dashboard.resultado < 0"
            >{{ money(dashboard.resultado) }}</strong
          >
        </article>
        <article class="metric">
          <span>Rentabilidade</span><strong>{{ dashboard.rentabilidadePercentual }}%</strong>
        </article>
      </section>
      <section class="panel">
        <h2>Composição ({{ dashboard.quantidadeAtivos }} ativos)</h2>
        <p class="muted">Última atualização: {{ dashboard.ultimaAtualizacao ?? 'indisponível' }}</p>
        @if (!dashboard.composicao.length) {
          <app-empty-state title="Carteira sem posições" />
        } @else {
          <table>
            <thead>
              <tr>
                <th>Ativo</th>
                <th>Investido</th>
                <th>Atual</th>
                <th>Resultado</th>
              </tr>
            </thead>
            <tbody>
              @for (item of dashboard.composicao; track item.posicaoId) {
                <tr>
                  <td>{{ item.ticker }}</td>
                  <td>{{ money(item.valorInvestido) }}</td>
                  <td>{{ money(item.valorAtual) }}</td>
                  <td [class.positive]="item.resultado >= 0" [class.negative]="item.resultado < 0">
                    {{ money(item.resultado) }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>
    } @else {
      <app-empty-state
        title="Consulte uma carteira"
        detail="Informe o identificador para visualizar os indicadores."
      />
    }
  </section>`,
})
export class DashboardPage {
  private readonly api = inject(DashboardApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  readonly money = money;
  readonly data = signal<DashboardCarteira | null>(null);
  readonly loading = signal(false);
  readonly form = this.fb.nonNullable.group({ carteiraId: [0, Validators.min(1)] });
  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('carteiraId'));
    if (id > 0) {
      this.form.controls.carteiraId.setValue(id);
      this.load();
    }
  }
  load(): void {
    if (!this.form.valid) return;
    this.loading.set(true);
    this.api
      .getPortfolio(this.form.controls.carteiraId.value)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((data) => this.data.set(data));
  }
}
