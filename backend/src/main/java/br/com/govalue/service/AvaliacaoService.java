package br.com.govalue.service;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.Pergunta;
import br.com.govalue.repository.AvaliacaoFuncionarioRepository;
import br.com.govalue.repository.AvaliacaoRepository;
import br.com.govalue.repository.PadraoRespostaRepository;
import br.com.govalue.repository.PerguntaRepository;
import br.com.govalue.repository.TipoAvaliacaoRepository;
import br.com.govalue.web.dto.CatalogoDtos.AvaliacaoRequest;
import br.com.govalue.web.dto.CatalogoDtos.AvaliacaoResponse;
import br.com.govalue.web.dto.CatalogoDtos.PerguntaRequest;
import br.com.govalue.web.dto.CatalogoDtos.PerguntaResponse;
import br.com.govalue.web.dto.CatalogoDtos.TipoAvaliacaoResponse;
import br.com.govalue.web.error.NegocioException;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacoes;
    private final PerguntaRepository perguntas;
    private final TipoAvaliacaoRepository tipos;
    private final PadraoRespostaRepository padroes;
    private final AvaliacaoFuncionarioRepository vinculos;

    public AvaliacaoService(
            AvaliacaoRepository avaliacoes,
            PerguntaRepository perguntas,
            TipoAvaliacaoRepository tipos,
            PadraoRespostaRepository padroes,
            AvaliacaoFuncionarioRepository vinculos) {
        this.avaliacoes = avaliacoes;
        this.perguntas = perguntas;
        this.tipos = tipos;
        this.padroes = padroes;
        this.vinculos = vinculos;
    }

    @Transactional(readOnly = true)
    public List<TipoAvaliacaoResponse> listarTipos() {
        return tipos.findAll().stream().map(TipoAvaliacaoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listar() {
        return avaliacoes.findAll().stream().map(AvaliacaoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponse buscar(Long id) {
        return AvaliacaoResponse.de(obterAvaliacao(id));
    }

    @Transactional
    public AvaliacaoResponse criar(AvaliacaoRequest req) {
        Avaliacao avaliacao = new Avaliacao();
        aplicar(avaliacao, req);
        return AvaliacaoResponse.de(avaliacoes.save(avaliacao));
    }

    @Transactional
    public AvaliacaoResponse atualizar(Long id, AvaliacaoRequest req) {
        Avaliacao avaliacao = obterAvaliacao(id);
        aplicar(avaliacao, req);
        return AvaliacaoResponse.de(avaliacao);
    }

    /** Uma avaliacao ja vinculada a funcionarios nao pode ser excluida, para nao perder respostas. */
    @Transactional
    public void excluir(Long id) {
        Avaliacao avaliacao = obterAvaliacao(id);
        if (vinculos.existsByAvaliacaoId(id)) {
            throw new NegocioException("Avaliação já vinculada a funcionários não pode ser excluída");
        }
        perguntas.deleteAll(perguntas.findByAvaliacaoId(id));
        avaliacoes.delete(avaliacao);
    }

    @Transactional(readOnly = true)
    public List<PerguntaResponse> listarPerguntas(Long avaliacaoId) {
        obterAvaliacao(avaliacaoId);
        return perguntas.findByAvaliacaoId(avaliacaoId).stream().map(PerguntaResponse::de).toList();
    }

    @Transactional
    public PerguntaResponse criarPergunta(Long avaliacaoId, PerguntaRequest req) {
        Pergunta pergunta = new Pergunta();
        pergunta.setAvaliacao(obterAvaliacao(avaliacaoId));
        aplicar(pergunta, req);
        return PerguntaResponse.de(perguntas.save(pergunta));
    }

    @Transactional
    public PerguntaResponse atualizarPergunta(Long id, PerguntaRequest req) {
        Pergunta pergunta = obterPergunta(id);
        aplicar(pergunta, req);
        return PerguntaResponse.de(pergunta);
    }

    @Transactional
    public void excluirPergunta(Long id) {
        Pergunta pergunta = obterPergunta(id);
        if (vinculos.existsByPerguntaId(id)) {
            throw new NegocioException("Pergunta já vinculada a funcionários não pode ser excluída");
        }
        perguntas.delete(pergunta);
    }

    private void aplicar(Avaliacao avaliacao, AvaliacaoRequest req) {
        if (req.dataFimVigencia().isBefore(req.dataInicioVigencia())) {
            throw new NegocioException("A data fim da vigência não pode ser anterior ao início");
        }
        avaliacao.setDescricao(req.descricao());
        avaliacao.setDataInicioVigencia(req.dataInicioVigencia());
        avaliacao.setDataFimVigencia(req.dataFimVigencia());
        avaliacao.setTipoAvaliacao(tipos.findById(req.tipoAvaliacaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de avaliação", req.tipoAvaliacaoId())));
    }

    private void aplicar(Pergunta pergunta, PerguntaRequest req) {
        pergunta.setDescricao(req.descricao());
        pergunta.setPadraoResposta(padroes.findById(req.padraoRespostaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Padrão de resposta", req.padraoRespostaId())));
    }

    private Avaliacao obterAvaliacao(Long id) {
        return avaliacoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação", id));
    }

    private Pergunta obterPergunta(Long id) {
        return perguntas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta", id));
    }
}
