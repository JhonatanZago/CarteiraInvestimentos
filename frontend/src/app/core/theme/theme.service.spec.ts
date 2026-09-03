import { TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { describe, beforeEach, expect, it, vi } from 'vitest';
import { ThemeService } from './theme.service';

function createService(prefersDark = false): ThemeService {
  Object.defineProperty(window, 'matchMedia', { configurable: true, value: vi.fn(() => ({ matches: prefersDark })) });
  TestBed.configureTestingModule({ providers: [{ provide: PLATFORM_ID, useValue: 'browser' }] });
  return TestBed.inject(ThemeService);
}

describe('ThemeService', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('uses light when there is no saved or system preference', () => {
    expect(createService().theme()).toBe('light');
  });

  it('uses the system dark preference on the first visit', () => {
    expect(createService(true).theme()).toBe('dark');
  });

  it('prioritizes the saved preference over the system preference', () => {
    localStorage.setItem('carteira-investimento-theme', 'light');
    expect(createService(true).theme()).toBe('light');
  });

  it('toggles light to dark, saves it and applies it to html', () => {
    const service = createService();
    service.toggleTheme();
    expect(service.theme()).toBe('dark');
    expect(localStorage.getItem('carteira-investimento-theme')).toBe('dark');
    expect(document.documentElement.dataset['theme']).toBe('dark');
  });

  it('toggles dark to light', () => {
    const service = createService(true);
    service.toggleTheme();
    expect(service.theme()).toBe('light');
    expect(localStorage.getItem('carteira-investimento-theme')).toBe('light');
    expect(document.documentElement.dataset['theme']).toBe('light');
  });

  it('restores the saved choice when the service is recreated', () => {
    const service = createService(true);
    service.toggleTheme();
    TestBed.resetTestingModule();
    expect(createService(true).theme()).toBe('light');
  });
});
