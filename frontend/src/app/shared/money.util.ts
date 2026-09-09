export type CurrencyCode = 'BRL' | 'USD' | 'EUR' | 'GBP' | 'CAD' | 'JPY';

const SUPPORTED: ReadonlySet<string> = new Set(['BRL', 'USD', 'EUR', 'GBP', 'CAD', 'JPY']);

/** Formata valores sem assumir BRL quando a moeda não veio da API. */
export function formatMoney(value: number | null | undefined, currency: string | null = 'BRL'): string {
  if (value === null || value === undefined || !Number.isFinite(value)) return 'Indisponível';
  const code = currency?.trim().toUpperCase();
  if (!code || !SUPPORTED.has(code)) {
    return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  }
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: code, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
}
