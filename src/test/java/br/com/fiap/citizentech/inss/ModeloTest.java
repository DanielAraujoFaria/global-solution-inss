package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.modelo.Avaliacao;
import br.com.fiap.citizentech.inss.modelo.Checklist;
import br.com.fiap.citizentech.inss.modelo.Exigencia;
import br.com.fiap.citizentech.inss.modelo.ItemChecklist;
import br.com.fiap.citizentech.inss.modelo.StatusExigencia;
import br.com.fiap.citizentech.inss.modelo.StatusItem;
import br.com.fiap.citizentech.inss.modelo.TipoExigencia;
import br.com.fiap.citizentech.inss.servico.PoliticaDeLembrete;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Regras de prazo, checklist, lembrete e validação de notas. */
public class ModeloTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 18);

    private static Exigencia exigenciaComPrazoEm(int dias) {
        TipoExigencia tipo = new TipoExigencia(1L, "X", "t", "o", "e", 30, false);
        return new Exigencia(1L, 1L, tipo, "texto", HOJE.minusDays(5), HOJE.plusDays(dias), StatusExigencia.ABERTA, null);
    }

    @Test
    public void diasRestantesEVencimento() {
        assertEquals(4, exigenciaComPrazoEm(4).diasRestantes(HOJE));
        assertFalse("vence hoje ainda não está vencida", exigenciaComPrazoEm(0).estaVencida(HOJE));
        assertTrue("venceu ontem", exigenciaComPrazoEm(-1).estaVencida(HOJE));
        assertEquals(-15, exigenciaComPrazoEm(-15).diasRestantes(HOJE));
    }

    @Test
    public void limiteDeUrgenciaEmCincoDias() {
        assertTrue(exigenciaComPrazoEm(0).ehUrgente(HOJE));
        assertTrue(exigenciaComPrazoEm(5).ehUrgente(HOJE));
        assertFalse(exigenciaComPrazoEm(6).ehUrgente(HOJE));
        assertFalse("vencida não é 'urgente', é vencida", exigenciaComPrazoEm(-1).ehUrgente(HOJE));
    }

    @Test
    public void lembreteNuncaPassaDeTresDiasENaoSeOfereceSemTempoUtil() {
        assertEquals(Optional.empty(), PoliticaDeLembrete.diasParaLembrete(-3));
        assertEquals(Optional.empty(), PoliticaDeLembrete.diasParaLembrete(0));
        assertEquals(Optional.empty(), PoliticaDeLembrete.diasParaLembrete(1));
        assertEquals(Optional.of(1), PoliticaDeLembrete.diasParaLembrete(2));
        assertEquals(Optional.of(2), PoliticaDeLembrete.diasParaLembrete(3));
        assertEquals(Optional.of(3), PoliticaDeLembrete.diasParaLembrete(4));
        assertEquals(Optional.of(3), PoliticaDeLembrete.diasParaLembrete(30));
    }

    private static ItemChecklist item(boolean obrigatorio, StatusItem status) {
        return new ItemChecklist(1L, 1L, "doc", "como obter", obrigatorio, status);
    }

    @Test
    public void checklistSoFicaProntaComTodosOsObrigatorios() {
        assertTrue(new Checklist(List.of(item(true, StatusItem.PRONTO), item(false, StatusItem.PENDENTE))).obrigatoriosProntos());
        assertFalse(new Checklist(List.of(item(true, StatusItem.PRONTO), item(true, StatusItem.DUVIDA))).obrigatoriosProntos());
        assertFalse(new Checklist(List.of(item(true, StatusItem.ILEGIVEL))).obrigatoriosProntos());
        assertTrue("ENVIADO conta como pronto", new Checklist(List.of(item(true, StatusItem.ENVIADO))).obrigatoriosProntos());
    }

    @Test
    public void checklistIdentificaDuvidaOuIlegivel() {
        assertTrue(new Checklist(List.of(item(false, StatusItem.DUVIDA))).temDuvidaOuIlegivel());
        assertTrue(new Checklist(List.of(item(true, StatusItem.ILEGIVEL))).temDuvidaOuIlegivel());
        assertFalse(new Checklist(List.of(item(true, StatusItem.PENDENTE))).temDuvidaOuIlegivel());
    }

    @Test
    public void notasDaAvaliacaoDevemEstarEntreUmECinco() {
        new Avaliacao(1, 5, true);
        assertThrows(IllegalArgumentException.class, () -> new Avaliacao(0, 3, true));
        assertThrows(IllegalArgumentException.class, () -> new Avaliacao(3, 6, true));
    }
}
