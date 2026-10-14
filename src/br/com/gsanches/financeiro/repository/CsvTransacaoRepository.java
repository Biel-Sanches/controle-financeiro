package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.model.FormaPagamento;
import br.com.gsanches.financeiro.model.TipoTransacao;
import br.com.gsanches.financeiro.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Formato da linha: id;dataISO;tipo;categoria;descricao;forma;valor;observacao
 */
public class CsvTransacaoRepository extends CsvRepository<Transacao> implements TransacaoRepository {

    public CsvTransacaoRepository() {
        super("transacoes.csv");
    }

    @Override
    protected Transacao fromLinha(String[] c) {
        return new Transacao(
                Long.parseLong(c[0]),
                LocalDate.parse(c[1]),
                TipoTransacao.valueOf(c[2]),
                c[3],
                c[4],
                FormaPagamento.valueOf(c[5]),
                new BigDecimal(c[6]),
                c.length > 7 ? c[7] : "");
    }

    @Override
    protected String toLinha(Transacao t) {
        return String.join(";",
                String.valueOf(t.id()),
                t.data().toString(),
                t.tipo().name(),
                limpar(t.categoria()),
                limpar(t.descricao()),
                t.formaPagamento().name(),
                t.valor().toPlainString(),
                limpar(t.observacao()));
    }

    @Override
    protected long idDe(Transacao t) {
        return t.id();
    }

    @Override
    protected Transacao comId(Transacao t, long novoId) {
        return new Transacao(novoId, t.data(), t.tipo(), t.categoria(), t.descricao(),
                t.formaPagamento(), t.valor(), t.observacao());
    }
}
