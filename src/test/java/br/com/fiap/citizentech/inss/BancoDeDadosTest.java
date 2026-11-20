package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.dao.ExigenciaDao;
import br.com.fiap.citizentech.inss.db.BancoDeDados;
import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BancoDeDadosTest extends TesteBase {

    @Test
    public void esquemaEDadosFicticiosForamCarregados() throws SQLException {
        assertEquals(11, contar("SELECT COUNT(*) FROM cidadao"));
        assertEquals(5, contar("SELECT COUNT(*) FROM tipo_exigencia"));
        assertEquals(11, contar("SELECT COUNT(*) FROM documento_modelo"));
        assertEquals(9, contar("SELECT COUNT(*) FROM pedido"));
        assertEquals(7, contar("SELECT COUNT(*) FROM exigencia"));
    }

    @Test
    public void numeroDoPerfilNoLoginCorrespondeAoSeed() throws SQLException {
        // Os cenários de validação dependem desta ordem.
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 1").startsWith("Maria"));
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 2").startsWith("Carlos"));
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 3").startsWith("Ana"));
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 4").startsWith("Rita"));
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 5").startsWith("Marta"));
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 6").startsWith("Raimundo"));
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 7").startsWith("Lúcia"));
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 8").startsWith("José"));
        assertTrue(texto("SELECT nome_ficticio FROM cidadao WHERE id = 9").startsWith("Beatriz"));
    }

    @Test
    public void prepararDeNovoNaoDuplicaDados() throws SQLException {
        BancoDeDados.prepararSeNecessario(con);
        assertEquals(11, contar("SELECT COUNT(*) FROM cidadao"));
    }

    @Test
    public void reiniciarVoltaAoEstadoInicial() throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("UPDATE exigencia SET status = 'CUMPRIDA'");
        }
        BancoDeDados.reiniciar(con);
        assertEquals(0, contar("SELECT COUNT(*) FROM exigencia WHERE status = 'CUMPRIDA'"));
    }

    @Test
    public void restaurarPedidosVoltaOEstadoMasPreservaOsDadosColetados() throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("INSERT INTO conversa (origem, inicio) VALUES ('TESTE_USUARIO', CURRENT_TIMESTAMP)");
            st.executeUpdate("UPDATE exigencia SET status = 'CUMPRIDA', data_cumprimento = CURRENT_DATE, "
                    + "lembrete_consentido = TRUE, lembrete_dias_antes = 3");
            st.executeUpdate("INSERT INTO item_checklist (exigencia_id, nome, orientacao, obrigatorio) VALUES (1, 'x', 'y', TRUE)");
        }
        BancoDeDados.restaurarPedidos(con);
        assertEquals(7, contar("SELECT COUNT(*) FROM exigencia WHERE status = 'ABERTA' AND data_cumprimento IS NULL "
                + "AND lembrete_consentido = FALSE"));
        assertEquals(0, contar("SELECT COUNT(*) FROM item_checklist"));
        assertEquals("a conversa coletada foi preservada", 1, contar("SELECT COUNT(*) FROM conversa"));
    }

    @Test
    public void divisorDeScriptIgnoraComentariosEPontoEVirgulaEmTexto() {
        List<String> comandos = BancoDeDados.dividirComandos(
                "SELECT 1; -- comentário com ; dentro\nINSERT INTO t VALUES ('a;b', 'it''s');\n  \n");
        assertEquals(2, comandos.size());
        assertEquals("SELECT 1", comandos.get(0));
        assertTrue(comandos.get(1).contains("'a;b'"));
        assertTrue(comandos.get(1).contains("'it''s'"));
    }

    @Test
    public void restricoesDoBancoBarramDadosInvalidos() {
        // prazo anterior à emissão
        ExigenciaDao dao = new ExigenciaDao(con);
        assertThrows(ErroDeBancoException.class,
                () -> dao.inserir(1, 1, "texto", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1)));
        // status inválido
        assertThrows(SQLException.class, () -> {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("UPDATE pedido SET status = 'INVENTADO'");
            }
        });
        // avaliação fora de 1..5
        assertThrows(SQLException.class, () -> {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("INSERT INTO conversa (origem, inicio) VALUES ('SIMULACAO', CURRENT_TIMESTAMP)");
                st.executeUpdate("INSERT INTO avaliacao (conversa_id, clareza, satisfacao, sabe_proximo_passo, avaliada_em) "
                        + "VALUES (1, 9, 3, TRUE, CURRENT_TIMESTAMP)");
            }
        });
    }
}
