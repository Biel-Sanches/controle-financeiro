package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.infra.Conexao;
import br.com.gsanches.financeiro.model.Conta;
import br.com.gsanches.financeiro.model.StatusConta;
import br.com.gsanches.financeiro.model.TipoConta;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persistência das contas a pagar/receber em MySQL via JDBC. */
public class JdbcContaRepository implements ContaRepository {

    @Override
    public List<Conta> buscarTodas() {
        String sql = """
                SELECT id, nome, tipo, valor, vencimento, status, data_pagamento
                FROM conta ORDER BY vencimento, id
                """;
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Conta> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar contas: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Conta> buscarPorId(long id) {
        String sql = "SELECT id, nome, tipo, valor, vencimento, status, data_pagamento FROM conta WHERE id = ?";
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar conta: " + e.getMessage(), e);
        }
    }

    @Override
    public Conta salvar(Conta c) {
        if (c.id() == 0) {
            return inserir(c);
        }
        return atualizar(c);
    }

    private Conta inserir(Conta c) {
        String sql = """
                INSERT INTO conta (nome, tipo, valor, vencimento, status, data_pagamento)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(ps, c);
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    return new Conta(chaves.getLong(1), c.nome(), c.tipo(), c.valor(),
                            c.vencimento(), c.status(), c.dataPagamento());
                }
            }
            return c;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar conta: " + e.getMessage(), e);
        }
    }

    private Conta atualizar(Conta c) {
        String sql = """
                UPDATE conta SET nome = ?, tipo = ?, valor = ?, vencimento = ?, status = ?, data_pagamento = ?
                WHERE id = ?
                """;
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, c);
            ps.setLong(7, c.id());
            ps.executeUpdate();
            return c;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar conta: " + e.getMessage(), e);
        }
    }

    private void preencher(PreparedStatement ps, Conta c) throws SQLException {
        ps.setString(1, c.nome());
        ps.setString(2, c.tipo().name());
        ps.setBigDecimal(3, c.valor());
        ps.setDate(4, Date.valueOf(c.vencimento()));
        ps.setString(5, c.status().name());
        ps.setDate(6, c.dataPagamento() == null ? null : Date.valueOf(c.dataPagamento()));
    }

    @Override
    public boolean remover(long id) {
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement("DELETE FROM conta WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover conta: " + e.getMessage(), e);
        }
    }

    @Override
    public long proximoId() {
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM conta");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao calcular próximo id: " + e.getMessage(), e);
        }
    }

    private Conta mapear(ResultSet rs) throws SQLException {
        Date dataPagamento = rs.getDate("data_pagamento");
        return new Conta(
                rs.getLong("id"),
                rs.getString("nome"),
                TipoConta.valueOf(rs.getString("tipo")),
                rs.getBigDecimal("valor"),
                rs.getDate("vencimento").toLocalDate(),
                StatusConta.valueOf(rs.getString("status")),
                dataPagamento == null ? null : dataPagamento.toLocalDate());
    }
}
