import { Routes } from '@angular/router';
import { AcoesPage, CarteirasPage, CorretorasPage, HistoricoPage } from './features/pages';
import { InsightsDashboardPage } from './features/insights-dashboard.page';
import { PositionTablePage } from './features/position-table.page';
import { VendasPage } from './features/vendas.page';
import { LoginPage } from './features/auth/login/login.page';
import { RegisterPage } from './features/auth/register/register.page';
import { ForgotPasswordPage } from './features/auth/forgot-password.page';
import { ResetPasswordPage } from './features/auth/reset-password.page';
import { authGuard, guestGuard } from './core/auth/auth.guards';
import { AuthLayoutComponent } from './layouts/auth-layout.component';
import { AppLayoutComponent } from './layouts/app-layout.component';

export const routes: Routes = [
  { path: '', component: AuthLayoutComponent, canActivate: [guestGuard], children: [
    { path: 'login', component: LoginPage }, { path: 'cadastro', component: RegisterPage }, { path: 'esqueci-minha-senha', component: ForgotPasswordPage }, { path: 'redefinir-senha', component: ResetPasswordPage }
  ]},
  { path: '', component: AppLayoutComponent, canActivate: [authGuard], children: [
    { path: 'corretoras', component: CorretorasPage }, { path: 'acoes', component: AcoesPage }, { path: 'carteiras', component: CarteirasPage }, { path: 'historico/:acaoId', component: HistoricoPage }, { path: 'carteiras/:carteiraId/posicoes', component: PositionTablePage }, { path: 'vendas', component: VendasPage }, { path: 'visao-geral', component: InsightsDashboardPage }, { path: 'dashboard', component: InsightsDashboardPage }, { path: 'dashboard/:carteiraId', component: InsightsDashboardPage }, { path: '', pathMatch: 'full', redirectTo: 'visao-geral' }
  ]},
  { path: '**', redirectTo: 'login' },
];
