import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { CollectionControlsComponent, ConfirmDialogComponent, EmptyStateComponent, LoadingComponent, PaginationComponent } from './feedback.component';

describe('shared feedback components', () => {
  it('renders loading and empty states', async () => {
    await TestBed.configureTestingModule({ imports: [LoadingComponent, EmptyStateComponent] }).compileComponents();
    const loading = TestBed.createComponent(LoadingComponent); loading.detectChanges();
    expect(loading.nativeElement.textContent).toContain('Carregando');
    const empty = TestBed.createComponent(EmptyStateComponent); empty.componentRef.setInput('title', 'Sem dados'); empty.detectChanges();
    expect(empty.nativeElement.textContent).toContain('Sem dados');
  });

  it('emits the confirmation action', async () => {
    await TestBed.configureTestingModule({ imports: [ConfirmDialogComponent] }).compileComponents();
    const fixture = TestBed.createComponent(ConfirmDialogComponent); fixture.componentRef.setInput('open', true); let confirmed = false;
    fixture.componentInstance.confirmed.subscribe(() => confirmed = true); fixture.detectChanges();
    (fixture.nativeElement.querySelector('.danger') as HTMLButtonElement).click();
    expect(confirmed).toBe(true);
  });

  it('emits the requested page', async () => {
    await TestBed.configureTestingModule({ imports: [PaginationComponent] }).compileComponents();
    const fixture = TestBed.createComponent(PaginationComponent); fixture.componentRef.setInput('totalPages', 2); let page = -1;
    fixture.componentInstance.changed.subscribe(value => page = value); fixture.detectChanges();
    (fixture.nativeElement.querySelectorAll('button')[1] as HTMLButtonElement).click(); expect(page).toBe(1);
  });

  it('emits collection search terms', async () => {
    await TestBed.configureTestingModule({ imports: [CollectionControlsComponent] }).compileComponents();
    const fixture = TestBed.createComponent(CollectionControlsComponent); let query = '';
    fixture.componentInstance.search.subscribe(value => query = value); fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement; input.value = 'PETR'; input.dispatchEvent(new Event('input'));
    expect(query).toBe('PETR');
  });
});
