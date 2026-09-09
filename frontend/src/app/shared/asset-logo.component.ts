import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { safeLogoUrl } from './logo-url.util';

const CURATED_ASSET_LOGOS: Record<string, string> = {
  PETR4: '/assets/assets/petr4.svg',
  VALE3: '/assets/assets/vale3.svg',
  ITUB4: '/assets/assets/itub4.svg',
  AAPL: '/assets/assets/aapl.svg',
  AERI3: '/assets/assets/aeri3.svg',
  BRST3: '/assets/assets/brst3.svg',
};

@Component({
  selector: 'app-asset-logo',
  standalone: true,
  templateUrl: './asset-logo.component.html',
  styleUrl: './asset-logo.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetLogoComponent {
  readonly ticker = input.required<string>();
  readonly companyName = input('');
  readonly logoUrl = input<string | null | undefined>(null);
  readonly size = input(40);
  private readonly failedUrl = signal<string | null>(null);
  readonly normalizedTicker = computed(() => this.ticker().trim().toUpperCase());
  readonly initials = computed(() => this.normalizedTicker().replace(/[^A-Z0-9]/g, '').slice(0, 4) || 'AT');
  readonly background = computed(() => {
    const hash = [...this.normalizedTicker()].reduce((value, character) => (value * 31 + character.charCodeAt(0)) >>> 0, 0);
    return `hsl(${hash % 360} 48% 34%)`;
  });
  readonly validLogoUrl = computed(() => {
    const value = this.logoUrl()?.trim();
    const candidate = value || CURATED_ASSET_LOGOS[this.normalizedTicker()];
    if (!candidate) return null;
    return safeLogoUrl(candidate);
  });
  readonly showImage = computed(() => {
    const url = this.validLogoUrl();
    return !!url && this.failedUrl() !== url;
  });
  handleImageError(): void { this.failedUrl.set(this.validLogoUrl()); }
}
