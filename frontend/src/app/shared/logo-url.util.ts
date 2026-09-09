/**
 * Aceita apenas assets locais da aplicação ou imagens HTTPS públicas.
 * URLs de logo são dados externos e nunca devem permitir credenciais,
 * esquemas executáveis ou hosts locais/privados.
 */
export function safeLogoUrl(value: string | null | undefined): string | null {
  const candidate = value?.trim();
  if (!candidate) return null;
  if (candidate.startsWith('/assets/')) return candidate;

  try {
    const url = new URL(candidate);
    if (url.protocol !== 'https:' || url.username || url.password) return null;
    const host = url.hostname.toLowerCase();
    if (host === 'localhost' || host.endsWith('.local') || host === '::1' || host === '[::1]') return null;
    if (host === '0.0.0.0' || host === '::' || host === '[::]') return null;
    const ipv4 = host.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
    if (ipv4) {
      const octets = ipv4.slice(1).map(Number);
      if (octets.some(part => part > 255) || octets[0] === 10 || octets[0] === 127 ||
          (octets[0] === 172 && octets[1] >= 16 && octets[1] <= 31) ||
          (octets[0] === 192 && octets[1] === 168)) return null;
    }
    for (const key of url.searchParams.keys()) {
      if (/(^|_)(token|api[_-]?key|access[_-]?token|secret|password|authorization)(_|$)/i.test(key)) return null;
    }
    return url.toString();
  } catch {
    return null;
  }
}
