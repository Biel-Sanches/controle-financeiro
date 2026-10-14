package br.com.gsanches.financeiro.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base de persistência em arquivo CSV (pasta data/).
 * Cada linha é um registro; o separador é ";".
 * Quando migrarmos para MySQL, novas implementações substituem estas
 * sem tocar no restante do sistema.
 */
public abstract class CsvRepository<T> {

    private final Path arquivo;
    private final List<T> itens = new ArrayList<>();

    protected CsvRepository(String nomeArquivo) {
        this.arquivo = Path.of("data", nomeArquivo);
        carregar();
    }

    protected abstract T fromLinha(String[] campos);

    protected abstract String toLinha(T item);

    protected abstract long idDe(T item);

    private void carregar() {
        if (!Files.exists(arquivo)) {
            return;
        }
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                if (linha.isBlank()) {
                    continue;
                }
                itens.add(fromLinha(linha.split(";", -1)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + arquivo, e);
        }
    }

    protected void persistir() {
        try {
            Files.createDirectories(arquivo.getParent());
            List<String> linhas = new ArrayList<>();
            for (T item : itens) {
                linhas.add(toLinha(item));
            }
            Files.write(arquivo, linhas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível salvar " + arquivo, e);
        }
    }

    public List<T> buscarTodas() {
        return new ArrayList<>(itens);
    }

    public Optional<T> buscarPorId(long id) {
        return itens.stream().filter(item -> idDe(item) == id).findFirst();
    }

    /** Insere (id novo) ou atualiza (id já existente). */
    public T salvar(T item) {
        for (int i = 0; i < itens.size(); i++) {
            if (idDe(itens.get(i)) == idDe(item)) {
                itens.set(i, item);
                persistir();
                return item;
            }
        }
        itens.add(item);
        persistir();
        return item;
    }

    public boolean remover(long id) {
        boolean removeu = itens.removeIf(item -> idDe(item) == id);
        if (removeu) {
            persistir();
        }
        return removeu;
    }

    public long proximoId() {
        return itens.stream().mapToLong(this::idDe).max().orElse(0) + 1;
    }

    /** Remove caracteres que quebrariam o formato do CSV. */
    protected static String limpar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }
}
