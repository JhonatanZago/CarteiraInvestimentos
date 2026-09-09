import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `<section class="empty-state"><strong>{{ title() }}</strong><p>{{ detail() }}</p></section>`,
})
export class EmptyStateComponent { readonly title = input.required<string>(); readonly detail = input('Nenhum registro encontrado.'); }

@Component({ selector: 'app-loading', standalone: true, template: `<p class="loading" role="status">Carregando…</p>` })
export class LoadingComponent {}

@Component({
  selector: 'app-confirm-dialog', standalone: true,
  template: `@if (open()) { <section class="dialog-backdrop" role="presentation"><div class="dialog" role="alertdialog" aria-modal="true"><h3>{{ title() }}</h3><p>{{ detail() }}</p><button type="button" (click)="cancelled.emit()">Cancelar</button><button type="button" class="danger" (click)="confirmed.emit()">{{ confirmLabel() }}</button></div></section> }`,
})
export class ConfirmDialogComponent {
  readonly open = input(false); readonly title = input('Confirmar ação'); readonly detail = input('Esta ação não pode ser desfeita.');
  readonly confirmLabel = input('Confirmar'); readonly confirmed = output<void>(); readonly cancelled = output<void>();
}

@Component({
  selector: 'app-pagination', standalone: true,
  template: `<nav class="pagination" aria-label="Paginação"><button type="button" class="secondary" [disabled]="page() === 0" (click)="changed.emit(page() - 1)">Anterior</button><span>Página {{ page() + 1 }} de {{ totalPages() || 1 }}</span><button type="button" class="secondary" [disabled]="page() + 1 >= totalPages()" (click)="changed.emit(page() + 1)">Próxima</button></nav>`,
})
export class PaginationComponent { readonly page = input(0); readonly totalPages = input(0); readonly changed = output<number>(); }

@Component({ selector: 'app-collection-controls', standalone: true, template: `<div class="collection-controls"><input [value]="query()" (input)="search.emit($any($event.target).value)" placeholder="Pesquisar" aria-label="Pesquisar coleção"><select (change)="sort.emit($any($event.target).value)" aria-label="Ordenar"><option value="name">Nome</option><option value="ticker">Ticker</option></select></div>` })
export class CollectionControlsComponent { readonly query = input(''); readonly search = output<string>(); readonly sort = output<string>(); }
