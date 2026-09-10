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
import { AssetLogoComponent } from '../shared/asset-logo.component';
import { BrokerLogoComponent } from '../shared/broker-logo.component';
import { formatMoney as money } from '../shared/money.util';


type TipoPreco = 'COTACAO_ATUAL' | 'PRECO_INFORMADO';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, EmptyStateComponent, LoadingComponent, BrokerLogoComponent],
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
        <label class="form-field"><span class="field-label">CNPJ</span><input formControlName="cnpj" placeholder="00.000.000/0000-00" aria-label="CNPJ" inputmode="numeric" /><small class="help-text">Informe o CNPJ da instituição.</small></label>
        <label class="form-field"><span class="field-label">CEP</span><input formControlName="cep" placeholder="00000-000" inputmode="numeric" aria-label="CEP" /><small class="help-text help-placeholder" aria-hidden="true">&nbsp;</small></label>
        <label class="form-field"><span class="field-label">Número</span><input formControlName="numero" placeholder="Número" aria-label="Número" /></label>
        <label class="form-field"><span class="field-label">Complemento</span><input formControlName="complemento" placeholder="Complemento" aria-label="Complemento" /></label>
        <button [disabled]="form.invalid || saving()">
          {{ saving() ? 'Validando…' : 'Cadastrar' }}
        </button>
      </form>
      @if (error()) { <p role="alert" class="notice error">{{ error() }}</p> }
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
              <th>ID</th>
              <th>Instituição</th>
              <th>CNPJ</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            @for (item of items(); track item.id) {
              <tr>
                <td><span class="entity-id-badge" [attr.title]="'Identificador interno: ' + item.id">#{{ item.id }}</span></td>
                <td><div class="broker-identity"><app-broker-logo [cnpj]="item.cnpj" [name]="item.nomeFantasia || item.razaoSocial" [logoUrl]="item.logoUrl" [size]="44" /><span><strong>{{ item.nomeFantasia || item.razaoSocial }}</strong><small>{{ item.razaoSocial }}</small><small>{{ item.cidade }}{{ item.uf ? '/' + item.uf : '' }}</small></span></div></td>
                <td>{{ item.cnpj }}</td>
                <td><span class="status-badge">{{ item.statusValidacao || (item.validadaMercadoFinanceiro ? 'VALIDADA' : 'AGUARDANDO_VALIDACAO') }}</span><small>{{ item.motivoValidacao }}</small></td>
                <td><button type="button" (click)="revalidate(item)" [disabled]="revalidating() === item.id || deleting()">{{ revalidating() === item.id ? 'Validando…' : 'Validar novamente' }}</button><button type="button" class="danger" (click)="confirmDelete(item)" [disabled]="deleting()">Excluir</button></td>
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
  readonly revalidating = signal<number | null>(null);
  readonly error = signal('');
  readonly deleting = signal(false);
  readonly form = this.fb.nonNullable.group({
    cnpj: ['', Validators.required],
    cep: ['', Validators.required],
    numero: ['', Validators.required],
    complemento: [''],
  });
  constructor() {
    this.load();
  }
  revalidate(item: Corretora): void {
    this.revalidating.set(item.id);
    this.api.revalidate(item.id).pipe(finalize(() => this.revalidating.set(null))).subscribe(updated => {
      this.items.update(items => items.map(current => current.id === updated.id ? updated : current));
      this.notice.show('Corretora revalidada.', 'success');
    });
  }
  confirmDelete(item: Corretora): void {
    const nome = item.nomeFantasia || item.razaoSocial;
    if (!window.confirm(`Deseja excluir ${nome}? Esta ação não poderá ser desfeita.`) || this.deleting()) return;
    this.deleting.set(true);
    this.api.delete(item.id).pipe(finalize(() => this.deleting.set(false))).subscribe({
      next: () => { this.items.update(items => items.filter(current => current.id !== item.id)); this.notice.show('Corretora excluída com sucesso.', 'success'); },
      error: (error) => this.notice.show(error?.error?.message ?? 'Não foi possível excluir a corretora.', 'error'),
    });
  }

  load(): void {
    this.loading.set(true);
    this.api
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({ next: (page) => this.items.set(page.content), error: (error) => this.error.set(error?.error?.message ?? 'Não foi possível carregar os ativos.') });
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
  imports: [ReactiveFormsModule, RouterLink, EmptyStateComponent, LoadingComponent, AssetLogoComponent, ConfirmDialogComponent],
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
      @if (error()) { <p role="alert" class="notice error">{{ error() }}</p> }
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
              <th>ID</th>
              <th>Ticker</th>
              <th>Empresa</th>
              <th>Cotação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            @for (item of items(); track item.id) {
              <tr>
                <td><span class="entity-id-badge" [attr.title]="'Identificador interno: ' + item.id">#{{ item.id }}</span></td>
                <td>
                  <a class="ticker-badge" [routerLink]="['/historico', item.id]"><app-asset-logo [ticker]="item.ticker" [companyName]="item.nomeEmpresa" [logoUrl]="item.logoUrl" [size]="32" />{{ item.ticker.trim().toUpperCase() }}</a>
                </td>
                <td>{{ item.nomeEmpresa }}</td>
                <td>{{ money(item.cotacaoAtual, item.moeda) }}</td>
                <td>
                  <button
                    class="secondary"
                    [disabled]="refreshing() === item.id"
                    (click)="refresh(item)"
                  >
                    Atualizar
                  </button>
                  <button class="danger" type="button" [disabled]="deleting()" [attr.title]="'Excluir ativo'" [attr.aria-label]="'Excluir ativo ' + item.ticker" (click)="requestDelete(item)">Excluir</button>
                  <button class="secondary" type="button" [disabled]="revalidating() === item.id" (click)="revalidate(item)">{{ revalidating() === item.id ? 'Validando…' : 'Validar mercado' }}</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
    <app-confirm-dialog [open]="!!pendingDelete()" [title]="'Excluir ativo?'" [detail]="deleteError() || (pendingDelete() ? 'Deseja excluir ' + pendingDelete()!.ticker + ' — ' + pendingDelete()!.nomeEmpresa + '? Esta ação não poderá ser desfeita.' : '')" confirmLabel="Excluir ativo" (cancelled)="pendingDelete.set(null); deleteError.set('')" (confirmed)="deleteAsset()" />
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
  readonly pendingDelete = signal<Acao | null>(null);
  readonly deleting = signal(false);
  readonly deleteError = signal('');
  readonly revalidating = signal<number | null>(null);
  readonly error = signal('');
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
    this.error.set('');
    this.api
      .create({ ...this.form.getRawValue(), ticker: this.form.controls.ticker.value.trim().toUpperCase(), selectedCountryCode: this.form.controls.mercado.value === 'BRASIL' ? 'BR' : 'US' })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({ next: () => {
        this.notice.show('Ação cadastrada.', 'success');
        this.form.reset({ ticker: '', mercado: 'BRASIL' });
        this.load();
      }, error: (error) => this.error.set(error?.error?.message ?? 'Não foi possível cadastrar a ação. Verifique o ticker, mercado e a disponibilidade da cotação.') });
  }
  lookup(ticker: string): void {
    const normalizedTicker = ticker.trim().toUpperCase();
    if (normalizedTicker) this.api.getByTicker(normalizedTicker).subscribe({ next: (item) => this.items.set([item]), error: (error) => this.error.set(error?.error?.message ?? 'Ativo não encontrado.') });
  }
  refresh(item: Acao): void {
    if (this.refreshing() === item.id) return;
    this.refreshing.set(item.id);
    this.api
      .refresh(item.id)
      .pipe(finalize(() => this.refreshing.set(null)))
      .subscribe({ next: (updated) => {
        this.items.update(items => items.map(current => current.id === updated.id ? updated : current));
        this.notice.show('Cotação atualizada com sucesso.', 'success');
      }, error: (error) => {
        this.notice.show(error?.error?.message ?? 'Não foi possível atualizar a cotação.', 'error');
      });
  }
  requestDelete(item: Acao): void { if (!this.deleting()) { this.deleteError.set(''); this.pendingDelete.set(item); } }
  deleteAsset(): void { const item = this.pendingDelete(); if (!item || this.deleting()) return; this.deleting.set(true); this.api.delete(item.id).pipe(finalize(() => this.deleting.set(false))).subscribe({ next: () => { this.items.update(items => items.filter(current => current.id !== item.id)); this.pendingDelete.set(null); this.deleteError.set(''); this.notice.show('Ativo excluído com sucesso.', 'success'); }, error: (error) => { const message = error?.error?.message ?? (error?.status === 409 ? 'Este ativo está vinculado a uma carteira ou possui histórico financeiro.' : 'Não foi possível excluir o ativo.'); this.deleteError.set(message); this.notice.show(message, 'error'); } }); }
  revalidate(item: Acao): void { if (this.revalidating()) return; this.revalidating.set(item.id); this.api.revalidate(item.id).pipe(finalize(() => this.revalidating.set(null))).subscribe({ next: updated => { this.items.update(items => items.map(current => current.id === updated.id ? updated : current)); this.notice.show('Mercado, moeda e cotação revalidados.', 'success'); }, error: () => this.notice.show('Não foi possível revalidar este ativo agora.', 'error') }); }
}

@Component({
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    EmptyStateComponent,
    LoadingComponent,
    ConfirmDialogComponent,
    AssetLogoComponent,
    BrokerLogoComponent,
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
      <section class="panel portfolio-detail-panel portfolio-detail">
        @if (selected()) {
          <header class="portfolio-detail-header"><span class="portfolio-initial">{{ selected()!.nome.charAt(0).toUpperCase() }}</span><div class="portfolio-detail-copy"><h2>{{ selected()!.nome }}</h2><p class="muted">{{ selected()!.descricao || 'Carteira de investimentos' }}</p></div><span class="selected-badge">Selecionada</span><a class="overview-link" [routerLink]="['/dashboard', selected()!.id]">Abrir visão geral</a></header>
          <hr class="section-divider"><h2>Adicionar posição</h2>
          <form class="position-form" [formGroup]="positionForm" (ngSubmit)="savePosition()">
            <div class="position-selectors">
              <label class="form-field" for="acaoId">Ação<select id="acaoId" formControlName="acaoId" aria-label="Ação"><option [ngValue]="null">Selecione uma ação</option>@for (acao of acoes(); track acao.id) {<option [ngValue]="acao.id">#{{ acao.id }} — {{ acao.ticker }} — {{ acao.nomeEmpresa }}</option>}</select></label>
              <label class="form-field" for="corretoraId">Corretora<select id="corretoraId" formControlName="corretoraId" aria-label="Corretora"><option [ngValue]="null">Selecione uma corretora</option>@for (corretora of corretoras(); track corretora.id) {<option [ngValue]="corretora.id">#{{ corretora.id }} — {{ corretora.nomeFantasia || corretora.razaoSocial }}</option>}</select></label>
            </div>
            @if (selectedCorretora(); as corretora) { <div class="selected-broker-preview"><app-broker-logo [cnpj]="corretora.cnpj" [name]="corretora.nomeFantasia || corretora.razaoSocial" [logoUrl]="corretora.logoUrl" [size]="36" /><span>{{ corretora.nomeFantasia || corretora.razaoSocial }}</span></div> }
            @if (selectedAcao(); as acao) {
              <section class="asset-quote-card">
                <div class="asset-quote-identity"><app-asset-logo [ticker]="acao.ticker" [companyName]="acao.nomeEmpresa" [logoUrl]="acao.logoUrl" [size]="40" /><span><small>Ticker</small><strong>{{ acao.ticker.trim().toUpperCase() }}</strong></span></div><div><small>Empresa</small><strong>{{ acao.nomeEmpresa }}</strong></div><div><small>Mercado</small><strong>{{ acao.mercado }}</strong></div>
            @if (cotacaoDisponivel()) {<div><small>Cotação atual</small><strong>{{ money(acao.cotacaoAtual!, acao.moeda) }}</strong></div><div><small>Atualização</small><strong>{{ acao.dataHoraCotacao || 'data indisponível' }}</strong></div>} @else {<div class="asset-quote-unavailable">Cotação atual indisponível. Não foi possível calcular o valor atual.</div>}
              </section>
              @if (cotacaoDesatualizada()) {<p class="notice warning">A cotação pode estar desatualizada.</p>}
            }
            <fieldset class="price-mode-section"><legend>Tipo de preço</legend><div class="price-mode-options"><label class="price-mode-option"><input type="radio" name="tipoPreco" [checked]="tipoPreco() === 'COTACAO_ATUAL'" [disabled]="!cotacaoDisponivel()" (change)="setTipoPreco('COTACAO_ATUAL')" /><span>Usar cotação atual</span></label><label class="price-mode-option"><input type="radio" name="tipoPreco" [checked]="tipoPreco() === 'PRECO_INFORMADO'" (change)="setTipoPreco('PRECO_INFORMADO')" /><span>Informar preço de compra</span></label></div></fieldset>
            <div class="position-fields">
              <label class="form-field">Quantidade<input type="number" formControlName="quantidade" placeholder="Quantidade" aria-label="Quantidade" /></label>
              <label class="form-field">Preço atual<input type="number" formControlName="precoMedio" placeholder="Preço atual" aria-label="Preço médio" [readOnly]="tipoPreco() === 'COTACAO_ATUAL'" />@if (tipoPreco() === 'PRECO_INFORMADO') {<small class="field-help">Informe o valor efetivamente pago por unidade.</small>}@if (diferencaPrecoInformado(); as diferenca) {<small class="price-difference">O preço informado está {{ diferenca }}% {{ diferenca > 0 ? 'acima' : 'abaixo' }} da cotação atual.</small>}</label>
              <label class="form-field">Data da primeira compra<input type="date" formControlName="dataPrimeiraCompra" aria-label="Data da compra" /></label>
              <button class="position-submit" [disabled]="positionForm.invalid || saving() || !acoes().length || !corretoras().length">{{ editingPosition() ? 'Salvar posição' : 'Adicionar posição' }}</button>
            </div>
            @if (resumoPosicao(); as resumo) {<section class="position-preview"><div><small>Preço usado</small><strong>{{ money(resumo.precoMedio) }}</strong></div><div><small>Total investido</small><strong>{{ money(resumo.totalInvestido) }}</strong></div><div><small>Valor atual estimado</small><strong>{{ resumo.valorAtual === null ? 'Indisponível' : money(resumo.valorAtual) }}</strong></div><div><small>Resultado estimado</small><strong [class.positive]="resumo.resultado! > 0" [class.negative]="resumo.resultado! < 0">{{ resumo.resultado === null ? 'Indisponível' : money(resumo.resultado) }}</strong></div><div><small>Rentabilidade estimada</small><strong>{{ resumo.variacao === null ? 'Indisponível' : resumo.variacao.toFixed(2) + '%' }}</strong></div></section>}
          </form>
          @if (!acoes().length) {<p class="notice warning">Nenhum ativo disponível. Cadastre um ativo antes de adicionar uma posição. <a routerLink="/acoes">Cadastrar ativo</a></p>}
          @if (!corretoras().length) {<p class="notice warning">Nenhuma corretora disponível. Cadastre uma corretora antes de adicionar uma posição. <a routerLink="/corretoras">Cadastrar corretora</a></p>}
          @if (!positions().length) {
            <app-empty-state title="Sem posições" detail="Adicione uma posição a esta carteira." />
          } @else {
            <div class="positions-table-wrapper"><table class="positions-table">
              <thead>
                <tr>
                  <th>Ativo</th>
                  <th>Quantidade</th>
                  <th>Preço médio</th>
                  <th>Cotação atual</th>
                  <th>Investido</th>
                  <th>Atual</th>
                  <th>Resultado</th>
                  <th>Rentabilidade</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (position of positions(); track position.id) {
                  <tr>
                    <td class="asset-column"><div class="asset-cell"><app-asset-logo [ticker]="position.ticker" [companyName]="position.nomeEmpresa" [logoUrl]="position.logoUrl" [size]="40" /><span><strong class="asset-ticker">{{ position.ticker.trim().toUpperCase() }}</strong><small class="asset-company-name">{{ position.nomeEmpresa }}</small></span></div></td>
                    <td class="numeric-cell quantity-cell">{{ position.quantidade }}</td>
                    <td class="numeric-cell">{{ money(position.precoMedio, position.moeda) }}</td>
                    <td class="numeric-cell">{{ position.cotacaoAtual === null ? 'Indisponível' : money(position.cotacaoAtual, position.moeda) }}</td>
                    <td class="numeric-cell">{{ money(position.valorInvestido, position.moeda) }}</td>
                    <td class="numeric-cell">{{ position.valorAtual === null ? 'Indisponível' : money(position.valorAtual, position.moeda) }}</td>
                    <td class="numeric-cell result-cell"
                      [class.positive]="position.resultado !== null && position.resultado > 0"
                      [class.negative]="position.resultado !== null && position.resultado < 0"
                      [class.value-positive]="position.resultado !== null && position.resultado > 0"
                      [class.value-negative]="position.resultado !== null && position.resultado < 0"
                      [class.value-neutral]="position.resultado === null || position.resultado === 0"
                    >
                      {{ position.resultado === null ? 'Indisponível' : money(position.resultado, position.moeda) }}
                    </td>
                    <td class="numeric-cell profitability-cell" [class.positive]="position.rentabilidadePercentual !== null && position.rentabilidadePercentual > 0" [class.negative]="position.rentabilidadePercentual !== null && position.rentabilidadePercentual < 0" [class.value-positive]="position.rentabilidadePercentual !== null && position.rentabilidadePercentual > 0" [class.value-negative]="position.rentabilidadePercentual !== null && position.rentabilidadePercentual < 0" [class.value-neutral]="position.rentabilidadePercentual === null || position.rentabilidadePercentual === 0">{{ position.rentabilidadePercentual === null ? 'Indisponível' : position.rentabilidadePercentual.toFixed(2) + '%' }}</td>
                    <td><div class="position-actions">
                      <button class="secondary" (click)="editPosition(position)">Editar</button>
                      <button class="danger" (click)="removePosition(position)">Excluir</button>
                    </div></td>
                  </tr>
                }
              </tbody>
            </table></div>
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
  readonly tipoPreco = signal<TipoPreco>('COTACAO_ATUAL');
  readonly selectedAcaoId = signal<number | null>(null);
  readonly selectedAcao = computed(() => this.acoes().find((acao) => acao.id === this.selectedAcaoId()) ?? null);
  readonly selectedCorretora = computed(() => this.corretoras().find((corretora) => corretora.id === this.positionForm.controls.corretoraId.value) ?? null);
  readonly cotacaoDisponivel = computed(() => (this.selectedAcao()?.cotacaoAtual ?? 0) > 0);
  readonly cotacaoDesatualizada = computed(() => {
    const dataHora = this.selectedAcao()?.dataHoraCotacao;
    return !!dataHora && Date.now() - new Date(dataHora).getTime() > 24 * 60 * 60 * 1000;
  });
  readonly resumoPosicao = computed(() => {
    const quantidade = this.positionForm.controls.quantidade.value;
    const precoMedio = this.positionForm.controls.precoMedio.value;
    const cotacaoAtual = this.selectedAcao()?.cotacaoAtual ?? null;
    if (quantidade === null || quantidade <= 0 || precoMedio === null || precoMedio <= 0) return null;
    const totalInvestido = quantidade * precoMedio;
    if (cotacaoAtual === null || cotacaoAtual <= 0) return { quantidade, precoMedio, cotacaoAtual: null, totalInvestido, valorAtual: null, resultado: null, variacao: null };
    const valorAtual = quantidade * cotacaoAtual;
    const resultado = valorAtual - totalInvestido;
    return { quantidade, precoMedio, cotacaoAtual, totalInvestido, valorAtual, resultado, variacao: totalInvestido > 0 ? (resultado / totalInvestido) * 100 : 0 };
  });
  constructor() {
    this.load();
    this.acoesApi.list(0, 100).subscribe((page) => this.acoes.set(page.content));
    this.corretorasApi.list(0, 100).subscribe((page) => this.corretoras.set(page.content));
    this.positionForm.controls.acaoId.valueChanges.subscribe((acaoId) => {
      this.selectedAcaoId.set(acaoId);
      this.onAcaoChange();
    });
  }
  setTipoPreco(tipoPreco: TipoPreco): void {
    if (tipoPreco === 'COTACAO_ATUAL' && !this.cotacaoDisponivel()) return;
    this.tipoPreco.set(tipoPreco);
    if (tipoPreco === 'COTACAO_ATUAL') this.positionForm.controls.precoMedio.setValue(this.selectedAcao()!.cotacaoAtual!);
    else this.positionForm.controls.precoMedio.reset();
  }
  diferencaPrecoInformado(): number | null {
    const cotacao = this.selectedAcao()?.cotacaoAtual;
    const preco = this.positionForm.controls.precoMedio.value;
    if (this.tipoPreco() !== 'PRECO_INFORMADO' || cotacao === null || cotacao === undefined || cotacao <= 0 || preco === null || preco <= 0) return null;
    return Number((((preco - cotacao) / cotacao) * 100).toFixed(2));
  }
  private onAcaoChange(): void {
    if (this.editingPosition()) return;
    const acao = this.selectedAcao();
    if (!acao) return;
    if ((acao.cotacaoAtual ?? 0) <= 0) {
      this.acoesApi.getById(acao.id).subscribe((atualizada) => {
        this.acoes.update((acoes) => acoes.map((item) => item.id === atualizada.id ? atualizada : item));
        if (this.tipoPreco() === 'COTACAO_ATUAL' && (atualizada.cotacaoAtual ?? 0) > 0) this.positionForm.controls.precoMedio.setValue(atualizada.cotacaoAtual);
      });
    } else if (this.tipoPreco() === 'COTACAO_ATUAL') {
      this.positionForm.controls.precoMedio.setValue(acao.cotacaoAtual);
    } else {
      this.positionForm.controls.precoMedio.reset();
    }
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
    this.tipoPreco.set('PRECO_INFORMADO');
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
  imports: [ReactiveFormsModule, EmptyStateComponent, LoadingComponent, AssetLogoComponent],
  template: ` <section class="page">
    <header class="page-header">
      <div>
        <h1>Histórico de cotação</h1>
        <p class="muted">Pontos retornados pela API, do mais recente ao mais antigo.</p>
      </div>
    </header>
    @if (acao(); as ativo) { <section class="panel asset-history-heading"><app-asset-logo [ticker]="ativo.ticker" [companyName]="ativo.nomeEmpresa" [logoUrl]="ativo.logoUrl" [size]="40" /><div><span class="ticker-badge">{{ ativo.ticker.trim().toUpperCase() }}</span><small>{{ ativo.nomeEmpresa }}</small></div></section> }
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
  readonly acao = signal<Acao | null>(null);
  readonly loading = signal(false);
  readonly form = this.fb.nonNullable.group({ acaoId: [0, Validators.min(1)] });
  readonly max = computed(() => Math.max(...this.items().map((item) => item.valor), 1));
  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('acaoId'));
    if (id > 0) {
      this.form.controls.acaoId.setValue(id);
      this.api.getById(id).subscribe({ next: (acao) => this.acao.set(acao) });
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
            [class.positive]="dashboard.resultado !== null && dashboard.resultado > 0"
            [class.negative]="dashboard.resultado !== null && dashboard.resultado < 0"
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
                  <td [class.positive]="item.resultado !== null && item.resultado > 0" [class.negative]="item.resultado !== null && item.resultado < 0">
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
