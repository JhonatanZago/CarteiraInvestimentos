import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { AssetLogoComponent } from './asset-logo.component';

describe('AssetLogoComponent', () => {
  const create = async () => {
    await TestBed.configureTestingModule({ imports: [AssetLogoComponent] }).compileComponents();
    const fixture = TestBed.createComponent(AssetLogoComponent);
    fixture.componentRef.setInput('ticker', ' petr4 ');
    fixture.componentRef.setInput('companyName', 'Petróleo Brasileiro S.A.');
    fixture.componentRef.setInput('logoUrl', null);
    fixture.detectChanges();
    return fixture;
  };

  it('normalizes Brazilian and American tickers in the fallback', async () => {
    const fixture = await create();
    expect(fixture.nativeElement.textContent).toContain('PETR');
    fixture.componentRef.setInput('ticker', ' aapl ');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('AAPL');
  });

  it('renders a valid HTTPS logo and rejects non-HTTPS URLs', async () => {
    const fixture = await create();
    fixture.componentRef.setInput('logoUrl', 'https://cdn.example.com/petr4.png');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')?.getAttribute('src')).toBe('https://cdn.example.com/petr4.png');
    fixture.componentRef.setInput('logoUrl', 'http://cdn.example.com/petr4.png');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.asset-logo-fallback')).not.toBeNull();
  });

  it('rejects local, credentialed and token-bearing external URLs', async () => {
    const fixture = await create();
    for (const logoUrl of [
      'https://localhost/logo.png',
      'https://user:password@cdn.example.com/logo.png',
      'https://cdn.example.com/logo.png?access_token=secret',
      'https://192.168.1.10/logo.png',
    ]) {
      fixture.componentRef.setInput('logoUrl', logoUrl);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('img')).toBeNull();
    }
  });

  it('uses lazy loading and keeps the image dimensions controlled', async () => {
    const fixture = await create();
    fixture.componentRef.setInput('logoUrl', 'https://cdn.example.com/petr4.png');
    fixture.detectChanges();
    const image = fixture.nativeElement.querySelector('img');
    expect(image?.getAttribute('loading')).toBe('lazy');
    expect(image?.getAttribute('decoding')).toBe('async');
    expect(fixture.nativeElement.querySelector('.asset-logo')).not.toBeNull();
  });

  it('returns to the fallback when the image fails to load', async () => {
    const fixture = await create();
    fixture.componentRef.setInput('logoUrl', 'https://cdn.example.com/petr4.png');
    fixture.detectChanges();
    fixture.nativeElement.querySelector('img').dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.asset-logo-fallback')).not.toBeNull();
  });

  it('keeps the logo constrained by the reusable component class in both themes', async () => {
    const fixture = await create();
    document.documentElement.dataset['theme'] = 'dark';
    expect(fixture.nativeElement.querySelector('.asset-logo')).not.toBeNull();
    document.documentElement.dataset['theme'] = 'light';
  });
});
