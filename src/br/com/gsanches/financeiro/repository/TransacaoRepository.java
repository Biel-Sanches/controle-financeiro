package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.model.Transacao;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência das transações.
 * Hoje existe a implementação em CSV; quando chegarmos ao MySQL,
 * basta criar uma nova classe que implemente esta interface —
 * o restante do sistema não muda.
 */
public interface TransacaoRepository {

    List<Transacao> buscarTodas();

    Optional<Transacao> buscarPorId(long id);

    Transacao salvar(Transacao transacao);

    boolean remover(long id);

    long proximoId();
}
