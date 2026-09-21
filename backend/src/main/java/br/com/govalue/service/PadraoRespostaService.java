package br.com.govalue.service;

import br.com.govalue.domain.PadraoResposta;
import br.com.govalue.domain.Resposta;
import br.com.govalue.repository.PadraoRespostaRepository;
import br.com.govalue.repository.RespostaRepository;
import br.com.govalue.web.dto.CatalogoDtos.PadraoRespostaRequest;
import br.com.govalue.web.dto.CatalogoDtos.PadraoRespostaResponse;
import br.com.govalue.web.dto.CatalogoDtos.RespostaRequest;
import br.com.govalue.web.dto.CatalogoDtos.RespostaResponse;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Padroes de resposta (escalas) e suas opcoes. Excluir algo em uso e barrado pelas FKs do banco
 * e devolvido como 409 pelo tratador global de erros.
 */
@Service
public class PadraoRespostaService {

    private final PadraoRespostaRepository padroes;
    private final RespostaRepository respostas;

    public PadraoRespostaService(PadraoRespostaRepository padroes, RespostaRepository respostas) {
        this.padroes = padroes;
        this.respostas = respostas;
    }

    @Transactional(readOnly = true)
    public List<PadraoRespostaResponse> listar() {
        return padroes.findAll().stream().map(PadraoRespostaResponse::de).toList();
    }

    @Transactional
    public PadraoRespostaResponse criar(PadraoRespostaRequest req) {
        PadraoResposta padrao = new PadraoResposta();
        padrao.setDescricao(req.descricao());
        return PadraoRespostaResponse.de(padroes.save(padrao));
    }

    @Transactional
    public PadraoRespostaResponse atualizar(Long id, PadraoRespostaRequest req) {
        PadraoResposta padrao = obterPadrao(id);
        padrao.setDescricao(req.descricao());
        return PadraoRespostaResponse.de(padrao);
    }

    @Transactional
    public void excluir(Long id) {
        padroes.delete(obterPadrao(id));
        padroes.flush();
    }

    @Transactional(readOnly = true)
    public List<RespostaResponse> listarRespostas(Long padraoId) {
        obterPadrao(padraoId);
        return respostas.findByPadraoRespostaId(padraoId).stream().map(RespostaResponse::de).toList();
    }

    @Transactional
    public RespostaResponse criarResposta(Long padraoId, RespostaRequest req) {
        Resposta resposta = new Resposta();
        resposta.setPadraoResposta(obterPadrao(padraoId));
        resposta.setDescricao(req.descricao());
        return RespostaResponse.de(respostas.save(resposta));
    }

    @Transactional
    public RespostaResponse atualizarResposta(Long id, RespostaRequest req) {
        Resposta resposta = obterResposta(id);
        resposta.setDescricao(req.descricao());
        return RespostaResponse.de(resposta);
    }

    @Transactional
    public void excluirResposta(Long id) {
        respostas.delete(obterResposta(id));
        respostas.flush();
    }

    private PadraoResposta obterPadrao(Long id) {
        return padroes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Padrao de resposta", id));
    }

    private Resposta obterResposta(Long id) {
        return respostas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Resposta", id));
    }
}
