import { Routes } from '@angular/router';
import { AcoesPage, CarteirasPage, CorretorasPage, HistoricoPage } from './features/pages';
import { InsightsDashboardPage } from './features/insights-dashboard.page';

export const routes: Routes = [
  { path: 'corretoras', component: CorretorasPage }, { path: 'acoes', component: AcoesPage },
  { path: 'carteiras', component: CarteirasPage }, { path: 'historico/:acaoId', component: HistoricoPage },
  { path: 'dashboard/:carteiraId', component: InsightsDashboardPage }, { path: '', pathMatch: 'full', redirectTo: 'carteiras' },
];
