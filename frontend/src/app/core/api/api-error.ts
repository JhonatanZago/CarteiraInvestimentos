import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorResponse } from './api.models';

export class ApiClientError extends Error {
  constructor(
    readonly code: string,
    readonly status: number,
    readonly fieldErrors: ApiErrorResponse['fieldErrors'] = [],
  ) {
    super(code);
  }
}

const messages: Record<string, string> = {
  VALIDATION_ERROR: 'Revise os dados informados.',
  DUPLICATE_RESOURCE: 'Este registro já existe.',
  BUSINESS_RULE_VIOLATION: 'A operação não atende às regras de negócio.',
  RESOURCE_NOT_FOUND: 'O registro solicitado não foi encontrado.',
  EXTERNAL_INTEGRATION_ERROR: 'O serviço de consulta está indisponível. Tente novamente.',
  UNEXPECTED_ERROR: 'Não foi possível concluir a operação.',
};

export function apiErrorMessage(code: string): string { return messages[code] ?? messages['UNEXPECTED_ERROR']; }

export function toApiClientError(error: HttpErrorResponse): ApiClientError {
  const body = error.error as Partial<ApiErrorResponse> | null;
  return new ApiClientError(body?.code ?? 'UNEXPECTED_ERROR', error.status, body?.fieldErrors ?? []);
}
