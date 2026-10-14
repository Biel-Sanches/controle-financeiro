package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.model.Conta;
import br.com.gsanches.financeiro.model.StatusConta;
import br.com.gsanches.financeiro.model.TipoConta;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Formato da linha: id;nome;tipo;valor;vencimentoISO;status;dataPagamentoISO(ou vazio)
 */
public class CsvContaRepository extends CsvRepository<Conta> implements ContaRepository {

    public CsvContaRepository() {
        super("contas.csv");
    }

    @Override
    protected Conta fromLinha(String[] c) {
        LocalDate dataPagamento = (c.length > 6 && !c[6].isBlank()) ? LocalDate.parse(c[6]) : null;
        return new Conta(
                Long.parseLong(c[0]),
                c[1],
                TipoConta.valueOf(c[2]),
                new BigDecimal(c[3]),
                LocalDate.parse(c[4]),
                StatusConta.valueOf(c[5]),
                dataPagamento);
    }

    @Override
    protected String toLinha(Conta conta) {
        return String.join(";",
                String.valueOf(conta.id()),
                limpar(conta.nome()),
                conta.tipo().name(),
                conta.valor().toPlainString(),
                conta.vencimento().toString(),
                conta.status().name(),
                conta.dataPagamento() == null ? "" : conta.dataPagamento().toString());
    }

    @Override
    protected long idDe(Conta conta) {
        return conta.id();
    }
}
