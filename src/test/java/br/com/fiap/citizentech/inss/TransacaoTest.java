package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.dao.ExigenciaDao;
import br.com.fiap.citizentech.inss.db.Transacao;
import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;
import br.com.fiap.citizentech.inss.modelo.StatusExigencia;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Atomicidade: ou tudo é gravado, ou nada. */
public class TransacaoTest extends TesteBase {

    @Test
    public void commitGravaTudo() throws SQLException {
        ExigenciaDao dao = new ExigenciaDao(con);
        long a = idExigenciaDoPedido("FICT-0001");
        long b = idExigenciaDoPedido("FICT-0002");
        Transacao.executar(con, () -> {
            dao.atualizarStatus(a, StatusExigencia.CUMPRIDA, LocalDate.now());
            dao.atualizarStatus(b, StatusExigencia.CUMPRIDA, LocalDate.now());
        });
        assertEquals("CUMPRIDA", statusDaExigencia("FICT-0001"));
        assertEquals("CUMPRIDA", statusDaExigencia("FICT-0002"));
        assertTrue("auto-commit restaurado", con.getAutoCommit());
    }

    @Test
    public void falhaNoMeioDesfazTudo() throws SQLException {
        ExigenciaDao dao = new ExigenciaDao(con);
        long a = idExigenciaDoPedido("FICT-0001");
        long b = idExigenciaDoPedido("FICT-0002");
        assertThrows(IllegalStateException.class, () -> Transacao.executar(con, () -> {
            dao.atualizarStatus(a, StatusExigencia.CUMPRIDA, LocalDate.now());
            dao.atualizarStatus(b, StatusExigencia.CUMPRIDA, LocalDate.now());
            throw new IllegalStateException("falha no meio da operação");
        }));
        assertEquals("ABERTA", statusDaExigencia("FICT-0001"));
        assertEquals("ABERTA", statusDaExigencia("FICT-0002"));
        assertTrue("auto-commit restaurado mesmo após falha", con.getAutoCommit());
    }

    @Test
    public void excecaoVerificadaViraErroDeBancoComRollback() throws SQLException {
        ExigenciaDao dao = new ExigenciaDao(con);
        long a = idExigenciaDoPedido("FICT-0001");
        assertThrows(ErroDeBancoException.class, () -> Transacao.executar(con, () -> {
            dao.atualizarStatus(a, StatusExigencia.CUMPRIDA, LocalDate.now());
            throw new SQLException("erro de SQL simulado");
        }));
        assertEquals("ABERTA", statusDaExigencia("FICT-0001"));
    }
}
