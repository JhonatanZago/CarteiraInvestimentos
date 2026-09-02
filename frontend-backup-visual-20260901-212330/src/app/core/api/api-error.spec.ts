import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { apiErrorMessage, toApiClientError } from './api-error';

describe('toApiClientError', () => {
  it('maps the stable backend code without relying on the provider message', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { code: 'DUPLICATE_RESOURCE', message: 'Texto instavel do servidor', fieldErrors: [] },
    });

    const mapped = toApiClientError(error);

    expect(mapped.code).toBe('DUPLICATE_RESOURCE');
    expect(mapped.status).toBe(409);
  });

  it('presents a stable message from the backend code', () => {
    expect(apiErrorMessage('EXTERNAL_INTEGRATION_ERROR')).toContain('indisponível');
  });
});
