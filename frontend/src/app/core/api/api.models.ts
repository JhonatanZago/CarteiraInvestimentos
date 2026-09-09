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
  statusValidacao?: 'VALIDADA' | 'NAO_AUTORIZADA' | 'AGUARDANDO_VALIDACAO' | 'FONTE_INDISPONIVEL';
  motivoValidacao?: string;
  dataValidacaoMercado?: string;
  fonteValidacaoMercado?: string;
  dataCadastro: string;
  logoUrl?: string | null;
  logoSource?: string | null;
  logoUpdatedAt?: string | null;
  website?: string | null;
  statusLogo?: 'AVAILABLE' | 'FALLBACK' | 'NOT_FOUND' | 'SOURCE_UNAVAILABLE' | 'INVALID_URL' | null;
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
  cotacaoAtual: number | null;
  dataHoraCotacao: string | null;
  logoUrl?: string | null;
  listingCountryCode?: string | null;
  exchange?: string | null;
  exchangeMic?: string | null;
  dataSource?: string | null;
}

export interface AcaoCreateRequest {
  ticker: string;
  mercado: 'BRASIL' | 'EUA';
  selectedCountryCode: string;
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
  logoUrl?: string | null;
  cotacaoAtual: number | null;
  dataHoraCotacao: string | null;
  valorInvestido: number;
  valorAtual: number | null;
  resultado: number | null;
  rentabilidadePercentual: number | null;
  moeda?: string | null;
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
  valorAtual: number | null;
  resultado: number | null;
  rentabilidadePercentual: number | null;
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
  logoUrl?: string | null;
  quantidade: number;
  precoMedio: number;
  cotacaoAtual: number | null;
  dataHoraCotacao: string | null;
  valorInvestido: number;
  valorAtual: number | null;
  resultado: number | null;
  rentabilidadePercentual: number | null;
  moeda?: string | null;
  conversaoEstimada?: boolean;
  valorInvestidoConvertidoBase?: number | null;
  valorAtualConvertidoBase?: number | null;
  cambioParaBase?: number | null;
}

export type InsightAvailability = 'AVAILABLE' | 'STALE' | 'UNAVAILABLE';
export interface MarketIndicator { codigo: string; descricao: string; valor: number | null; variacaoPercentual: number | null; referenciaEm: string | null; disponibilidade: InsightAvailability; }
export interface PortfolioEvolutionPoint { referenciaEm: string | null; valorInvestido: number; valorAtual: number; disponibilidade: InsightAvailability; timestamp?: string | null; investedAmount?: number | null; currentAmount?: number | null; resultAmount?: number | null; returnPercentage?: number | null; }
export interface IncomeSummary { recebidosUltimosDozeMeses: number; proximosProventos: number | null; referenciaEm: string | null; disponibilidade: InsightAvailability; }
export interface CurrencyAnalysis { exposicoes: Array<{ moeda: string; nome: string; valorOriginal: number; percentual: number | null }>; quantidadeEmLucro: number; quantidadeEmPrejuizo: number; quantidadeNeutra: number; resultadosPorMoeda: Array<{ moeda: string; resultado: number }>; ativos: Array<{ acaoId: number; ticker: string; nomeEmpresa: string; logoUrl?: string | null; moeda: string; resultado: number | null; rentabilidade: number | null }>; moedaBase: string; mensagemCambio?: string | null; }
