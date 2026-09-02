import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { apiErrorMessage, toApiClientError } from './api-error';
import { inject } from '@angular/core';
import { NotificationService } from '../feedback/notification.service';
import { SILENT_HTTP_ERROR } from './silent-http-error';
import { environment } from '../../../environments/environment';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(NotificationService);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (!environment.production) console.error('Erro da API', { status: error.status, url: error.url, body: error.error });
        const mapped = toApiClientError(error);
        if (!request.context.get(SILENT_HTTP_ERROR)) {
          notifications.show(apiErrorMessage(mapped.code), 'error');
        }
        return throwError(() => mapped);
      }
      return throwError(() => error);
    }),
  );
};
