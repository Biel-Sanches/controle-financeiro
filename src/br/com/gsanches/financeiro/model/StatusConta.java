package br.com.gsanches.financeiro.model;

public enum StatusConta {

    PENDENTE("Pendente"),
    PAGA("Paga"),
    RECEBIDA("Recebida");

    private final String rotulo;

    StatusConta(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
