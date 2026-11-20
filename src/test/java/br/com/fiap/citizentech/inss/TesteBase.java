package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.db.BancoDeDados;
import br.com.fiap.citizentech.inss.db.Conexao;
import org.junit.After;
import org.junit.Before;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/** Cada teste ganha um banco H2 em memória novo, já com o esquema e os dados fictícios. */
public abstract class TesteBase {
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();
    protected Connection con;

    @Before
    public void abrirBanco() {
        con = Conexao.emMemoria("teste" + SEQUENCIA.incrementAndGet());
        BancoDeDados.prepararSeNecessario(con);
    }

    @After
    public void fecharBanco() throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("SHUTDOWN");
        } catch (SQLException ignorada) {
            // já fechado
        }
        con.close();
    }

    protected long contar(String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    protected String texto(String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    protected long idExigenciaDoPedido(String protocolo) throws SQLException {
        return contar("SELECT e.id FROM exigencia e JOIN pedido p ON p.id = e.pedido_id WHERE p.protocolo = ?", protocolo);
    }

    protected String statusDaExigencia(String protocoloDoPedido) throws SQLException {
        return texto("SELECT e.status FROM exigencia e JOIN pedido p ON p.id = e.pedido_id WHERE p.protocolo = ?",
                protocoloDoPedido);
    }
}
