package br.com.gsanches.financeiro.service;

import br.com.gsanches.financeiro.model.Conta;
import br.com.gsanches.financeiro.model.StatusConta;
import br.com.gsanches.financeiro.model.TipoConta;
import br.com.gsanches.financeiro.repository.ContaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Regras para contas a pagar e a receber:
 * vencimentos, avisos de atraso e registro de pagamento.
 */
public class ContasService {

    private final ContaRepository contas;

    public ContasService(ContaRepository contas) {
        this.contas = contas;
    }

    /** Cria uma conta nova (o repositório gera o id, status pendente). */
    public Conta criar(String nome, TipoConta tipo, BigDecimal valor, LocalDate vencimento) {
        Conta nova = new Conta(0, nome, tipo, valor, vencimento,
                StatusConta.PENDENTE, null);
        return salvar(nova);
    }

    public Conta salvar(Conta conta) {
        if (conta.valor() == null || conta.valor().signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        return contas.salvar(conta);
    }

    public boolean remover(long id) {
        return contas.remover(id);
    }

    /** Marca como paga (conta a pagar) ou recebida (conta a receber). */
    public Conta resolver(long id, LocalDate dataPagamento) {
        Conta conta = contas.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta " + id + " não encontrada."));
        StatusConta novoStatus = conta.tipo() == TipoConta.A_PAGAR
                ? StatusConta.PAGA
                : StatusConta.RECEBIDA;
        return contas.salvar(conta.resolver(novoStatus, dataPagamento));
    }

    public List<Conta> buscarTodas() {
        return contas.buscarTodas().stream()
                .sorted(Comparator.comparing(Conta::vencimento).thenComparing(Conta::id))
                .collect(Collectors.toList());
    }

    public List<Conta> pendentes() {
        return buscarTodas().stream()
                .filter(Conta::isPendente)
                .collect(Collectors.toList());
    }

    public List<Conta> vencidas() {
        return pendentes().stream()
                .filter(c -> c.diasAtraso() > 0)
                .collect(Collectors.toList());
    }

    public BigDecimal totalPendente() {
        return pendentes().stream()
                .map(Conta::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
