package br.com.gsanches.financeiro.model;

public enum TipoTransacao {

    RECEITA("Receita"),
    DESPESA("Despesa");

    private final String rotulo;

    TipoTransacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
