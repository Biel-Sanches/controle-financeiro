package br.com.gsanches.financeiro;

import br.com.gsanches.financeiro.repository.CsvContaRepository;
import br.com.gsanches.financeiro.repository.CsvMetaRepository;
import br.com.gsanches.financeiro.repository.CsvTransacaoRepository;
import br.com.gsanches.financeiro.service.ContasService;
import br.com.gsanches.financeiro.service.FinanceiroService;
import br.com.gsanches.financeiro.service.MetasService;
import br.com.gsanches.financeiro.ui.ConsoleUI;

public class Main {

    public static void main(String[] args) {
        FinanceiroService financeiro = new FinanceiroService(new CsvTransacaoRepository());
        ContasService contas = new ContasService(new CsvContaRepository());
        MetasService metas = new MetasService(new CsvMetaRepository());

        new ConsoleUI(financeiro, contas, metas).executar();
    }
}
