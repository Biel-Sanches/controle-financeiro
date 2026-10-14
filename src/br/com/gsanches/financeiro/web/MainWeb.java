package br.com.gsanches.financeiro.web;

import br.com.gsanches.financeiro.infra.Esquema;
import br.com.gsanches.financeiro.model.Conta;
import br.com.gsanches.financeiro.model.Meta;
import br.com.gsanches.financeiro.model.TipoTransacao;
import br.com.gsanches.financeiro.model.Transacao;
import br.com.gsanches.financeiro.repository.JdbcContaRepository;
import br.com.gsanches.financeiro.repository.JdbcMetaRepository;
import br.com.gsanches.financeiro.repository.JdbcTransacaoRepository;
import br.com.gsanches.financeiro.service.ContasService;
import br.com.gsanches.financeiro.service.FinanceiroService;
import br.com.gsanches.financeiro.service.MetasService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor web do Controle Financeiro: serve a interface (pasta web/)
 * e a API JSON em /api/. Usa apenas o JDK + Gson + MySQL Connector.
 */
public class MainWeb {

    private static final int PORTA = 8080;
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(java.time.LocalDate.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDate>) (data, tipo, ctx) ->
                            new com.google.gson.JsonPrimitive(data.toString()))
            .registerTypeAdapter(java.time.LocalDate.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDate>) (json, tipo, ctx) ->
                            java.time.LocalDate.parse(json.getAsString()))
            .create();

    private final FinanceiroService financeiro;
    private final ContasService contas;
    private final MetasService metas;

    public MainWeb(FinanceiroService financeiro, ContasService contas, MetasService metas) {
        this.financeiro = financeiro;
        this.contas = contas;
        this.metas = metas;
    }

    public static void main(String[] args) throws IOException {
        Esquema.criarTabelas();

        MainWeb app = new MainWeb(
                new FinanceiroService(new JdbcTransacaoRepository()),
                new ContasService(new JdbcContaRepository()),
                new MetasService(new JdbcMetaRepository()));

        HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA), 0);
        servidor.createContext("/api/", app::servirApi);
        servidor.createContext("/", app::servirEstatico);
        servidor.setExecutor(Executors.newFixedThreadPool(8));
        servidor.start();
        System.out.println("Controle Financeiro no ar: http://localhost:" + PORTA);
    }

    // ===== API =====

    private void servirApi(HttpExchange troca) throws IOException {
        try {
            String metodo = troca.getRequestMethod();
            String caminho = troca.getRequestURI().getPath(); // ex.: /api/transacoes/12
            Map<String, String> consulta = lerConsulta(troca);
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            String[] partes = caminho.substring("/api/".length()).split("/");
            String recurso = partes[0];
            String idOuAcao = partes.length > 1 ? partes[1] : "";
            String acao = partes.length > 2 ? partes[2] : "";

            Object resposta = rotear(metodo, recurso, idOuAcao, acao, consulta, corpo);
            enviarJson(troca, resposta);
        } catch (IllegalArgumentException e) {
            enviarErro(troca, 400, e.getMessage());
        } catch (Exception e) {
            enviarErro(troca, 500, e.getMessage());
        }
    }

    private Object rotear(String metodo, String recurso, String idOuAcao, String acao,
                          Map<String, String> consulta, String corpo) {
        switch (recurso) {
            case "resumo" -> {
                return montarResumo(mesDaConsulta(consulta));
            }
            case "transacoes" -> {
                if (metodo.equals("GET")) {
                    return consultarTransacoes(consulta);
                }
                if (metodo.equals("POST")) {
                    Transacao recebida = GSON.fromJson(corpo, Transacao.class);
                    return financeiro.salvar(recebida);
                }
                long id = Long.parseLong(idOuAcao);
                if (metodo.equals("PUT")) {
                    Transacao recebida = GSON.fromJson(corpo, Transacao.class);
                    return financeiro.salvar(new Transacao(id, recebida.data(), recebida.tipo(),
                            recebida.categoria(), recebida.descricao(), recebida.formaPagamento(),
                            recebida.valor(), recebida.observacao()));
                }
                if (metodo.equals("DELETE")) {
                    exigir(financeiro.remover(id), "Transação não encontrada.");
                    return ok();
                }
            }
            case "contas" -> {
                if (metodo.equals("GET")) {
                    return contas.buscarTodas();
                }
                if (metodo.equals("POST") && idOuAcao.isEmpty()) {
                    Conta recebida = GSON.fromJson(corpo, Conta.class);
                    return contas.criar(recebida.nome(), recebida.tipo(), recebida.valor(), recebida.vencimento());
                }
                long id = Long.parseLong(idOuAcao);
                if (metodo.equals("POST") && acao.equals("pagar")) {
                    JsonObject json = GSON.fromJson(corpo, JsonObject.class);
                    LocalDate data = json.has("data") && !json.get("data").isJsonNull()
                            ? LocalDate.parse(json.get("data").getAsString())
                            : LocalDate.now();
                    return contas.resolver(id, data);
                }
                if (metodo.equals("DELETE")) {
                    exigir(contas.remover(id), "Conta não encontrada.");
                    return ok();
                }
            }
            case "metas" -> {
                if (metodo.equals("GET")) {
                    return metas.buscarTodas();
                }
                if (metodo.equals("POST") && idOuAcao.isEmpty()) {
                    Meta recebida = GSON.fromJson(corpo, Meta.class);
                    return metas.criar(recebida.nome(), recebida.valorObjetivo());
                }
                long id = Long.parseLong(idOuAcao);
                if (metodo.equals("POST") && acao.equals("depositar")) {
                    JsonObject json = GSON.fromJson(corpo, JsonObject.class);
                    BigDecimal valor = json.get("valor").getAsBigDecimal();
                    return metas.depositar(id, valor);
                }
                if (metodo.equals("DELETE")) {
                    exigir(metas.remover(id), "Meta não encontrada.");
                    return ok();
                }
            }
            case "exportar" -> {
                return csvConteudoDaConsulta(consulta);
            }
            default -> {
                throw new IllegalArgumentException("Recurso desconhecido: " + recurso);
            }
        }
        throw new IllegalArgumentException("Combinação inválida: " + metodo + " " + recurso);
    }

    private JsonObject montarResumo(YearMonth mes) {
        List<Transacao> doMes = financeiro.doMes(mes);
        BigDecimal receitas = financeiro.total(TipoTransacao.RECEITA, doMes);
        BigDecimal despesas = financeiro.total(TipoTransacao.DESPESA, doMes);

        JsonObject resumo = new JsonObject();
        resumo.addProperty("mes", mes.toString());
        resumo.addProperty("saldoGeral", financeiro.saldoGeral());
        resumo.addProperty("receitas", receitas);
        resumo.addProperty("despesas", despesas);
        resumo.addProperty("resultado", receitas.subtract(despesas));

        JsonObject pendentes = new JsonObject();
        List<Conta> pendentesLista = contas.pendentes();
        pendentes.addProperty("quantidade", pendentesLista.size());
        pendentes.addProperty("total", contas.totalPendente());
        pendentes.addProperty("vencidas", contas.vencidas().size());
        resumo.add("pendentes", pendentes);

        var porCategoria = financeiro.gastosPorCategoria(doMes);
        var categorias = new com.google.gson.JsonArray();
        for (var gasto : porCategoria.entrySet()) {
            JsonObject item = new JsonObject();
            item.addProperty("categoria", gasto.getKey());
            item.addProperty("total", gasto.getValue());
            if (despesas.signum() > 0) {
                item.addProperty("percentual", gasto.getValue()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(despesas, 1, RoundingMode.HALF_UP));
            } else {
                item.addProperty("percentual", BigDecimal.ZERO);
            }
            categorias.add(item);
        }
        resumo.add("porCategoria", categorias);

        var evolucao = new com.google.gson.JsonArray();
        for (var item : financeiro.evolucaoDespesas(mes, 6).entrySet()) {
            JsonObject linha = new JsonObject();
            linha.addProperty("mes", item.getKey().toString());
            linha.addProperty("total", item.getValue());
            evolucao.add(linha);
        }
        resumo.add("evolucao", evolucao);
        return resumo;
    }

    private List<Transacao> consultarTransacoes(Map<String, String> consulta) {
        if (consulta.containsKey("mes")) {
            return financeiro.doMes(YearMonth.parse(consulta.get("mes")));
        }
        LocalDate inicio = LocalDate.parse(consulta.get("inicio"));
        LocalDate fim = LocalDate.parse(consulta.get("fim"));
        return financeiro.doPeriodo(inicio, fim);
    }

    private String csvConteudoDaConsulta(Map<String, String> consulta) {
        LocalDate inicio;
        LocalDate fim;
        if (consulta.containsKey("mes")) {
            YearMonth mes = YearMonth.parse(consulta.get("mes"));
            inicio = mes.atDay(1);
            fim = mes.atEndOfMonth();
        } else {
            inicio = LocalDate.parse(consulta.get("inicio"));
            fim = LocalDate.parse(consulta.get("fim"));
        }
        return financeiro.csvConteudo(inicio, fim);
    }

    private YearMonth mesDaConsulta(Map<String, String> consulta) {
        return consulta.containsKey("mes") ? YearMonth.parse(consulta.get("mes")) : YearMonth.now();
    }

    private JsonObject ok() {
        JsonObject json = new JsonObject();
        json.addProperty("ok", true);
        return json;
    }

    private void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    // ===== Arquivos estáticos (web/) =====

    private void servirEstatico(HttpExchange troca) throws IOException {
        String caminho = troca.getRequestURI().getPath();
        if (caminho.equals("/")) {
            caminho = "/index.html";
        }
        Path arquivo = Path.of("web", caminho.substring(1)).normalize();
        if (!arquivo.startsWith("web") || !Files.isRegularFile(arquivo)) {
            enviarTexto(troca, 404, "text/plain; charset=utf-8", "404 — não encontrado");
            return;
        }
        byte[] bytes = Files.readAllBytes(arquivo);
        troca.getResponseHeaders().set("Content-Type", tipoDeConteudo(arquivo.toString()));
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private String tipoDeConteudo(String nome) {
        if (nome.endsWith(".html")) return "text/html; charset=utf-8";
        if (nome.endsWith(".css")) return "text/css; charset=utf-8";
        if (nome.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (nome.endsWith(".svg")) return "image/svg+xml";
        if (nome.endsWith(".png")) return "image/png";
        if (nome.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    // ===== Respostas =====

    private void enviarJson(HttpExchange troca, Object resposta) throws IOException {
        if (resposta instanceof String csv && csv.startsWith("\uFEFF")) {
            // exportação de CSV: devolve como download
            byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
            troca.getResponseHeaders().set("Content-Disposition", "attachment; filename=relatorio.csv");
            troca.sendResponseHeaders(200, bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
            return;
        }
        byte[] bytes = GSON.toJson(resposta).getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private void enviarErro(HttpExchange troca, int codigo, String mensagem) throws IOException {
        JsonObject erro = new JsonObject();
        erro.addProperty("erro", mensagem == null ? "erro interno" : mensagem);
        byte[] bytes = GSON.toJson(erro).getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private void enviarTexto(HttpExchange troca, int codigo, String tipo, String texto)
            throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private Map<String, String> lerConsulta(HttpExchange troca) {
        Map<String, String> parametros = new HashMap<>();
        String consulta = troca.getRequestURI().getRawQuery();
        if (consulta == null || consulta.isBlank()) {
            return parametros;
        }
        for (String par : consulta.split("&")) {
            String[] chaveValor = par.split("=", 2);
            String chave = URLDecoder.decode(chaveValor[0], StandardCharsets.UTF_8);
            String valor = chaveValor.length > 1 ? URLDecoder.decode(chaveValor[1], StandardCharsets.UTF_8) : "";
            parametros.put(chave, valor);
        }
        return parametros;
    }
}
