import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService); const isAuthEndpoint = req.url.includes('/auth/login') || req.url.includes('/auth/refresh') || req.url.includes('/auth/cadastro');
  const request = auth.token && !isAuthEndpoint ? req.clone({ setHeaders: { Authorization: `Bearer ${auth.token}` }, withCredentials: true }) : req.clone({ withCredentials: true });
  return next(request).pipe(catchError(error => error.status === 401 && !isAuthEndpoint ? auth.refresh().pipe(switchMap(()=>next(req.clone({setHeaders:{Authorization:`Bearer ${auth.token}`},withCredentials:true}))),catchError(refreshError=>{auth.clear();return throwError(()=>refreshError);})):throwError(()=>error)));
};
