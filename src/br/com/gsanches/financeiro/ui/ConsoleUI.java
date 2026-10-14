package br.com.gsanches.financeiro.ui;

import br.com.gsanches.financeiro.model.Conta;
import br.com.gsanches.financeiro.model.FormaPagamento;
import br.com.gsanches.financeiro.model.Meta;
import br.com.gsanches.financeiro.model.TipoConta;
import br.com.gsanches.financeiro.model.TipoTransacao;
import br.com.gsanches.financeiro.model.Transacao;
import br.com.gsanches.financeiro.service.ContasService;
import br.com.gsanches.financeiro.service.FinanceiroService;
import br.com.gsanches.financeiro.service.MetasService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Interface de console: menus, entrada de dados e exibição dos relatórios.
 */
public class ConsoleUI {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final String[] CATEGORIAS_RECEITA = { "Salário", "Freelance", "Presente", "Outros" };
    private static final String[] CATEGORIAS_DESPESA = { "Alimentação", "Transporte", "Moradia",
            "Lazer", "Saúde", "Estudos", "Outros" };

    private final Scanner entrada = new Scanner(System.in);
    private final FinanceiroService financeiro;
    private final ContasService contas;
    private final MetasService metas;
    private YearMonth mesReferencia = YearMonth.now();

    public ConsoleUI(FinanceiroService financeiro, ContasService contas, MetasService metas) {
        this.financeiro = financeiro;
        this.contas = contas;
        this.metas = metas;
    }

    // ===== Menu principal =====

    public void executar() {
        System.out.println("\n=== CONTROLE FINANCEIRO PESSOAL ===");
        boolean rodando = true;
        while (rodando) {
            mostrarResumo();
            System.out.println("""
                    \n--- MENU ---
                    2 - Receitas e despesas
                    3 - Contas a pagar e a receber
                    4 - Análise de gastos
                    5 - Metas financeiras
                    6 - Relatórios e exportação
                    7 - Trocar mês de referência
                    0 - Sair""");
            String opcao = ler("\nOpção: ");
            switch (opcao) {
                case "2" -> menuTransacoes();
                case "3" -> menuContas();
                case "4" -> menuAnalise();
                case "5" -> menuMetas();
                case "6" -> menuRelatorios();
                case "7" -> trocarMes();
                case "0" -> {
                    System.out.println("Até mais! 👋");
                    rodando = false;
                }
                default -> System.out.println("⚠ Opção inválida.");
            }
        }
    }

    // ===== 1. Resumo financeiro (tela inicial) =====

    private void mostrarResumo() {
        List<Transacao> doMes = financeiro.doMes(mesReferencia);
        BigDecimal receitas = financeiro.total(TipoTransacao.RECEITA, doMes);
        BigDecimal despesas = financeiro.total(TipoTransacao.DESPESA, doMes);
        List<Conta> pendentes = contas.pendentes();

        System.out.printf("%n══════════ %s ══════════%n", nomeMes(mesReferencia).toUpperCase());
        System.out.printf("  Saldo atual:         %12s%n", moeda(financeiro.saldoGeral()));
        System.out.printf("  Receitas do mês:     %12s%n", moeda(receitas));
        System.out.printf("  Despesas do mês:     %12s%n", moeda(despesas));
        System.out.printf("  Sobrou no mês:       %12s%n", moeda(financeiro.resultado(doMes)));
        if (pendentes.isEmpty()) {
            System.out.println("  Contas pendentes:    nenhuma");
        } else {
            int vencidas = contas.vencidas().size();
            String aviso = vencidas > 0 ? String.format("  (⚠ %d vencida(s)!)", vencidas) : "";
            System.out.printf("  Contas pendentes:    %d conta(s) — %s %s%n",
                    pendentes.size(), moeda(contas.totalPendente()), aviso);
        }
    }

    // ===== 2. Receitas e despesas =====

