package br.com.gsanches.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Compromisso financeiro futuro: conta a pagar (aluguel, internet...)
 * ou a receber (salário previsto, etc.).
 */
public record Conta(
        long id,
        String nome,
        TipoConta tipo,
        BigDecimal valor,
        LocalDate vencimento,
        StatusConta status,
        LocalDate dataPagamento) {

    public boolean isPendente() {
        return status == StatusConta.PENDENTE;
    }

    /** Dias de atraso (0 se ainda não venceu ou já foi resolvida). */
    public long diasAtraso() {
        if (!isPendente() || !vencimento.isBefore(LocalDate.now())) {
            return 0;
        }
        return ChronoUnit.DAYS.between(vencimento, LocalDate.now());
    }

    /** Cópia com novo status e data de pagamento registrada. */
    public Conta resolver(StatusConta novoStatus, LocalDate pagamento) {
        return new Conta(id, nome, tipo, valor, vencimento, novoStatus, pagamento);
    }
}
