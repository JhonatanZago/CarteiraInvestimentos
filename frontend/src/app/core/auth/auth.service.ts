import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, tap } from 'rxjs';
import { API_BASE_URL } from '../api/api-url.token';
import { AuthUser, LoginRequest, LoginResponse, RegisterRequest } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient); private readonly baseUrl = inject(API_BASE_URL); private readonly router = inject(Router);
  readonly user = signal<AuthUser | null>(null); readonly loading = signal(false); private accessToken: string | null = null; private initialized = false;
  get token(): string | null { return this.accessToken; }
  get isAuthenticated(): boolean { return this.user() !== null && this.accessToken !== null; }
  login(request: LoginRequest): Observable<LoginResponse> { this.loading.set(true); return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`,request,{withCredentials:true}).pipe(tap(r=>{this.accessToken=r.accessToken;this.user.set(r.usuario);}),finalize(()=>this.loading.set(false))); }
  register(request: RegisterRequest): Observable<AuthUser> { return this.http.post<AuthUser>(`${this.baseUrl}/auth/cadastro`,request,{withCredentials:true}); }
  refresh(): Observable<LoginResponse> { return this.http.post<LoginResponse>(`${this.baseUrl}/auth/refresh`,{},{withCredentials:true}).pipe(tap(r=>{this.accessToken=r.accessToken;this.user.set(r.usuario);})); }
  me(): Observable<AuthUser> { return this.http.get<AuthUser>(`${this.baseUrl}/auth/me`,{withCredentials:true}).pipe(tap(u=>this.user.set(u))); }
  restore(): Observable<boolean> { if(this.initialized)return of(this.isAuthenticated); this.initialized=true; return this.refresh().pipe(map(()=>true),catchError(()=>{this.clear();return of(false);})); }
  logout(): Observable<void> { return this.http.post<void>(`${this.baseUrl}/auth/logout`,{},{withCredentials:true}).pipe(catchError(()=>of(void 0)),finalize(()=>this.clearLocalSession())); }
  clearLocalSession(): void { this.accessToken=null; this.user.set(null); }
  clear(): void { this.clearLocalSession(); }
}
