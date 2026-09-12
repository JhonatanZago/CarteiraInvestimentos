export interface AuthUser { id: number; nome: string; email: string; perfil: 'USER' | 'ADMIN'; }
export interface LoginRequest { email: string; senha: string; manterConectado: boolean; }
export interface LoginResponse { accessToken: string; expiresInSeconds: number; usuario: AuthUser; }
export interface RegisterRequest { nome: string; email: string; senha: string; confirmacaoSenha: string; }
