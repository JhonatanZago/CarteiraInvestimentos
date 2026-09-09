import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { BrokerLogoComponent } from './broker-logo.component';

describe('BrokerLogoComponent', () => {
  it('uses exact CNPJ registry matching and lazy image loading', async () => {
    await TestBed.configureTestingModule({ imports: [BrokerLogoComponent] }).compileComponents();
    const fixture = TestBed.createComponent(BrokerLogoComponent);
    fixture.componentRef.setInput('cnpj', '02.332.886/0001-04');
    fixture.componentRef.setInput('name', 'XP Investimentos');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')?.getAttribute('src')).toContain('xp-investimentos.svg');
    expect(fixture.nativeElement.querySelector('img')?.getAttribute('loading')).toBe('lazy');
  });

  it('falls back without rendering a broken image for an unsafe URL', async () => {
    await TestBed.configureTestingModule({ imports: [BrokerLogoComponent] }).compileComponents();
    const fixture = TestBed.createComponent(BrokerLogoComponent);
    fixture.componentRef.setInput('cnpj', '99.999.999/0001-99');
    fixture.componentRef.setInput('name', 'Corretora Exemplo');
    fixture.componentRef.setInput('logoUrl', 'http://localhost/logo.png');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.broker-logo-fallback')).not.toBeNull();
  });
});
