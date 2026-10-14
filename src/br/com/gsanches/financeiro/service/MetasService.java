package br.com.gsanches.financeiro.service;

import br.com.gsanches.financeiro.model.Meta;
import br.com.gsanches.financeiro.repository.MetaRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Regras para metas financeiras: criar objetivos,
 * depositar valores e acompanhar o progresso.
 */
public class MetasService {

    private final MetaRepository metas;

    public MetasService(MetaRepository metas) {
        this.metas = metas;
    }

    public Meta criar(String nome, BigDecimal valorObjetivo) {
        if (valorObjetivo == null || valorObjetivo.signum() <= 0) {
            throw new IllegalArgumentException("O valor do objetivo deve ser maior que zero.");
        }
        Meta meta = new Meta(metas.proximoId(), nome, valorObjetivo, BigDecimal.ZERO);
        return metas.salvar(meta);
    }

    public Meta depositar(long id, BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser maior que zero.");
        }
        Meta meta = metas.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Meta " + id + " não encontrada."));
        return metas.salvar(meta.depositar(valor));
    }

    public boolean remover(long id) {
        return metas.remover(id);
    }

    public List<Meta> buscarTodas() {
        return metas.buscarTodas().stream()
                .sorted(Comparator.comparing(Meta::id))
                .collect(Collectors.toList());
    }
}
