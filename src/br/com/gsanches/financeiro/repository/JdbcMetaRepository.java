package br.com.gsanches.financeiro.repository;

import br.com.gsanches.financeiro.infra.Conexao;
import br.com.gsanches.financeiro.model.Meta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persistência das metas financeiras em MySQL via JDBC. */
public class JdbcMetaRepository implements MetaRepository {

    @Override
    public List<Meta> buscarTodas() {
        String sql = "SELECT id, nome, valor_objetivo, valor_acumulado FROM meta ORDER BY id";
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Meta> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar metas: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Meta> buscarPorId(long id) {
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, nome, valor_objetivo, valor_acumulado FROM meta WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar meta: " + e.getMessage(), e);
        }
    }

    @Override
    public Meta salvar(Meta m) {
        if (m.id() == 0) {
            return inserir(m);
        }
        return atualizar(m);
    }

    private Meta inserir(Meta m) {
        String sql = "INSERT INTO meta (nome, valor_objetivo, valor_acumulado) VALUES (?, ?, ?)";
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.nome());
            ps.setBigDecimal(2, m.valorObjetivo());
            ps.setBigDecimal(3, m.valorAcumulado());
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    return new Meta(chaves.getLong(1), m.nome(), m.valorObjetivo(), m.valorAcumulado());
                }
            }
            return m;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar meta: " + e.getMessage(), e);
        }
    }

    private Meta atualizar(Meta m) {
        String sql = "UPDATE meta SET nome = ?, valor_objetivo = ?, valor_acumulado = ? WHERE id = ?";
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.nome());
            ps.setBigDecimal(2, m.valorObjetivo());
            ps.setBigDecimal(3, m.valorAcumulado());
            ps.setLong(4, m.id());
            ps.executeUpdate();
            return m;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar meta: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean remover(long id) {
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement("DELETE FROM meta WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover meta: " + e.getMessage(), e);
        }
    }

    @Override
    public long proximoId() {
        try (Connection con = Conexao.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT COALESCE(MAX(id), 0) + 1 FROM meta");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao calcular próximo id: " + e.getMessage(), e);
        }
    }

    private Meta mapear(ResultSet rs) throws SQLException {
        return new Meta(rs.getLong("id"), rs.getString("nome"),
                rs.getBigDecimal("valor_objetivo"), rs.getBigDecimal("valor_acumulado"));
    }
}
