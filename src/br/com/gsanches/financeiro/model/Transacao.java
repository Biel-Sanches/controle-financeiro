package br.com.gsanches.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lançamento financeiro (receita ou despesa).
 * Record imutável: para "editar", cria-se uma nova instância com o mesmo id.
 */
public record Transacao(
        long id,
        LocalDate data,
        TipoTransacao tipo,
        String categoria,
        String descricao,
        FormaPagamento formaPagamento,
        BigDecimal valor,
        String observacao) {

    public boolean isReceita() {
        return tipo == TipoTransacao.RECEITA;
    }
}
