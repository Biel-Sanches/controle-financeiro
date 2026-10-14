package br.com.gsanches.financeiro.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Objetivo financeiro: juntar um valor para algo (celular, viagem...).
 */
public record Meta(
        long id,
        String nome,
        BigDecimal valorObjetivo,
        BigDecimal valorAcumulado) {

    public BigDecimal valorFaltante() {
        BigDecimal falta = valorObjetivo.subtract(valorAcumulado);
        return falta.max(BigDecimal.ZERO);
    }

    /** Progresso de 0 a 100 (com 1 casa decimal). */
    public BigDecimal percentual() {
        if (valorObjetivo.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return valorAcumulado
                .multiply(BigDecimal.valueOf(100))
                .divide(valorObjetivo, 1, RoundingMode.HALF_UP);
    }

    public Meta depositar(BigDecimal valor) {
        return new Meta(id, nome, valorObjetivo, valorAcumulado.add(valor));
    }
}
