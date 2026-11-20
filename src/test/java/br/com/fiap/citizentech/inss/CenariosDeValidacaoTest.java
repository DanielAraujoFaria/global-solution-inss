package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.dao.ConversaDao;
import br.com.fiap.citizentech.inss.modelo.ConversaRegistro;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.validacao.CenariosDeValidacao;
import br.com.fiap.citizentech.inss.validacao.CenariosDeValidacao.ResultadoCenario;
import org.junit.Test;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Executa os 19 cenários de ponta a ponta (assistente + banco) e confere o resultado de cada um. */
public class CenariosDeValidacaoTest extends TesteBase {

    @Test
    public void todosOsCenariosPassam() {
        List<ResultadoCenario> resultados = CenariosDeValidacao.executarTodos(con, Clock.systemDefaultZone());
        StringBuilder falhas = new StringBuilder();
        for (ResultadoCenario r : resultados) {
            if (!r.ok()) {
                falhas.append(r.cenario().id()).append(" - ").append(r.cenario().descricao())
                        .append(": ").append(r.falhas()).append('\n');
            }
        }
        assertEquals("cenários com falha:\n" + falhas, 0, falhas.length());
        assertEquals(19, resultados.size());
    }

    @Test
    public void osCenariosCobremTodosOsDesfechosEMotivosDeEncaminhamento() {
        Set<Desfecho> desfechos = new HashSet<>();
        Set<Object> motivos = new HashSet<>();
        CenariosDeValidacao.todos().forEach(c -> {
            desfechos.add(c.desfechoEsperado());
            if (c.motivoEsperado() != null) {
                motivos.add(c.motivoEsperado());
            }
        });
        assertEquals("todos os desfechos exceto EM_ANDAMENTO", Desfecho.values().length - 1, desfechos.size());
        assertEquals("todos os motivos de encaminhamento", br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento.values().length,
                motivos.size());
    }

    @Test
    public void todoEncaminhamentoGravadoVaiParaODestinoOficialDoMotivo() {
        CenariosDeValidacao.executarTodos(con, Clock.systemDefaultZone());
        int encaminhadas = 0;
        for (ConversaRegistro c : new ConversaDao(con).listarTodas()) {
            if (c.desfecho() == Desfecho.ENCAMINHADA) {
                encaminhadas++;
                assertEquals("conversa " + c.id(), c.motivoEncaminhamento().destino(), c.encaminhamento());
            } else {
                assertTrue("conversa " + c.id() + " não deveria ter destino", c.encaminhamento() == null);
            }
        }
        assertEquals(11, encaminhadas);
    }
}
