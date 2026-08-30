import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { apiErrorMessage, toApiClientError } from './api-error';
import { inject } from '@angular/core';
import { NotificationService } from '../feedback/notification.service';
import { SILENT_HTTP_ERROR } from './silent-http-error';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(NotificationService);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
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
