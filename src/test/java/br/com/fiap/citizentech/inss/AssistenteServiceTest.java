package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.dao.AuditoriaDao;
import br.com.fiap.citizentech.inss.dao.ChecklistDao;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Origem;
import br.com.fiap.citizentech.inss.modelo.PedidoComExigencia;
import br.com.fiap.citizentech.inss.modelo.StatusItem;
import br.com.fiap.citizentech.inss.servico.AssistenteFactory;
import br.com.fiap.citizentech.inss.servico.AssistenteService;
import br.com.fiap.citizentech.inss.servico.AssistenteService.Resultado;
import br.com.fiap.citizentech.inss.servico.AutenticacaoSimulada;
import br.com.fiap.citizentech.inss.servico.AutenticacaoSimulada.Sessao;
import br.com.fiap.citizentech.inss.servico.ExplicadorPorCatalogo;
import br.com.fiap.citizentech.inss.servico.InssGateway;
import br.com.fiap.citizentech.inss.servico.InssGatewayFicticio;
import br.com.fiap.citizentech.inss.ui.CanalRoteirizado;
import org.junit.Test;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Comportamentos do assistente que os cenários não cobrem diretamente. */
public class AssistenteServiceTest extends TesteBase {
    // Maria: pendência, motivo, sem relato, autoriza, perfil 1, autoriza no gov.br
    private static final String[] LOGIN_MARIA = {"1", "1", "0", "1", "1", "1"};

    private static String[] roteiro(String[] inicio, String... resto) {
        String[] todos = new String[inicio.length + resto.length];
        System.arraycopy(inicio, 0, todos, 0, inicio.length);
        System.arraycopy(resto, 0, todos, inicio.length, resto.length);
        return todos;
    }

    @Test
    public void semNenhumaEntradaAConversaTerminaAbandonada() throws SQLException {
        AssistenteService servico = AssistenteFactory.criar(con);
        Resultado r = servico.atender(new CanalRoteirizado(null), Origem.TESTE_USUARIO, "P00");
        assertEquals(Desfecho.ABANDONADA, r.desfecho());
        assertEquals(1, contar("SELECT COUNT(*) FROM conversa WHERE desfecho = 'ABANDONADA' AND fim IS NOT NULL"));
    }

    @Test
    public void entradaInvalidaEPedidaDeNovoSemQuebrarAConversa() {
        AssistenteService servico = AssistenteFactory.criar(con);
        CanalRoteirizado canal = new CanalRoteirizado(null, "abc", "9", "-1", "", "2", "0");
        Resultado r = servico.atender(canal, Origem.SIMULACAO, "invalida");
        assertEquals(MotivoEncaminhamento.SOLICITADO_PELO_CIDADAO, r.motivoEncaminhamento());
        assertTrue(canal.falasDoAssistente().contains("Responda só com um número de 1 a 4"));
    }

    @Test
    public void checklistFicaGravadaEPodeSerRetomada() throws SQLException {
        AssistenteService servico = AssistenteFactory.criar(con);
        // Maria responde "tenho" e "não tenho", mas NÃO confirma o envio.
        servico.atender(new CanalRoteirizado(null, roteiro(LOGIN_MARIA, "1", "2", "2", "0")), Origem.SIMULACAO, "a");

        long exigencia = idExigenciaDoPedido("FICT-0001");
        var itens = new ChecklistDao(con).listar(exigencia).itens();
        assertEquals(StatusItem.PRONTO, itens.get(0).getStatus());
        assertEquals(StatusItem.PENDENTE, itens.get(1).getStatus());
        assertEquals("EM_ANDAMENTO", statusDaExigencia("FICT-0001"));

        // Segunda conversa: a lista não é duplicada e dá para concluir.
        Resultado r = servico.atender(new CanalRoteirizado(null, roteiro(LOGIN_MARIA, "1", "2", "1", "0")), Origem.SIMULACAO, "b");
        assertEquals(Desfecho.CONCLUIDA_DIGITAL, r.desfecho());
        assertEquals("a checklist não foi duplicada", 2, contar("SELECT COUNT(*) FROM item_checklist WHERE exigencia_id = ?", exigencia));
        assertEquals("CUMPRIDA", statusDaExigencia("FICT-0001"));
    }

