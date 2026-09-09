import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { API_BASE_URL } from './api-url.token';
import { AcoesApiService, CarteirasApiService, CorretorasApiService } from './api.services';

describe('typed API services', () => {
  const configure = () => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '/api/v1' }] });
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('preserves pagination parameters for stocks and brokers', () => {
    configure(); const http = TestBed.inject(HttpTestingController);
    TestBed.inject(AcoesApiService).list(2, 10).subscribe();
    const stocks = http.expectOne('/api/v1/acoes?page=2&size=10'); expect(stocks.request.method).toBe('GET'); stocks.flush({ content: [] });
    TestBed.inject(CorretorasApiService).list(1, 5).subscribe();
    const brokers = http.expectOne('/api/v1/corretoras?page=1&size=5'); expect(brokers.request.method).toBe('GET'); brokers.flush({ content: [] });
  });

  it('uses the portfolio position endpoints with typed methods', () => {
    configure(); const http = TestBed.inject(HttpTestingController); const api = TestBed.inject(CarteirasApiService);
    api.createPosition(4, { acaoId: 2, corretoraId: 3, quantidade: 1, precoMedio: 10, dataPrimeiraCompra: '2026-08-01' }).subscribe();
    const position = http.expectOne('/api/v1/carteiras/4/posicoes'); expect(position.request.method).toBe('POST'); position.flush({});
  });

  it('preserves validated market, currency and logo metadata from the asset response', () => {
    configure(); const http = TestBed.inject(HttpTestingController); const api = TestBed.inject(AcoesApiService);
    let received: any;
    api.getById(3).subscribe(value => received = value);
    const request = http.expectOne('/api/v1/acoes/3');
    expect(request.request.method).toBe('GET');
    request.flush({
      id: 3, ticker: 'AAPL', nomeEmpresa: 'Apple Inc.', mercado: 'EUA', moeda: 'USD',
      cotacaoAtual: 191.32, dataHoraCotacao: '2026-09-06T12:00:00Z', logoUrl: 'https://example.com/aapl.png',
      listingCountryCode: 'US', exchange: 'NASDAQ', exchangeMic: 'XNAS', dataSource: 'provider',
    });
    expect(received.moeda).toBe('USD');
    expect(received.listingCountryCode).toBe('US');
    expect(received.exchange).toBe('NASDAQ');
    expect(received.exchangeMic).toBe('XNAS');
    expect(received.logoUrl).toBe('https://example.com/aapl.png');
  });
});
