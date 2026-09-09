import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { safeLogoUrl } from './logo-url.util';

const CURATED_BROKER_LOGOS: Record<string, string> = {
  // Marcas oficiais distribuídas pelo Simple Icons (SVG, sem tokens ou tracking).
  '02332886000104': '/assets/brokers/xp-investimentos.svg',
  '30306294000145': '/assets/brokers/btg-pactual.svg',
};

@Component({
  selector: 'app-broker-logo',
  standalone: true,
  templateUrl: './broker-logo.component.html',
  styleUrl: './broker-logo.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrokerLogoComponent {
  readonly cnpj = input('');
  readonly name = input('');
  readonly logoUrl = input<string | null | undefined>(null);
  readonly size = input(44);
  private readonly failedUrl = signal<string | null>(null);
  readonly normalizedCnpj = computed(() => this.cnpj().replace(/\D/g, ''));
  readonly displayName = computed(() => this.name().trim() || 'Corretora');
  readonly initials = computed(() => this.displayName().split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase() || 'CO');
  readonly fallbackBackground = computed(() => {
    const hash = [...this.normalizedCnpj() || this.displayName()].reduce((value, character) => (value * 31 + character.charCodeAt(0)) >>> 0, 0);
    return `hsl(${hash % 360} 48% 34%)`;
  });
  readonly validLogoUrl = computed(() => {
    const candidate = this.logoUrl()?.trim() || CURATED_BROKER_LOGOS[this.normalizedCnpj()];
    if (!candidate) return null;
    return safeLogoUrl(candidate);
  });
  readonly showImage = computed(() => {
    const url = this.validLogoUrl();
    return !!url && this.failedUrl() !== url;
  });
  handleImageError(): void { this.failedUrl.set(this.validLogoUrl()); }
}
