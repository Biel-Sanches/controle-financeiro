package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.model.Conta;

import java.util.List;
import java.util.Optional;

public interface ContaRepository {

    List<Conta> buscarTodas();

    Optional<Conta> buscarPorId(long id);

    Conta salvar(Conta conta);

    boolean remover(long id);

    long proximoId();
}
