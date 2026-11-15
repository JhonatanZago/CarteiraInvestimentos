import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { API_BASE_URL } from '../api/api-url.token';
import { AuthService } from './auth.service';

describe('AuthService security boundary', () => {
  const configure = () => TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: API_BASE_URL, useValue: '/api/v1' }],
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('uses credentialed requests and keeps refresh tokens out of browser storage', () => {
    configure();
    const service = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    let response: unknown;

    service.login({ email: 'user@example.com', senha: 'Senha123!', manterConectado: false }).subscribe(value => response = value);
    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual({ email: 'user@example.com', senha: 'Senha123!', manterConectado: false });
    request.flush({ accessToken: 'access-token-test', expiresInSeconds: 900, usuario: { id: 1, nome: 'User', email: 'user@example.com', perfil: 'USER' } });

    expect(response).toBeTruthy();
    expect(service.token).toBe('access-token-test');
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(sessionStorage.getItem('refresh_token')).toBeNull();
  });

  it('clears the local session after logout even when the server rejects it', () => {
    configure();
    const service = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    service.clearLocalSession();
    service.login({ email: 'user@example.com', senha: 'Senha123!', manterConectado: false }).subscribe();
    http.expectOne('/api/v1/auth/login').flush({ accessToken: 'access-token-test', expiresInSeconds: 900, usuario: { id: 1, nome: 'User', email: 'user@example.com', perfil: 'USER' } });

    service.logout().subscribe();
    const request = http.expectOne('/api/v1/auth/logout');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    expect(service.token).toBeNull();
    expect(service.user()).toBeNull();
  });

  it('refreshes through the HttpOnly cookie boundary without browser storage', () => {
    configure();
    const service = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);

    service.refresh().subscribe();
    const request = http.expectOne('/api/v1/auth/refresh');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ accessToken: 'refreshed-access-token', expiresInSeconds: 900, usuario: { id: 1, nome: 'User', email: 'user@example.com', perfil: 'USER' } });

    expect(service.token).toBe('refreshed-access-token');
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(sessionStorage.getItem('refresh_token')).toBeNull();
  });

  it('clears the session when restore refresh returns 401', () => {
    configure();
    const service = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    let restored: boolean | undefined;
    service.restore().subscribe(value => restored = value);

    const request = http.expectOne('/api/v1/auth/refresh');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    expect(restored).toBe(false);
    expect(service.token).toBeNull();
    expect(service.user()).toBeNull();
  });
});
