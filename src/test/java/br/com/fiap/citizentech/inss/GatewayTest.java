package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.dao.CidadaoDao;
import br.com.fiap.citizentech.inss.modelo.AcessoNegadoException;
import br.com.fiap.citizentech.inss.modelo.Cidadao;
import br.com.fiap.citizentech.inss.modelo.Escopo;
import br.com.fiap.citizentech.inss.modelo.PedidoComExigencia;
import br.com.fiap.citizentech.inss.servico.AutenticacaoSimulada;
import br.com.fiap.citizentech.inss.servico.AutenticacaoSimulada.Sessao;
import br.com.fiap.citizentech.inss.servico.InssGatewayFicticio;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Controle de acesso: o que cada sessão pode (e não pode) ver e fazer. */
public class GatewayTest extends TesteBase {
    private RelogioMutavel relogio;
    private AutenticacaoSimulada auth;
    private InssGatewayFicticio gateway;
    private CidadaoDao cidadaos;

    @Before
    public void montar() {
        relogio = new RelogioMutavel();
        auth = new AutenticacaoSimulada(relogio);
        gateway = new InssGatewayFicticio(con, auth, relogio, true, true);
        cidadaos = new CidadaoDao(con);
    }

    private Sessao entrar(long cidadaoId) {
        Cidadao c = cidadaos.buscarPorId(cidadaoId).orElseThrow();
        return auth.iniciarSessao(c, EnumSet.allOf(Escopo.class));
    }

    @Test
    public void titularVeSomenteOsProprioPedidos() {
        List<PedidoComExigencia> pedidos = gateway.consultarPedidos(entrar(1));
        assertEquals(1, pedidos.size());
        assertEquals("FICT-0001", pedidos.get(0).pedido().getProtocolo());
        assertTrue(pedidos.get(0).exigencia().isPresent());
    }

    @Test
    public void representanteHabilitadoVeOsPedidosDoRepresentado() {
        List<PedidoComExigencia> pedidos = gateway.consultarPedidos(entrar(5));       // Marta -> Josefa
        assertEquals("FICT-0011", pedidos.get(0).pedido().getProtocolo());
    }

    @Test
    public void representanteNaoHabilitadoERecusado() {
        assertThrows(AcessoNegadoException.class, () -> gateway.consultarPedidos(entrar(4)));   // Rita
    }

    @Test
    public void semEscopoDeSituacaoNaoHaConsulta() {
        Sessao s = auth.iniciarSessao(cidadaos.buscarPorId(1).orElseThrow(), EnumSet.of(Escopo.PROTOCOLO));
        assertThrows(AcessoNegadoException.class, () -> gateway.consultarPedidos(s));
    }

    @Test
    public void sessaoExpiradaNaoConsulta() {
        Sessao s = entrar(1);
        relogio.avancar(Duration.ofMinutes(11));
        assertThrows(AcessoNegadoException.class, () -> gateway.consultarPedidos(s));
    }

    @Test
    public void naoSeProtocolaExigenciaDeOutraPessoa() throws SQLException {
        long exigenciaDaJosefa = idExigenciaDoPedido("FICT-0011");
        Sessao maria = entrar(1);
        assertThrows(AcessoNegadoException.class, () -> gateway.protocolarEnvio(maria, 1, exigenciaDaJosefa));
        assertEquals("nada foi gravado", 0, contar("SELECT COUNT(*) FROM envio_protocolo"));
    }

    @Test
    public void naoSeProtocolaSemEscopoDeProtocolo() throws SQLException {
        Sessao semProtocolo = auth.iniciarSessao(cidadaos.buscarPorId(1).orElseThrow(),
                EnumSet.of(Escopo.SITUACAO_PEDIDO, Escopo.TEXTO_EXIGENCIA, Escopo.PRAZO));
        long exigencia = idExigenciaDoPedido("FICT-0001");
        assertThrows(AcessoNegadoException.class, () -> gateway.protocolarEnvio(semProtocolo, 1, exigencia));
    }
}
