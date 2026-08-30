import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CarteirasApiService } from '../core/api/api.services';
import { PageResponse, Posicao } from '../core/api/api.models';
import { EmptyStateComponent, LoadingComponent } from '../shared/feedback.component';

const money = (value: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

@Component({ standalone: true, imports: [FormsModule, RouterLink, EmptyStateComponent, LoadingComponent], template: `
<section class="page"><header class="page-header"><div><h1>Posições</h1><p class="muted">Valores e classificações calculados pelo backend.</p></div><a routerLink="/carteiras">Voltar</a></header>
<section class="panel"><form (ngSubmit)="load(0)"><input [(ngModel)]="busca" name="busca" aria-label="Buscar ticker" placeholder="Buscar ticker"><select [(ngModel)]="mercado" name="mercado" aria-label="Filtrar mercado"><option value="">Todos os mercados</option><option value="BRASIL">Brasil</option><option value="EUA">Estados Unidos</option></select><select [(ngModel)]="classificacao" name="classificacao" aria-label="Filtrar classe"><option value="">Todas as classes</option><option value="ACOES_BRASIL">Ações Brasil</option><option value="ACOES_EXTERIOR">Ações exterior</option></select><select [(ngModel)]="ordenarPor" name="ordenarPor" aria-label="Ordenar posições"><option value="ticker">Ticker</option><option value="valorAtual">Valor atual</option><option value="resultado">Resultado</option><option value="rentabilidade">Rentabilidade</option></select><button>Aplicar</button></form></section>
@if (loading()) { <app-loading/> } @else if (!result().content.length) { <app-empty-state title="Nenhuma posição encontrada" detail="Altere os filtros ou cadastre uma posição."/> } @else { <section class="panel"><table><thead><tr><th>Ativo</th><th>Mercado</th><th>Classe</th><th>Atual</th><th>Resultado</th><th>Atualização</th></tr></thead><tbody>@for (item of result().content; track item.id) { <tr><td>{{ item.ticker }}</td><td>{{ item.mercado }}</td><td>{{ item.classificacaoAlocacao }}</td><td>{{ money(item.valorAtual) }}</td><td [class.positive]="item.resultado >= 0" [class.negative]="item.resultado < 0">{{ money(item.resultado) }}</td><td>{{ item.dataHoraCotacao ?? 'Indisponível' }}</td></tr>}</tbody></table><p><button class="secondary" [disabled]="result().page === 0" (click)="load(result().page - 1)">Anterior</button> Página {{ result().page + 1 }} de {{ result().totalPages || 1 }} <button class="secondary" [disabled]="result().page + 1 >= result().totalPages" (click)="load(result().page + 1)">Próxima</button></p></section> }</section>` })
export class PositionTablePage {
  private readonly api = inject(CarteirasApiService); private readonly route = inject(ActivatedRoute); private readonly id = Number(this.route.snapshot.paramMap.get('carteiraId'));
  readonly money = money; readonly loading = signal(true); readonly result = signal<PageResponse<Posicao>>({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }); busca = ''; mercado = ''; classificacao = ''; ordenarPor = 'ticker';
  constructor() { this.load(0); }
  load(page: number): void { if (!this.id) { this.loading.set(false); return; } this.loading.set(true); this.api.listPositionsPage(this.id, page, 20, { busca: this.busca, mercado: this.mercado, classificacao: this.classificacao, ordenarPor: this.ordenarPor }).subscribe({ next: result => { this.result.set(result); this.loading.set(false); }, error: () => this.loading.set(false) }); }
}
