package br.com.gsanches.financeiro.model;

public enum TipoConta {

    A_PAGAR("A pagar"),
    A_RECEBER("A receber");

    private final String rotulo;

    TipoConta(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