    private void menuTransacoes() {
        while (true) {
            System.out.println("""

                    --- RECEITAS E DESPESAS ---
                    1 - Nova receita
                    2 - Nova despesa
                    3 - Listar transações do mês
                    4 - Consultar por período
                    5 - Editar transação
                    6 - Excluir transação
                    0 - Voltar""");
            String opcao = ler("\nOpção: ");
            switch (opcao) {
                case "1" -> novaTransacao(TipoTransacao.RECEITA);
                case "2" -> novaTransacao(TipoTransacao.DESPESA);
                case "3" -> listarTransacoes(financeiro.doMes(mesReferencia),
                        "Transações de " + nomeMes(mesReferencia));
                case "4" -> consultarPorPeriodo();
                case "5" -> editarTransacao();
                case "6" -> excluirTransacao();
                case "0" -> { return; }
                default -> System.out.println("⚠ Opção inválida.");
            }
        }
    }

    private void novaTransacao(TipoTransacao tipo) {
        System.out.println("\n--- NOVA " + tipo.getRotulo().toUpperCase() + " ---");
        String descricao = lerObrigatorio("Descrição: ");
        BigDecimal valor = lerValor("Valor: ", null);
        LocalDate data = lerData("Data (dd/mm/aaaa, Enter = hoje): ", LocalDate.now());
        String categoria = lerCategoria(tipo);
        FormaPagamento forma = lerFormaPagamento();
        String observacao = ler("Observações (Enter = nenhuma): ");

        try {
            financeiro.registrar(data, tipo, categoria, descricao, forma, valor, observacao);
            System.out.println("✔ " + tipo.getRotulo() + " registrada: " + descricao + " — " + moeda(valor));
        } catch (IllegalArgumentException e) {
            System.out.println("⚠ " + e.getMessage());
        }
    }

    private void listarTransacoes(List<Transacao> lista, String titulo) {
        System.out.printf("%n--- %s ---%n", titulo);
        if (lista.isEmpty()) {
            System.out.println("Nenhuma transação encontrada.");
            return;
        }
        System.out.printf("%-4s %-10s %-8s %-15s %-14s %-25s %13s%n",
                "ID", "DATA", "TIPO", "CATEGORIA", "FORMA", "DESCRIÇÃO", "VALOR");
        for (Transacao t : lista) {
            System.out.printf("%-4d %-10s %-8s %-15s %-14s %-25s %13s%n",
                    t.id(), t.data().format(FMT_DATA), t.tipo().getRotulo(),
                    truncar(t.categoria(), 15), truncar(t.formaPagamento().getRotulo(), 14),
                    truncar(t.descricao(), 25), moeda(t.valor()));
            if (t.observacao() != null && !t.observacao().isBlank()) {
                System.out.printf("%-4s └ obs: %s%n", "", truncar(t.observacao(), 60));
            }
        }
        System.out.printf("%nReceitas: %s | Despesas: %s | Resultado: %s%n",
                moeda(financeiro.total(TipoTransacao.RECEITA, lista)),
                moeda(financeiro.total(TipoTransacao.DESPESA, lista)),
                moeda(financeiro.resultado(lista)));
    }

    private void consultarPorPeriodo() {
        LocalDate inicio = lerData("Data inicial (dd/mm/aaaa): ", null);
        LocalDate fim = lerData("Data final (dd/mm/aaaa): ", null);
        if (fim.isBefore(inicio)) {
            System.out.println("⚠ A data final não pode ser antes da inicial.");
            return;
        }
        listarTransacoes(financeiro.doPeriodo(inicio, fim),
                "Período " + inicio.format(FMT_DATA) + " a " + fim.format(FMT_DATA));
    }

