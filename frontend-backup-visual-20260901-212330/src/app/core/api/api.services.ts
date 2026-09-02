import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url.token';
import {
  Acao, AcaoCreateRequest, Carteira, CarteiraRequest, Corretora, CorretoraCreateRequest,
  DashboardCarteira, HistoricoCotacao, IncomeSummary, MarketIndicator, PageResponse, PortfolioEvolutionPoint, Posicao, PosicaoRequest,
} from './api.models';

function pageParams(page = 0, size = 20): { params: HttpParams } {
  return { params: new HttpParams().set('page', page).set('size', size) };
}

@Injectable({ providedIn: 'root' })
export class CorretorasApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  list(page?: number, size?: number): Observable<PageResponse<Corretora>> {
    return this.http.get<PageResponse<Corretora>>(`${this.baseUrl}/corretoras`, pageParams(page, size));
  }
  create(request: CorretoraCreateRequest): Observable<Corretora> {
    return this.http.post<Corretora>(`${this.baseUrl}/corretoras`, request);
  }
  getById(id: number): Observable<Corretora> { return this.http.get<Corretora>(`${this.baseUrl}/corretoras/${id}`); }
  getByCnpj(cnpj: string): Observable<Corretora> { return this.http.get<Corretora>(`${this.baseUrl}/corretoras/cnpj/${cnpj}`); }
}

@Injectable({ providedIn: 'root' })
export class AcoesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  list(page?: number, size?: number): Observable<PageResponse<Acao>> {
    return this.http.get<PageResponse<Acao>>(`${this.baseUrl}/acoes`, pageParams(page, size));
  }
  create(request: AcaoCreateRequest): Observable<Acao> { return this.http.post<Acao>(`${this.baseUrl}/acoes`, request); }
  getById(id: number): Observable<Acao> { return this.http.get<Acao>(`${this.baseUrl}/acoes/${id}`); }
  getByTicker(ticker: string): Observable<Acao> { return this.http.get<Acao>(`${this.baseUrl}/acoes/ticker/${ticker}`); }
  refresh(id: number): Observable<Acao> { return this.http.post<Acao>(`${this.baseUrl}/acoes/${id}/atualizar-cotacao`, {}); }
  history(id: number): Observable<HistoricoCotacao[]> { return this.http.get<HistoricoCotacao[]>(`${this.baseUrl}/acoes/${id}/historico`); }
}

@Injectable({ providedIn: 'root' })
export class CarteirasApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  list(page?: number, size?: number): Observable<PageResponse<Carteira>> {
    return this.http.get<PageResponse<Carteira>>(`${this.baseUrl}/carteiras`, pageParams(page, size));
  }
  create(request: CarteiraRequest): Observable<Carteira> { return this.http.post<Carteira>(`${this.baseUrl}/carteiras`, request); }
  getById(id: number): Observable<Carteira> { return this.http.get<Carteira>(`${this.baseUrl}/carteiras/${id}`); }
  update(id: number, request: CarteiraRequest): Observable<Carteira> { return this.http.put<Carteira>(`${this.baseUrl}/carteiras/${id}`, request); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/carteiras/${id}`); }
  listPositions(carteiraId: number, filters: { mercado?: string; classificacao?: string; busca?: string; ordenarPor?: string } = {}): Observable<Posicao[]> {
    let params = new HttpParams(); Object.entries(filters).forEach(([key, value]) => { if (value) params = params.set(key, value); });
    return this.http.get<Posicao[]>(`${this.baseUrl}/carteiras/${carteiraId}/posicoes`, { params });
  }
  listPositionsPage(carteiraId: number, page = 0, size = 20, filters: { mercado?: string; classificacao?: string; busca?: string; ordenarPor?: string } = {}): Observable<PageResponse<Posicao>> {
    let params = new HttpParams().set('page', page).set('size', size); Object.entries(filters).forEach(([key, value]) => { if (value) params = params.set(key, value); });
    return this.http.get<PageResponse<Posicao>>(`${this.baseUrl}/carteiras/${carteiraId}/posicoes/paginadas`, { params });
  }
  createPosition(carteiraId: number, request: PosicaoRequest): Observable<Posicao> { return this.http.post<Posicao>(`${this.baseUrl}/carteiras/${carteiraId}/posicoes`, request); }
  updatePosition(carteiraId: number, posicaoId: number, request: PosicaoRequest): Observable<Posicao> { return this.http.put<Posicao>(`${this.baseUrl}/carteiras/${carteiraId}/posicoes/${posicaoId}`, request); }
  deletePosition(carteiraId: number, posicaoId: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/carteiras/${carteiraId}/posicoes/${posicaoId}`); }
}

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  getPortfolio(carteiraId: number, context?: HttpContext): Observable<DashboardCarteira> {
    return this.http.get<DashboardCarteira>(`${this.baseUrl}/dashboard/carteiras/${carteiraId}`, context ? { context } : {});
  }
}

@Injectable({ providedIn: 'root' })
export class InsightsApiService {
  private readonly http = inject(HttpClient); private readonly baseUrl = inject(API_BASE_URL);
  indicators(context?: HttpContext): Observable<MarketIndicator[]> { return this.http.get<MarketIndicator[]>(`${this.baseUrl}/mercado/indicadores`, context ? { context } : {}); }
  evolution(carteiraId: number, context?: HttpContext): Observable<PortfolioEvolutionPoint[]> { return this.http.get<PortfolioEvolutionPoint[]>(`${this.baseUrl}/carteiras/${carteiraId}/evolucao`, context ? { context } : {}); }
  income(carteiraId: number, context?: HttpContext): Observable<IncomeSummary> { return this.http.get<IncomeSummary>(`${this.baseUrl}/carteiras/${carteiraId}/proventos`, context ? { context } : {}); }
}
