package br.com.gsanches.financeiro.model;

public enum FormaPagamento {

    PIX("Pix"),
    DINHEIRO("Dinheiro"),
    CARTAO_CREDITO("Cartão de crédito"),
    CARTAO_DEBITO("Cartão de débito"),
    TRANSFERENCIA("Transferência"),
    BOLETO("Boleto"),
    OUTRO("Outro");

    private final String rotulo;

    FormaPagamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