    private void editarTransacao() {
        listarTransacoes(financeiro.doMes(mesReferencia), "Editar — " + nomeMes(mesReferencia));
        long id = lerLong("\nID da transação a editar (0 = cancelar): ");
        if (id == 0) {
            return;
        }
        Transacao atual = financeiro.buscarPorId(id).orElse(null);
        if (atual == null) {
            System.out.println("⚠ Transação não encontrada.");
            return;
        }
        System.out.println("Deixe em branco (Enter) para manter o valor atual.");
        String descricao = lerOuPadrao("Descrição [" + atual.descricao() + "]: ", atual.descricao());
        BigDecimal valor = lerValor("Valor [" + atual.valor().toPlainString() + "]: ", atual.valor());
        LocalDate data = lerData("Data [" + atual.data().format(FMT_DATA) + "]: ", atual.data());
        String categoria = lerOuPadrao("Categoria [" + atual.categoria() + "]: ", atual.categoria());
        FormaPagamento forma = lerForma(atual.formaPagamento());
        String observacao = lerOuPadrao("Observações [" + (atual.observacao() == null ? "" : atual.observacao()) + "]: ",
                atual.observacao() == null ? "" : atual.observacao());

        financeiro.salvar(new Transacao(id, data, atual.tipo(), categoria, descricao,
                forma, valor, observacao));
        System.out.println("✔ Transação atualizada.");
    }

    private void excluirTransacao() {
        listarTransacoes(financeiro.doMes(mesReferencia), "Excluir — " + nomeMes(mesReferencia));
        long id = lerLong("\nID da transação a excluir (0 = cancelar): ");
        if (id == 0) {
            return;
        }
        if (confirmar("Confirmar exclusão? (s/n): ")) {
            System.out.println(financeiro.remover(id) ? "✔ Transação excluída." : "⚠ Transação não encontrada.");
        }
    }

    // ===== 3. Contas a pagar e a receber =====

    private void menuContas() {
        while (true) {
            System.out.println("""

                    --- CONTAS A PAGAR E A RECEBER ---
                    1 - Nova conta a pagar
                    2 - Nova conta a receber
                    3 - Listar contas
                    4 - Registrar pagamento/recebimento
                    5 - Excluir conta
                    0 - Voltar""");
            String opcao = ler("\nOpção: ");
            switch (opcao) {
                case "1" -> novaConta(TipoConta.A_PAGAR);
                case "2" -> novaConta(TipoConta.A_RECEBER);
                case "3" -> listarContas();
                case "4" -> registrarPagamento();
                case "5" -> excluirConta();
                case "0" -> { return; }
                default -> System.out.println("⚠ Opção inválida.");
            }
        }
    }

    private void novaConta(TipoConta tipo) {
        System.out.println("\n--- NOVA CONTA " + tipo.getRotulo().toUpperCase() + " ---");
        String nome = lerObrigatorio("Nome da conta: ");
        BigDecimal valor = lerValor("Valor: ", null);
        LocalDate vencimento = lerData("Vencimento (dd/mm/aaaa): ", null);
        try {
            contas.criar(nome, tipo, valor, vencimento);
            System.out.println("✔ Conta registrada: " + nome + " — " + moeda(valor)
                    + " (vence " + vencimento.format(FMT_DATA) + ")");
        } catch (IllegalArgumentException e) {
            System.out.println("⚠ " + e.getMessage());
        }
    }

    private void listarContas() {
        List<Conta> lista = contas.buscarTodas();
        System.out.printf("%n--- CONTAS ---%n");
        if (lista.isEmpty()) {
            System.out.println("Nenhuma conta registrada.");
            return;
        }
        System.out.printf("%-4s %-10s %-10s %-10s %-25s %13s%n",
                "ID", "VENCIMENTO", "TIPO", "STATUS", "NOME", "VALOR");
        for (Conta c : lista) {
            System.out.printf("%-4d %-10s %-10s %-10s %-25s %13s%n",
                    c.id(), c.vencimento().format(FMT_DATA), c.tipo().getRotulo(),
                    c.status().getRotulo(), truncar(c.nome(), 25), moeda(c.valor()));
            if (c.diasAtraso() > 0) {
                System.out.printf("%-4s └ ⚠ VENCIDA há %d dia(s)!%n", "", c.diasAtraso());
            } else if (c.dataPagamento() != null) {
                System.out.printf("%-4s └ resolvida em %s%n", "", c.dataPagamento().format(FMT_DATA));
            }
        }
    }

