package br.com.gsanches.financeiro.service;

import br.com.gsanches.financeiro.model.FormaPagamento;
import br.com.gsanches.financeiro.model.TipoTransacao;
import br.com.gsanches.financeiro.model.Transacao;
import br.com.gsanches.financeiro.repository.TransacaoRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Regras de negócio sobre transações: resumo, totais,
 * análise por categoria, relatórios e exportação.
 */
public class FinanceiroService {

    private final TransacaoRepository transacoes;

    public FinanceiroService(TransacaoRepository transacoes) {
        this.transacoes = transacoes;
    }

    // ===== Cadastro =====

    /** Cria uma transação nova (o repositório gera o id). */
    public Transacao registrar(LocalDate data, TipoTransacao tipo, String categoria,
                               String descricao, FormaPagamento forma, BigDecimal valor,
                               String observacao) {
        Transacao nova = new Transacao(0, data, tipo, categoria,
                descricao, forma, valor, observacao);
        return salvar(nova);
    }

    public java.util.Optional<Transacao> buscarPorId(long id) {
        return transacoes.buscarPorId(id);
    }

    public Transacao salvar(Transacao transacao) {
        if (transacao.valor() == null || transacao.valor().signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        return transacoes.salvar(transacao);
    }

    public boolean remover(long id) {
        return transacoes.remover(id);
    }

    public List<Transacao> buscarTodas() {
        return transacoes.buscarTodas().stream()
                .sorted(Comparator.comparing(Transacao::data).reversed()
                        .thenComparing(Transacao::id))
                .collect(Collectors.toList());
    }

    // ===== Consultas =====

    public List<Transacao> doPeriodo(LocalDate inicio, LocalDate fim) {
        return buscarTodas().stream()
                .filter(t -> !t.data().isBefore(inicio) && !t.data().isAfter(fim))
                .collect(Collectors.toList());
    }

    public List<Transacao> doMes(YearMonth mes) {
        return doPeriodo(mes.atDay(1), mes.atEndOfMonth());
    }

    public BigDecimal total(TipoTransacao tipo, List<Transacao> lista) {
        return lista.stream()
                .filter(t -> t.tipo() == tipo)
                .map(Transacao::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Saldo geral (todas as transações registradas). */
    public BigDecimal saldoGeral() {
        List<Transacao> todas = buscarTodas();
        return total(TipoTransacao.RECEITA, todas).subtract(total(TipoTransacao.DESPESA, todas));
    }

    /** Receitas - despesas de uma lista (o "sobrou" do mês). */
    public BigDecimal resultado(List<Transacao> lista) {
        return total(TipoTransacao.RECEITA, lista).subtract(total(TipoTransacao.DESPESA, lista));
    }

    // ===== Análise por categoria =====

    /** Total gasto por categoria (só despesas), já ordenado do maior para o menor. */
    public Map<String, BigDecimal> gastosPorCategoria(List<Transacao> lista) {
        Map<String, BigDecimal> gastos = new TreeMap<>();
        for (Transacao t : lista) {
            if (t.tipo() == TipoTransacao.DESPESA) {
                gastos.merge(t.categoria(), t.valor(), BigDecimal::add);
            }
        }
        return gastos.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    public Map.Entry<String, BigDecimal> categoriaComMaiorGasto(List<Transacao> lista) {
        return gastosPorCategoria(lista).entrySet().stream().findFirst().orElse(null);
    }

    /** Total de despesas por mês, dos últimos n meses até o mês de referência. */
    public Map<YearMonth, BigDecimal> evolucaoDespesas(YearMonth mesReferencia, int meses) {
        Map<YearMonth, BigDecimal> evolucao = new TreeMap<>();
        for (int i = meses - 1; i >= 0; i--) {
            YearMonth mes = mesReferencia.minusMonths(i);
            evolucao.put(mes, total(TipoTransacao.DESPESA, doMes(mes)));
        }
        return evolucao;
    }

    // ===== Exportação =====

    /** Conteúdo CSV do período (com BOM UTF-8 para o Excel). */
    public String csvConteudo(LocalDate inicio, LocalDate fim) {
        List<Transacao> lista = doPeriodo(inicio, fim);
        StringBuilder conteudo = new StringBuilder("\uFEFF"); // BOM UTF-8
        conteudo.append("Data;Tipo;Categoria;Descricao;Forma de pagamento;Valor;Observacoes\r\n");
        for (Transacao t : lista) {
            conteudo.append(t.data()).append(';')
                    .append(t.tipo().getRotulo()).append(';')
                    .append(t.categoria()).append(';')
                    .append(t.descricao()).append(';')
                    .append(t.formaPagamento().getRotulo()).append(';')
                    .append(t.valor().toPlainString()).append(';')
                    .append(t.observacao() == null ? "" : t.observacao())
                    .append("\r\n");
        }
        return conteudo.toString();
    }

    /** Exporta as transações do período em CSV e devolve o caminho gerado. */
    public Path exportarCsv(LocalDate inicio, LocalDate fim, Path destino) throws IOException {
        Files.createDirectories(destino.getParent());
        Files.write(destino, csvConteudo(inicio, fim).getBytes(StandardCharsets.UTF_8));
        return destino;
    }
}
