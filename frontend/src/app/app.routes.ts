import { Routes } from '@angular/router';
import { AcoesPage, CarteirasPage, CorretorasPage, HistoricoPage } from './features/pages';
import { InsightsDashboardPage } from './features/insights-dashboard.page';
import { PositionTablePage } from './features/position-table.page';
import { VendasPage } from './features/vendas.page';

export const routes: Routes = [
  { path: 'corretoras', component: CorretorasPage }, { path: 'acoes', component: AcoesPage },
  { path: 'carteiras', component: CarteirasPage }, { path: 'historico/:acaoId', component: HistoricoPage },
  { path: 'carteiras/:carteiraId/posicoes', component: PositionTablePage },
  { path: 'vendas', component: VendasPage }, { path: 'dashboard', component: InsightsDashboardPage }, { path: 'dashboard/:carteiraId', component: InsightsDashboardPage }, { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
];
