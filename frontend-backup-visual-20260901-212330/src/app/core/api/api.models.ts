export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  fieldErrors: Array<{ field: string; message: string }>;
}

export interface Corretora {
  id: number;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia?: string;
  email?: string;
  telefone?: string;
  cep?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  uf?: string;
  situacaoCadastral?: string;
  validadaMercadoFinanceiro: boolean;
  dataValidacaoMercado?: string;
  fonteValidacaoMercado?: string;
  dataCadastro: string;
}

export interface CorretoraCreateRequest {
  cnpj: string;
  cep: string;
  numero: string;
  complemento?: string;
}

export interface Acao {
  id: number;
  ticker: string;
  nomeEmpresa: string;
  mercado: 'BRASIL' | 'EUA';
  moeda: string;
  cotacaoAtual: number;
  dataHoraCotacao: string;
}

export interface AcaoCreateRequest {
  ticker: string;
  mercado: 'BRASIL' | 'EUA';
}

export interface HistoricoCotacao {
  id: number;
  acaoId: number;
  valor: number;
  dataHoraCotacao: string;
  dataHoraRegistro: string;
  fonte: string;
}

export interface Carteira {
  id: number;
  nome: string;
  descricao?: string;
  dataCriacao: string;
}

export interface CarteiraRequest {
  nome: string;
  descricao?: string;
}

export interface Posicao {
  id: number;
  carteiraId: number;
  acaoId: number;
  corretoraId: number;
  quantidade: number;
  precoMedio: number;
  dataPrimeiraCompra: string;
  mercado: 'BRASIL' | 'EUA';
  classificacaoAlocacao: 'ACOES_BRASIL' | 'ACOES_EXTERIOR';
  ticker: string;
  nomeEmpresa: string;
  cotacaoAtual: number | null;
  dataHoraCotacao: string | null;
  valorInvestido: number;
  valorAtual: number;
  resultado: number;
  rentabilidadePercentual: number;
}

export interface PosicaoRequest {
  acaoId: number;
  corretoraId: number;
  quantidade: number;
  precoMedio: number;
  dataPrimeiraCompra: string;
}

export interface DashboardCarteira {
  carteiraId: number;
  valorInvestido: number;
  valorAtual: number;
  resultado: number;
  rentabilidadePercentual: number;
  ultimaAtualizacao?: string;
  quantidadeAtivos: number;
  composicao: ComposicaoCarteira[];
}

export interface ComposicaoCarteira {
  posicaoId: number;
  acaoId: number;
  mercado: 'BRASIL' | 'EUA';
  classificacaoAlocacao: 'ACOES_BRASIL' | 'ACOES_EXTERIOR';
  ticker: string;
  nomeEmpresa: string;
  quantidade: number;
  cotacaoAtual: number | null;
  dataHoraCotacao: string | null;
  valorInvestido: number;
  valorAtual: number;
  resultado: number;
  rentabilidadePercentual: number;
}

export type InsightAvailability = 'AVAILABLE' | 'STALE' | 'UNAVAILABLE';
export interface MarketIndicator { codigo: string; descricao: string; valor: number | null; variacaoPercentual: number | null; referenciaEm: string | null; disponibilidade: InsightAvailability; }
export interface PortfolioEvolutionPoint { referenciaEm: string | null; valorInvestido: number; valorAtual: number; disponibilidade: InsightAvailability; }
export interface IncomeSummary { recebidosUltimosDozeMeses: number; proximosProventos: number | null; referenciaEm: string | null; disponibilidade: InsightAvailability; }
