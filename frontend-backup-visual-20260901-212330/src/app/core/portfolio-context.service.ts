import { Injectable, inject, signal } from '@angular/core';
import { Carteira } from './api/api.models';
import { CarteirasApiService } from './api/api.services';
import { catchError, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PortfolioContextService {
  private readonly api = inject(CarteirasApiService);
  private readonly storageKey = 'carteira-selecionada';
  readonly portfolios = signal<Carteira[]>([]);
  readonly selectedId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly unavailable = signal(false);

  load(): void {
    this.loading.set(true);
    this.api.list(0, 100).pipe(catchError(() => { this.unavailable.set(true); return of({ content: [] } as { content: Carteira[] }); })).subscribe({
      next: page => {
        this.portfolios.set(page.content);
        const saved = Number(localStorage.getItem(this.storageKey));
        const selected = page.content.find(item => item.id === saved) ?? page.content[0] ?? null;
        this.selectedId.set(selected?.id ?? null);
        if (selected) localStorage.setItem(this.storageKey, String(selected.id));
      }, complete: () => this.loading.set(false), error: () => this.loading.set(false),
    });
  }

  select(id: number): void {
    if (!this.portfolios().some(item => item.id === id)) return;
    this.selectedId.set(id);
    localStorage.setItem(this.storageKey, String(id));
  }
}
