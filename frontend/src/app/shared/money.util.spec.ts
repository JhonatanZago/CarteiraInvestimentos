import { describe, expect, it } from 'vitest';
import { formatMoney } from './money.util';

describe('formatMoney', () => {
  it('formats BRL and USD from the explicit currency code', () => {
    expect(formatMoney(191.32, 'BRL')).toContain('R$');
    expect(formatMoney(191.32, 'USD')).toContain('US$');
  });

  it('does not invent a currency when the code is absent', () => {
    expect(formatMoney(191.32, null)).not.toContain('R$');
    expect(formatMoney(null, 'USD')).toBe('Indisponível');
  });
});
