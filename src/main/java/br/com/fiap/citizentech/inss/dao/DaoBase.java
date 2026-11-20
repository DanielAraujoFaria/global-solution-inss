package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.db.BdLog;
import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base dos DAOs: concentra o uso de PreparedStatement (sempre com parâmetros, nunca concatenando
 * valores no SQL, o que evita injeção de SQL) e o mapeamento de erros.
 */
public abstract class DaoBase {
    private static final Pattern TABELA = Pattern.compile("(?i)\\b(?:INTO|UPDATE|FROM)\\s+([a-z_]+)");

    @FunctionalInterface
    protected interface Mapeador<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    protected final Connection con;

    protected DaoBase(Connection con) {
        this.con = con;
    }

    protected long inserir(String sql, Object... parametros) {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            definir(ps, parametros);
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                long id = chaves.getLong(1);
                BdLog.registrar("INSERT", tabela(sql), "id=" + id);
                return id;
            }
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha ao inserir: " + sql, e);
        }
    }

    protected int atualizar(String sql, Object... parametros) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            definir(ps, parametros);
            int linhas = ps.executeUpdate();
            BdLog.registrar(sql.trim().split("\\s+")[0].toUpperCase(), tabela(sql), linhas + " linha(s)");
            return linhas;
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha ao atualizar: " + sql, e);
        }
    }

    protected <T> List<T> consultar(String sql, Mapeador<T> mapeador, Object... parametros) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            definir(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapeador.mapear(rs));
                }
                BdLog.registrar("SELECT", tabela(sql), resultado.size() + " linha(s)");
                return resultado;
            }
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha ao consultar: " + sql, e);
        }
    }

    private static void definir(PreparedStatement ps, Object[] parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            Object valor = parametros[i];
            if (valor instanceof Enum<?> e) {
                valor = e.name();
            }
            ps.setObject(i + 1, valor);
        }
    }

    private static String tabela(String sql) {
        Matcher m = TABELA.matcher(sql);
        return m.find() ? m.group(1).toLowerCase() : "?";
    }

    protected static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, LocalDate.class);
    }

    protected static LocalDateTime dataHora(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, LocalDateTime.class);
    }

    /** Lê uma coluna BIGINT que pode ser nula. */
    protected static Long longOuNulo(ResultSet rs, String coluna) throws SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }
}
