import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { ThemeService } from './core/theme/theme.service';
import { App } from './app';

describe('App', () => {
  it('shows the accessible action for the next theme and toggles it without HTTP', async () => {
    const isDark = signal(false);
    const toggleTheme = vi.fn();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: ThemeService, useValue: { isDark, toggleTheme } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const button = host.querySelector<HTMLButtonElement>('.theme-toggle');

    expect(button?.getAttribute('aria-label')).toBe('Ativar tema escuro');
    button?.click();
    expect(toggleTheme).toHaveBeenCalledOnce();

    isDark.set(true);
    fixture.detectChanges();
    expect(button?.getAttribute('aria-label')).toBe('Ativar tema claro');
  });
});
