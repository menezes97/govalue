// Tipos que espelham os DTOs do backend (br.com.govalue.web.dto).

export type Perfil = 'ADMIN' | 'FUNCIONARIO'
export type StatusFuncionario = 'ATIVO' | 'INATIVO'
export type Visao = 'FUNCIONARIO' | 'GESTOR'

export interface Usuario {
  id: number
  nome: string
  email: string
  perfil: Perfil
  funcionarioId: number | null
  gestor: boolean
}

export interface LoginResponse {
  token: string
  expiraEm: string
  usuario: Usuario
}

export interface ApiErrorBody {
  status: number
  mensagem: string
  campos: Record<string, string>
  timestamp: string
}

// ---------- cadastros (admin) ----------

export interface Funcionario {
  id: number
  nome: string
  cpf: string
  funcao: string | null
  matricula: string | null
  area: string | null
  email: string
  status: StatusFuncionario
  gestorId: number | null
  gestorNome: string | null
}

export interface CriarFuncionario {
  nome: string
  cpf: string
  email: string
  senhaInicial: string
  funcao?: string
  matricula?: string
  area?: string
  gestorId?: number | null
}

export interface AtualizarFuncionario {
  nome: string
  cpf: string
  email: string
  status: StatusFuncionario
  funcao?: string
  matricula?: string
  area?: string
  gestorId?: number | null
}

export interface TipoAvaliacao {
  id: number
  descricao: string
}

export interface Avaliacao {
  id: number
  descricao: string
  dataInicioVigencia: string
  dataFimVigencia: string
  tipoAvaliacaoId: number
  tipoAvaliacao: string
}

export interface AvaliacaoRequest {
  descricao: string
  dataInicioVigencia: string
  dataFimVigencia: string
  tipoAvaliacaoId: number
}

export interface Pergunta {
  id: number
  avaliacaoId: number
  descricao: string
  padraoRespostaId: number
  padraoResposta: string
}

export interface PerguntaRequest {
  descricao: string
  padraoRespostaId: number
}

export interface PadraoResposta {
  id: number
  descricao: string
}

export interface Resposta {
  id: number
  padraoRespostaId: number
  descricao: string
}

// ---------- fluxo de avaliacao ----------

export interface VinculoResultado {
  avaliacaoId: number
  totalPerguntas: number
  funcionariosVinculados: number
  funcionariosJaVinculados: number
}

export interface OpcaoResposta {
  id: number
  descricao: string
}

export interface PerguntaRespondivel {
  perguntaId: number
  descricao: string
  opcoes: OpcaoResposta[]
  respostaFuncionarioId: number | null
  respostaGestorId: number | null
}

export interface DetalheAvaliacao {
  avaliacaoId: number
  descricao: string
  tipo: string
  dataInicioVigencia: string
  dataFimVigencia: string
  aberta: boolean
  funcionarioId: number
  funcionarioNome: string
  perguntas: PerguntaRespondivel[]
}

export interface ResumoAvaliacao {
  avaliacaoId: number
  descricao: string
  tipo: string
  dataInicioVigencia: string
  dataFimVigencia: string
  aberta: boolean
  totalPerguntas: number
  respondidas: number
}

export interface PendenciaGestor {
  funcionarioId: number
  funcionarioNome: string
  avaliacaoId: number
  descricao: string
  dataFimVigencia: string
  aberta: boolean
  totalPerguntas: number
  respondidasPeloGestor: number
}

export interface ItemResposta {
  perguntaId: number
  respostaId: number
}

// ---------- dashboard ----------

export interface ConclusaoFuncionario {
  funcionarioId: number
  nome: string
  totalPerguntas: number
  respondidas: number
  percentual: number
  respondidasPeloGestor: number
  percentualGestor: number
}

export interface Conclusao {
  avaliacaoId: number
  descricao: string
  totalFuncionarios: number
  concluidos: number
  percentualGeral: number
  funcionarios: ConclusaoFuncionario[]
}

export interface OpcaoResultado {
  respostaId: number
  descricao: string
  quantidade: number
  percentual: number
}

export interface ResultadoPergunta {
  perguntaId: number
  descricao: string
  totalVinculados: number
  respondidas: number
  semResposta: number
  semRespostaPercentual: number
  opcoes: OpcaoResultado[]
}

export interface ResultadoAvaliacao {
  avaliacaoId: number
  descricao: string
  visao: Visao
  perguntas: ResultadoPergunta[]
}
