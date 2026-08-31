import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it } from 'vitest';
import { PositionTablePage } from './position-table.page';
import { AcoesApiService, CarteirasApiService, CorretorasApiService } from '../core/api/api.services';

describe('PositionTablePage', () => {
  it('loads named action and broker options', async () => {
    await TestBed.configureTestingModule({ imports: [PositionTablePage], providers: [
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' } } } },
      { provide: AcoesApiService, useValue: { list: () => of({ content: [{ id: 2, ticker: 'PETR4', nomeEmpresa: 'Petrobras' }] }) } },
      { provide: CorretorasApiService, useValue: { list: () => of({ content: [{ id: 3, razaoSocial: 'Corretora Teste' }] }) } },
      { provide: CarteirasApiService, useValue: { listPositionsPage: () => of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }) } },
    ] }).compileComponents();
    const page = TestBed.createComponent(PositionTablePage).componentInstance;
    expect(page.valid()).toBe(false);
    expect(page.acoes().length).toBe(1);
    expect(page.corretoras().length).toBe(1);
  });
});