    private void registrarPagamento() {
        listarContas();
        long id = lerLong("\nID da conta a resolver (0 = cancelar): ");
        if (id == 0) {
            return;
        }
        LocalDate data = lerData("Data do pagamento (Enter = hoje): ", LocalDate.now());
        try {
            Conta resolvida = contas.resolver(id, data);
            System.out.println("✔ Conta \"" + resolvida.nome() + "\" marcada como "
                    + resolvida.status().getRotulo().toLowerCase() + ".");
        } catch (IllegalArgumentException e) {
            System.out.println("⚠ " + e.getMessage());
        }
    }

    private void excluirConta() {
        listarContas();
        long id = lerLong("\nID da conta a excluir (0 = cancelar): ");
        if (id == 0) {
            return;
        }
        if (confirmar("Confirmar exclusão? (s/n): ")) {
            System.out.println(contas.remover(id) ? "✔ Conta excluída." : "⚠ Conta não encontrada.");
        }
    }

    // ===== 4. Análise de gastos =====

    private void menuAnalise() {
        System.out.printf("%n════════ ANÁLISE DE GASTOS — %s ════════%n", nomeMes(mesReferencia).toUpperCase());
        List<Transacao> doMes = financeiro.doMes(mesReferencia);
        BigDecimal totalDespesas = financeiro.total(TipoTransacao.DESPESA, doMes);
        System.out.printf("Total de despesas: %s%n", moeda(totalDespesas));

        Map<String, BigDecimal> porCategoria = financeiro.gastosPorCategoria(doMes);
        if (porCategoria.isEmpty()) {
            System.out.println("Nenhuma despesa registrada neste mês.");
            return;
        }

        System.out.println("\nGastos por categoria (maior → menor):");
        for (Map.Entry<String, BigDecimal> gasto : porCategoria.entrySet()) {
            double participacao = totalDespesas.signum() == 0 ? 0
                    : gasto.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(totalDespesas, 1, RoundingMode.HALF_UP).doubleValue();
            System.out.printf("  %-15s %12s  %s %4.1f%%%n", truncar(gasto.getKey(), 15),
                    moeda(gasto.getValue()), barra(participacao, 20), participacao);
        }

        Map.Entry<String, BigDecimal> maior = financeiro.categoriaComMaiorGasto(doMes);
        System.out.printf("%nCategoria com maior gasto: %s (%s)%n", maior.getKey(), moeda(maior.getValue()));

        // comparação com o mês anterior
        YearMonth anterior = mesReferencia.minusMonths(1);
        Map<String, BigDecimal> porCategoriaAnterior = financeiro.gastosPorCategoria(financeiro.doMes(anterior));
        System.out.printf("%nComparação com %s:%n", nomeMes(anterior));
        for (Map.Entry<String, BigDecimal> gasto : porCategoria.entrySet()) {
            BigDecimal antes = porCategoriaAnterior.getOrDefault(gasto.getKey(), BigDecimal.ZERO);
            String variacao;
            if (antes.signum() == 0) {
                variacao = "nova categoria";
            } else {
                double pct = gasto.getValue().subtract(antes)
                        .divide(antes, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                variacao = String.format("%s%.1f%%", pct >= 0 ? "↑ " : "↓ ", Math.abs(pct));
            }
            System.out.printf("  %-15s %12s  (antes: %12s, %s)%n", truncar(gasto.getKey(), 15),
                    moeda(gasto.getValue()), moeda(antes), variacao);
        }

        System.out.println("\nEvolução das despesas (últimos 6 meses):");
        Map<YearMonth, BigDecimal> evolucao = financeiro.evolucaoDespesas(mesReferencia, 6);
        BigDecimal maximo = evolucao.values().stream().reduce(BigDecimal.ZERO, BigDecimal::max);
        for (Map.Entry<YearMonth, BigDecimal> mes : evolucao.entrySet()) {
            double escala = maximo.signum() == 0 ? 0
                    : mes.getValue().divide(maximo, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
            System.out.printf("  %s  %12s  %s%n", mes.getKey().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                    moeda(mes.getValue()), barra(escala, 20));
        }
    }

    // ===== 5. Metas financeiras =====

    private void menuMetas() {
        while (true) {
            System.out.println("""

                    --- METAS FINANCEIRAS ---
                    1 - Nova meta
                    2 - Listar metas
                    3 - Depositar em uma meta
                    4 - Excluir meta
                    0 - Voltar""");
            String opcao = ler("\nOpção: ");
            switch (opcao) {
                case "1" -> novaMeta();
                case "2" -> listarMetas();
                case "3" -> depositarMeta();
                case "4" -> excluirMeta();
                case "0" -> { return; }
                default -> System.out.println("⚠ Opção inválida.");
            }
        }
    }

    private void novaMeta() {
        System.out.println("\n--- NOVA META ---");
        String nome = lerObrigatorio("Nome da meta (ex: Celular novo): ");
        BigDecimal objetivo = lerValor("Valor do objetivo: ", null);
        try {
            metas.criar(nome, objetivo);
            System.out.println("✔ Meta criada: " + nome + " — objetivo " + moeda(objetivo));
        } catch (IllegalArgumentException e) {
            System.out.println("⚠ " + e.getMessage());
        }
    }

    private void listarMetas() {
        List<Meta> lista = metas.buscarTodas();
        System.out.printf("%n--- METAS ---%n");
        if (lista.isEmpty()) {
            System.out.println("Nenhuma meta criada.");
            return;
        }
        for (Meta m : lista) {
            double pct = m.percentual().doubleValue();
            System.out.printf("%n[%d] %s%n", m.id(), m.nome());
            System.out.printf("     Objetivo: %12s | Acumulado: %12s | Falta: %12s%n",
                    moeda(m.valorObjetivo()), moeda(m.valorAcumulado()), moeda(m.valorFaltante()));
            System.out.printf("     %s %5.1f%%%s%n", barra(pct, 24), pct,
                    pct >= 100 ? "  🎉 meta concluída!" : "");
        }
    }

    private void depositarMeta() {
        listarMetas();
        long id = lerLong("\nID da meta para depósito (0 = cancelar): ");
        if (id == 0) {
            return;
        }
        BigDecimal valor = lerValor("Valor do depósito: ", null);
        try {
            Meta meta = metas.depositar(id, valor);
            System.out.printf("✔ Depósito feito! %s: %s de %s (%.1f%%)%n", meta.nome(),
                    moeda(meta.valorAcumulado()), moeda(meta.valorObjetivo()),
                    meta.percentual().doubleValue());
        } catch (IllegalArgumentException e) {
            System.out.println("⚠ " + e.getMessage());
        }
    }

    private void excluirMeta() {
        listarMetas();
        long id = lerLong("\nID da meta a excluir (0 = cancelar): ");
        if (id == 0) {
            return;
        }
        if (confirmar("Confirmar exclusão? (s/n): ")) {
            System.out.println(metas.remover(id) ? "✔ Meta excluída." : "⚠ Meta não encontrada.");
        }
    }

    // ===== 6. Relatórios =====

    private void menuRelatorios() {
        while (true) {
            System.out.println("""

                    --- RELATÓRIOS E EXPORTAÇÃO ---
                    1 - Relatório do mês de referência
                    2 - Relatório por período
                    3 - Exportar período para CSV (Excel)
                    0 - Voltar""");
            String opcao = ler("\nOpção: ");
            switch (opcao) {
                case "1" -> relatorio(mesReferencia.atDay(1), mesReferencia.atEndOfMonth());
                case "2" -> {
                    LocalDate inicio = lerData("Data inicial (dd/mm/aaaa): ", null);
                    LocalDate fim = lerData("Data final (dd/mm/aaaa): ", null);
                    if (fim.isBefore(inicio)) {
                        System.out.println("⚠ A data final não pode ser antes da inicial.");
                    } else {
                        relatorio(inicio, fim);
                    }
                }
                case "3" -> exportarCsv();
                case "0" -> { return; }
                default -> System.out.println("⚠ Opção inválida.");
            }
        }
    }

    private void relatorio(LocalDate inicio, LocalDate fim) {
        List<Transacao> lista = financeiro.doPeriodo(inicio, fim);
        System.out.printf("%n════════ RELATÓRIO — %s a %s ════════%n",
                inicio.format(FMT_DATA), fim.format(FMT_DATA));
        System.out.printf("Receitas:   %12s%n", moeda(financeiro.total(TipoTransacao.RECEITA, lista)));
        System.out.printf("Despesas:   %12s%n", moeda(financeiro.total(TipoTransacao.DESPESA, lista)));
        System.out.printf("Saldo final: %11s%n", moeda(financeiro.resultado(lista)));

        Map<String, BigDecimal> categorias = financeiro.gastosPorCategoria(lista);
        if (!categorias.isEmpty()) {
            System.out.println("\nGastos por categoria:");
            for (Map.Entry<String, BigDecimal> gasto : categorias.entrySet()) {
                System.out.printf("  %-15s %12s%n", truncar(gasto.getKey(), 15), moeda(gasto.getValue()));
            }
        }
        listarTransacoes(lista, "Histórico de movimentações");
    }

    private void exportarCsv() {
        LocalDate inicio = lerData("Data inicial (Enter = 1º dia do mês de referência): ",
                mesReferencia.atDay(1));
        LocalDate fim = lerData("Data final (Enter = último dia do mês de referência): ",
                mesReferencia.atEndOfMonth());
        if (fim.isBefore(inicio)) {
            System.out.println("⚠ A data final não pode ser antes da inicial.");
            return;
        }
        String nomeArquivo = "relatorios/relatorio_" + inicio + "_" + fim + ".csv";
        try {
            var caminho = financeiro.exportarCsv(inicio, fim, java.nio.file.Path.of(nomeArquivo));
            int linhas = financeiro.doPeriodo(inicio, fim).size();
            System.out.println("✔ Exportado: " + caminho + " (" + linhas + " transações). Abra no Excel!");
        } catch (Exception e) {
            System.out.println("⚠ Não foi possível exportar: " + e.getMessage());
        }
    }

    // ===== Utilidades de entrada e exibição =====

    private void trocarMes() {
        String texto = ler("Mês de referência (mm/aaaa, Enter = mês atual): ");
        if (texto.isBlank()) {
            mesReferencia = YearMonth.now();
            return;
        }
        try {
            mesReferencia = YearMonth.parse(texto, DateTimeFormatter.ofPattern("MM/yyyy"));
        } catch (DateTimeParseException e) {
            System.out.println("⚠ Formato inválido. Use mm/aaaa, ex: 03/2026.");
        }
    }

    private String ler(String rotulo) {
        System.out.print(rotulo);
        return entrada.nextLine().trim();
    }

    private String lerObrigatorio(String rotulo) {
        while (true) {
            String valor = ler(rotulo);
            if (!valor.isEmpty()) {
                return valor;
            }
            System.out.println("⚠ Preenchimento obrigatório.");
        }
    }

    private String lerOuPadrao(String rotulo, String padrao) {
        String valor = ler(rotulo);
        return valor.isEmpty() ? padrao : valor;
    }

    private BigDecimal lerValor(String rotulo, BigDecimal padrao) {
        while (true) {
            String texto = ler(rotulo);
            if (texto.isEmpty() && padrao != null) {
                return padrao;
            }
            try {
                BigDecimal valor = new BigDecimal(texto.replace(',', '.'));
                if (valor.signum() > 0) {
                    return valor;
                }
                System.out.println("⚠ O valor deve ser maior que zero.");
            } catch (NumberFormatException e) {
                System.out.println("⚠ Valor inválido. Use números, ex: 1500 ou 1500,75.");
            }
        }
    }

    private LocalDate lerData(String rotulo, LocalDate padrao) {
        while (true) {
            String texto = ler(rotulo);
            if (texto.isEmpty() && padrao != null) {
                return padrao;
            }
            try {
                return LocalDate.parse(texto, FMT_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("⚠ Data inválida. Use dd/mm/aaaa, ex: 25/12/2026.");
            }
        }
    }

    private long lerLong(String rotulo) {
        while (true) {
            try {
                return Long.parseLong(ler(rotulo));
            } catch (NumberFormatException e) {
                System.out.println("⚠ Digite um número.");
            }
        }
    }

    private boolean confirmar(String rotulo) {
        return ler(rotulo).equalsIgnoreCase("s");
    }

    private String lerCategoria(TipoTransacao tipo) {
        String[] sugestoes = tipo == TipoTransacao.RECEITA ? CATEGORIAS_RECEITA : CATEGORIAS_DESPESA;
        System.out.println("Categorias: " + enumerar(sugestoes));
        String resposta = ler("Número da categoria (ou digite outra): ");
        try {
            int indice = Integer.parseInt(resposta);
            if (indice >= 1 && indice <= sugestoes.length) {
                return sugestoes[indice - 1];
            }
        } catch (NumberFormatException ignorado) {
            // cai para o texto digitado
        }
        return resposta.isEmpty() ? "Outros" : resposta;
    }

    private FormaPagamento lerFormaPagamento() {
        System.out.println("Formas: " + enumerarFormas());
        while (true) {
            String resposta = ler("Número da forma de pagamento: ");
            try {
                int indice = Integer.parseInt(resposta);
                FormaPagamento[] formas = FormaPagamento.values();
                if (indice >= 1 && indice <= formas.length) {
                    return formas[indice - 1];
                }
            } catch (NumberFormatException ignorado) {
                // tenta pelo nome
            }
            System.out.println("⚠ Escolha um número entre 1 e " + FormaPagamento.values().length + ".");
        }
    }

    private FormaPagamento lerForma(FormaPagamento padrao) {
        System.out.println("Formas: " + enumerarFormas());
        while (true) {
            String resposta = ler("Número da forma [" + padrao.getRotulo() + "] (Enter = manter): ");
            if (resposta.isEmpty()) {
                return padrao;
            }
            try {
                int indice = Integer.parseInt(resposta);
                FormaPagamento[] formas = FormaPagamento.values();
                if (indice >= 1 && indice <= formas.length) {
                    return formas[indice - 1];
                }
            } catch (NumberFormatException ignorado) {
                // número inválido
            }
            System.out.println("⚠ Escolha um número entre 1 e " + FormaPagamento.values().length + ".");
        }
    }

    private String enumerar(String[] itens) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < itens.length; i++) {
            texto.append("[").append(i + 1).append("] ").append(itens[i]);
            if (i < itens.length - 1) {
                texto.append("  ");
            }
        }
        return texto.toString();
    }

    private String enumerarFormas() {
        FormaPagamento[] formas = FormaPagamento.values();
        String[] rotulos = new String[formas.length];
        for (int i = 0; i < formas.length; i++) {
            rotulos[i] = formas[i].getRotulo();
        }
        return enumerar(rotulos);
    }

    private String nomeMes(YearMonth mes) {
        String nome = mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " " + mes.getYear();
    }

    private String moeda(BigDecimal valor) {
        return java.text.NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    /** Barra de progresso proporcional, ex: ████████░░░░. */
    private String barra(double percentual, int tamanho) {
        int preenchido = (int) Math.round(Math.min(Math.max(percentual, 0), 100) / 100.0 * tamanho);
        preenchido = Math.min(preenchido, tamanho);
        return "█".repeat(preenchido) + "░".repeat(tamanho - preenchido);
    }

    private String truncar(String texto, int tamanho) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho - 1) + "…";
    }
}
