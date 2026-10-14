package br.com.gsanches.financeiro.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Conexão com o banco de dados. Lê as configurações de db.properties
 * (arquivo local, fora do versionamento); sem ele, usa os padrões
 * de desenvolvimento local.
 */
public final class Conexao {

    private static final String URL_PADRAO = "jdbc:mysql://localhost:3306/financeiro";
    private static final String USUARIO_PADRAO = "financeiro";
    private static final String SENHA_PADRAO = "financeiro123";

    private static final String url;
    private static final String usuario;
    private static final String senha;

    static {
        String u = URL_PADRAO;
        String s = USUARIO_PADRAO;
        String p = SENHA_PADRAO;
        Path config = Path.of("db.properties");
        if (Files.exists(config)) {
            try {
                Properties props = new Properties();
                props.load(Files.newBufferedReader(config, java.nio.charset.StandardCharsets.UTF_8));
                u = props.getProperty("db.url", u);
                s = props.getProperty("db.usuario", s);
                p = props.getProperty("db.senha", p);
            } catch (IOException e) {
                throw new IllegalStateException("Não foi possível ler db.properties", e);
            }
        }
        url = u;
        usuario = s;
        senha = p;
    }

    private Conexao() {
    }

    public static Connection getConexao() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    public static String url() {
        return url;
    }
}