    @Test
    public void tempoPassadoDeDepoisDoLoginDerrubaASessaoEOAssistenteNaoConsulta() throws SQLException {
        RelogioMutavel relogio = new RelogioMutavel();
        AutenticacaoSimulada auth = new AutenticacaoSimulada(relogio);
        InssGateway gateway = new InssGatewayFicticio(con, auth, relogio, true, true);
        AssistenteService servico = new AssistenteService(con, relogio, gateway, auth, new ExplicadorPorCatalogo());
        CanalRoteirizado canal = new CanalRoteirizado(null, roteiro(LOGIN_MARIA, "0")) {
            @Override
            public void navegador(String texto) {
                super.navegador(texto);
                if (texto.startsWith("Login concluído")) {
                    relogio.avancar(Duration.ofMinutes(11));   // a sessão vence logo depois do login
                }
            }
        };
        Resultado r = servico.atender(canal, Origem.SIMULACAO, "expirou");
        assertEquals(MotivoEncaminhamento.AUTENTICACAO_NAO_CONCLUIDA, r.motivoEncaminhamento());
        assertEquals(1, new AuditoriaDao(con).contar("ACESSO_NEGADO"));
        assertEquals(0, new AuditoriaDao(con).contar("CONSULTA_REALIZADA"));
    }

    /** Gateway que grava o protocolo e DEPOIS falha: simula queda no meio do envio. */
    private static final class GatewayQueFalhaDepoisDeGravar implements InssGateway {
        private final InssGateway real;

        GatewayQueFalhaDepoisDeGravar(InssGateway real) {
            this.real = real;
        }

        @Override
        public boolean integracaoDisponivel() {
            return real.integracaoDisponivel();
        }

        @Override
        public boolean envioDisponivel() {
            return real.envioDisponivel();
        }

        @Override
        public List<PedidoComExigencia> consultarPedidos(Sessao sessao) {
            return real.consultarPedidos(sessao);
        }

        @Override
        public String protocolarEnvio(Sessao sessao, long conversaId, long exigenciaId) {
            real.protocolarEnvio(sessao, conversaId, exigenciaId);
            throw new ErroDeBancoException("queda simulada depois de gravar", null);
        }
    }

    @Test
    public void quedaNoMeioDoEnvioDesfazOProtocoloENaoMarcaComoCumprida() throws SQLException {
        Clock relogio = Clock.systemDefaultZone();
        AutenticacaoSimulada auth = new AutenticacaoSimulada(relogio);
        InssGateway falho = new GatewayQueFalhaDepoisDeGravar(new InssGatewayFicticio(con, auth, relogio, true, true));
        AssistenteService servico = new AssistenteService(con, relogio, falho, auth, new ExplicadorPorCatalogo());

        CanalRoteirizado canal = new CanalRoteirizado(null, roteiro(LOGIN_MARIA, "1", "2", "1", "0"));
        Resultado r = servico.atender(canal, Origem.SIMULACAO, "queda");

        assertEquals(Desfecho.ENCAMINHADA, r.desfecho());
        assertEquals(MotivoEncaminhamento.INTEGRACAO_INDISPONIVEL, r.motivoEncaminhamento());
        assertEquals("protocolo gravado foi desfeito (rollback)", 0, contar("SELECT COUNT(*) FROM envio_protocolo"));
        assertFalse("não pode constar como cumprida", "CUMPRIDA".equals(statusDaExigencia("FICT-0001")));
        assertEquals("documento não virou ENVIADO", 0, contar("SELECT COUNT(*) FROM item_checklist WHERE status = 'ENVIADO'"));
        assertTrue(canal.falasDoAssistente().contains("nada foi enviado"));
    }

    @Test
    public void textoOficialSempreApareceSemAlteracaoAoLadoDaExplicacao() throws SQLException {
        AssistenteService servico = AssistenteFactory.criar(con);
        CanalRoteirizado canal = new CanalRoteirizado(null, roteiro(LOGIN_MARIA, "1", "2", "2", "0"));
        servico.atender(canal, Origem.SIMULACAO, "texto");
        String oficial = texto("SELECT texto_oficial FROM exigencia WHERE id = ?", idExigenciaDoPedido("FICT-0001"));
        assertTrue("o texto oficial aparece literalmente", canal.falasDoAssistente().contains("\"" + oficial + "\""));
        assertTrue(canal.falasDoAssistente().contains("Em palavras simples"));
    }
}
