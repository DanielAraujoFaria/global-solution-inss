package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.modelo.AcessoNegadoException;
import br.com.fiap.citizentech.inss.modelo.Cidadao;
import br.com.fiap.citizentech.inss.modelo.Escopo;
import br.com.fiap.citizentech.inss.modelo.TipoAcesso;
import br.com.fiap.citizentech.inss.servico.AutenticacaoSimulada;
import br.com.fiap.citizentech.inss.servico.AutenticacaoSimulada.Sessao;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AutenticacaoSimuladaTest {
    private RelogioMutavel relogio;
    private AutenticacaoSimulada auth;
    private final Cidadao maria = new Cidadao(1L, "Maria", "60+", TipoAcesso.TITULAR, null);

    @Before
    public void preparar() {
        relogio = new RelogioMutavel();
        auth = new AutenticacaoSimulada(relogio);
    }

    @Test
    public void sessaoValidaPassaParaOsEscoposAutorizados() {
        Sessao s = auth.iniciarSessao(maria, EnumSet.allOf(Escopo.class));
        for (Escopo e : Escopo.values()) {
            auth.validar(s, e);
        }
    }

    @Test
    public void sessaoExpiraAposDezMinutos() {
        Sessao s = auth.iniciarSessao(maria, EnumSet.allOf(Escopo.class));
        relogio.avancar(Duration.ofMinutes(9).plusSeconds(59));
        auth.validar(s, Escopo.PRAZO);
        relogio.avancar(Duration.ofSeconds(1));
        AcessoNegadoException e = assertThrows(AcessoNegadoException.class, () -> auth.validar(s, Escopo.PRAZO));
        assertTrue(e.getMessage().contains("expirada"));
    }

    @Test
    public void escopoNaoAutorizadoEBarrado() {
        Sessao s = auth.iniciarSessao(maria, Set.of(Escopo.SITUACAO_PEDIDO));
        auth.validar(s, Escopo.SITUACAO_PEDIDO);
        assertThrows(AcessoNegadoException.class, () -> auth.validar(s, Escopo.PROTOCOLO));
    }

    @Test
    public void sessaoEncerradaOuDesconhecidaEBarrada() {
        Sessao s = auth.iniciarSessao(maria, EnumSet.allOf(Escopo.class));
        auth.encerrar(s);
        assertThrows(AcessoNegadoException.class, () -> auth.validar(s, Escopo.PRAZO));
        assertThrows(AcessoNegadoException.class, () -> auth.validar(null, Escopo.PRAZO));
        Sessao falsificada = new Sessao("token-inventado", 1L, EnumSet.allOf(Escopo.class), relogio.instant().plusSeconds(600));
        assertThrows(AcessoNegadoException.class, () -> auth.validar(falsificada, Escopo.PRAZO));
    }

    @Test
    public void tokenNaoApareceNoToStringENoBancoVaiSoOHash() {
        Sessao s = auth.iniciarSessao(maria, EnumSet.allOf(Escopo.class));
        assertFalse(s.toString().contains(s.token()));
        String ref = auth.referenciaSegura(s);
        assertEquals(64, ref.length());
        assertNotEquals(s.token(), ref);
        assertEquals("a referência é estável", ref, auth.referenciaSegura(s));
        assertTrue(ref.matches("[0-9a-f]{64}"));
    }
}
