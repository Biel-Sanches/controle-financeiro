package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.model.Meta;

import java.util.List;
import java.util.Optional;

public interface MetaRepository {

    List<Meta> buscarTodas();

    Optional<Meta> buscarPorId(long id);

    Meta salvar(Meta meta);

    boolean remover(long id);

    long proximoId();
}
