import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { CarteirasApiService } from './api/api.services';
import { PortfolioContextService } from './portfolio-context.service';

describe('PortfolioContextService', () => {
  it('restores only an available portfolio and falls back to the first', () => {
    localStorage.setItem('carteira-selecionada', '99');
    TestBed.configureTestingModule({ providers: [PortfolioContextService, { provide: CarteirasApiService, useValue: { list: () => of({ content: [{ id: 4, nome: 'Principal', dataCriacao: '2026-01-01' }] }) } }] });
    const service = TestBed.inject(PortfolioContextService);
    service.load();
    expect(service.selectedId()).toBe(4);
    expect(localStorage.getItem('carteira-selecionada')).toBe('4');
  });
});
