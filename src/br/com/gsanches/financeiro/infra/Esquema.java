package br.com.gsanches.financeiro.infra;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Criação das tabelas (executado na inicialização do servidor web).
 * SQL padrão, compatível com MySQL e H2.
 */
public final class Esquema {

    private Esquema() {
    }

    public static void criarTabelas() {
        String[] comandos = {
            """
            CREATE TABLE IF NOT EXISTS transacao (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                data DATE NOT NULL,
                tipo VARCHAR(10) NOT NULL,
                categoria VARCHAR(60) NOT NULL,
                descricao VARCHAR(120) NOT NULL,
                forma_pagamento VARCHAR(20) NOT NULL,
                valor DECIMAL(12,2) NOT NULL,
                observacao VARCHAR(200)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS conta (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                nome VARCHAR(120) NOT NULL,
                tipo VARCHAR(10) NOT NULL,
                valor DECIMAL(12,2) NOT NULL,
                vencimento DATE NOT NULL,
                status VARCHAR(10) NOT NULL,
                data_pagamento DATE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS meta (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                nome VARCHAR(120) NOT NULL,
                valor_objetivo DECIMAL(12,2) NOT NULL,
                valor_acumulado DECIMAL(12,2) NOT NULL DEFAULT 0
            )
            """
        };
        try (Connection con = Conexao.getConexao(); Statement st = con.createStatement()) {
            for (String sql : comandos) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao criar tabelas: " + e.getMessage(), e);
        }
    }
}
