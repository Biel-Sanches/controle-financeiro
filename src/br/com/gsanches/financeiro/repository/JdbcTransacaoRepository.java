package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.infra.Conexao;
import br.com.gsanches.financeiro.model.FormaPagamento;
import br.com.gsanches.financeiro.model.TipoTransacao;
import br.com.gsanches.financeiro.model.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistência das transações em banco de dados (MySQL via JDBC).
 * Mesmo contrato da versão CSV — o restante do sistema não sabe
 * qual implementação está em uso.
 */
public class JdbcTransacaoRepository implements TransacaoRepository {

    @Override
    public List<Transacao> buscarTodas() {
        String sql = """
                SELECT id, data, tipo, categoria, descricao, forma_pagamento, valor, observacao
                FROM transacao
                ORDER BY data DESC, id DESC
                """;
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Transacao> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar transações: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Transacao> buscarPorId(long id) {
        String sql = """
                SELECT id, data, tipo, categoria, descricao, forma_pagamento, valor, observacao
                FROM transacao WHERE id = ?
                """;
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar transação: " + e.getMessage(), e);
        }
    }

    @Override
    public Transacao salvar(Transacao t) {
        if (t.id() == 0) {
            return inserir(t);
        }
        return atualizar(t);
    }

    private Transacao inserir(Transacao t) {
        String sql = """
                INSERT INTO transacao (data, tipo, categoria, descricao, forma_pagamento, valor, observacao)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(ps, t);
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    return new Transacao(chaves.getLong(1), t.data(), t.tipo(), t.categoria(),
                            t.descricao(), t.formaPagamento(), t.valor(), t.observacao());
                }
            }
            return t;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar transação: " + e.getMessage(), e);
        }
    }

    private Transacao atualizar(Transacao t) {
        String sql = """
                UPDATE transacao
                SET data = ?, tipo = ?, categoria = ?, descricao = ?, forma_pagamento = ?, valor = ?, observacao = ?
                WHERE id = ?
                """;
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, t);
            ps.setLong(8, t.id());
            ps.executeUpdate();
            return t;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar transação: " + e.getMessage(), e);
        }
    }

    private void preencher(PreparedStatement ps, Transacao t) throws SQLException {
        ps.setDate(1, java.sql.Date.valueOf(t.data()));
        ps.setString(2, t.tipo().name());
        ps.setString(3, t.categoria());
        ps.setString(4, t.descricao());
        ps.setString(5, t.formaPagamento().name());
        ps.setBigDecimal(6, t.valor());
        ps.setString(7, t.observacao());
    }

    @Override
    public boolean remover(long id) {
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement("DELETE FROM transacao WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover transação: " + e.getMessage(), e);
        }
    }

    @Override
    public long proximoId() {
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM transacao");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao calcular próximo id: " + e.getMessage(), e);
        }
    }

    private Transacao mapear(ResultSet rs) throws SQLException {
        return new Transacao(
                rs.getLong("id"),
                rs.getDate("data").toLocalDate(),
                TipoTransacao.valueOf(rs.getString("tipo")),
                rs.getString("categoria"),
                rs.getString("descricao"),
                FormaPagamento.valueOf(rs.getString("forma_pagamento")),
                rs.getBigDecimal("valor"),
                rs.getString("observacao"));
    }
}
