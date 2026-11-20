package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.dao.AuditoriaDao;
import br.com.fiap.citizentech.inss.dao.CatalogoDao;
import br.com.fiap.citizentech.inss.dao.ChecklistDao;
import br.com.fiap.citizentech.inss.dao.CidadaoDao;
import br.com.fiap.citizentech.inss.dao.ConversaDao;
import br.com.fiap.citizentech.inss.dao.ExigenciaDao;
import br.com.fiap.citizentech.inss.dao.MensagemDao;
import br.com.fiap.citizentech.inss.dao.PedidoDao;
import br.com.fiap.citizentech.inss.modelo.Autor;
import br.com.fiap.citizentech.inss.modelo.Checklist;
import br.com.fiap.citizentech.inss.modelo.Cidadao;
import br.com.fiap.citizentech.inss.modelo.ConversaRegistro;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.modelo.Encaminhamento;
import br.com.fiap.citizentech.inss.modelo.Exigencia;
import br.com.fiap.citizentech.inss.modelo.ItemChecklist;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Origem;
import br.com.fiap.citizentech.inss.modelo.StatusExigencia;
import br.com.fiap.citizentech.inss.modelo.StatusItem;
import br.com.fiap.citizentech.inss.modelo.TipoAcesso;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** CRUD das principais tabelas: inserção, consulta, atualização e remoção. */
public class DaoTest extends TesteBase {

    @Test
    public void inserirEConsultarCidadao() {
        CidadaoDao dao = new CidadaoDao(con);
        long id = dao.inserir(new Cidadao(null, "Pessoa Teste", "18-39", TipoAcesso.TITULAR, null));
        Optional<Cidadao> lido = dao.buscarPorId(id);
        assertTrue(lido.isPresent());
        assertEquals("Pessoa Teste", lido.get().getNomeFicticio());
        assertEquals("Pessoa", lido.get().primeiroNome());
        assertEquals(12, dao.listar().size());
    }

    @Test
    public void catalogoTrazOsTiposDeExigencia() {
        CatalogoDao dao = new CatalogoDao(con);
        assertEquals(5, dao.listarTipos().size());
        assertTrue(dao.buscarPorCodigo("PERIODO_RURAL").orElseThrow().exigeAnaliseHumana());
        assertFalse(dao.buscarPorCodigo("COMP_RESIDENCIA").orElseThrow().exigeAnaliseHumana());
        assertEquals(Optional.empty(), dao.buscarPorCodigo("NAO_EXISTE"));
    }

    @Test
    public void atualizarStatusDaExigenciaEFiltrarPendentes() throws SQLException {
        ExigenciaDao dao = new ExigenciaDao(con);
        PedidoDao pedidos = new PedidoDao(con);
        long pedidoId = pedidos.listarPorCidadao(1).get(0).getId();
        Exigencia antes = dao.buscarPendenteDoPedido(pedidoId).orElseThrow();
        assertEquals(StatusExigencia.ABERTA, antes.getStatus());

        dao.atualizarStatus(antes.getId(), StatusExigencia.CUMPRIDA, LocalDate.of(2026, 11, 18));
        assertEquals("cumprida não é mais pendente", Optional.empty(), dao.buscarPendenteDoPedido(pedidoId));
        Exigencia depois = dao.buscarPorId(antes.getId()).orElseThrow();
        assertEquals(StatusExigencia.CUMPRIDA, depois.getStatus());
        assertEquals(LocalDate.of(2026, 11, 18), depois.getDataCumprimento());
    }

    @Test
    public void checklistEGeradaDoModeloEAtualizada() throws SQLException {
        ChecklistDao dao = new ChecklistDao(con);
        long exigenciaId = idExigenciaDoPedido("FICT-0003");          // vínculo de trabalho: 3 documentos-modelo
        long tipoId = contar("SELECT tipo_exigencia_id FROM exigencia WHERE id = ?", exigenciaId);

        assertEquals(3, dao.gerarAPartirDoModelo(exigenciaId, tipoId));
        Checklist lista = dao.listar(exigenciaId);
        assertEquals(3, lista.itens().size());
        assertEquals("só o primeiro é obrigatório", 1, lista.itens().stream().filter(ItemChecklist::isObrigatorio).count());
        assertTrue(lista.itens().stream().allMatch(i -> i.getStatus() == StatusItem.PENDENTE));

        ItemChecklist primeiro = lista.itens().get(0);
        ItemChecklist segundo = lista.itens().get(1);
        dao.atualizarStatus(primeiro.getId(), StatusItem.PRONTO, LocalDateTime.now());
        dao.atualizarStatus(segundo.getId(), StatusItem.DUVIDA, LocalDateTime.now());
        assertEquals("só o PRONTO vira ENVIADO", 1, dao.marcarProntosComoEnviados(exigenciaId, LocalDateTime.now()));

        Checklist depois = dao.listar(exigenciaId);
        assertEquals(StatusItem.ENVIADO, depois.itens().get(0).getStatus());
        assertEquals(StatusItem.DUVIDA, depois.itens().get(1).getStatus());
        assertEquals(StatusItem.PENDENTE, depois.itens().get(2).getStatus());
    }

    @Test
    public void finalizarConversaGravaODestinoDoMotivo() {
        ConversaDao dao = new ConversaDao(con);
        long id = dao.iniciar(Origem.TESTE_USUARIO, "P01", LocalDateTime.now());
        dao.finalizar(id, Desfecho.ENCAMINHADA, MotivoEncaminhamento.PRAZO_VENCIDO, "resumo", LocalDateTime.now());
        ConversaRegistro c = dao.buscar(id).orElseThrow();
        assertEquals(Desfecho.ENCAMINHADA, c.desfecho());
        assertEquals(Encaminhamento.AGENCIA_APS, c.encaminhamento());
        assertEquals(MotivoEncaminhamento.PRAZO_VENCIDO, c.motivoEncaminhamento());
        assertEquals("P01", c.rotulo());
        assertTrue(c.finalizada());
    }

    @Test
    public void politicaDeRetencaoApagaSoMensagensAntigas() {
        ConversaDao conversas = new ConversaDao(con);
        MensagemDao mensagens = new MensagemDao(con);
        long conversa = conversas.iniciar(Origem.SIMULACAO, null, LocalDateTime.now());
        LocalDateTime agora = LocalDateTime.of(2026, 11, 18, 12, 0);
        mensagens.inserir(conversa, Autor.CIDADAO, "antiga", false, agora.minusDays(45));
        mensagens.inserir(conversa, Autor.ASSISTENTE, "recente", false, agora.minusDays(2));

        assertEquals(1, mensagens.purgarAnterioresA(agora.minusDays(30)));
        assertEquals(1, mensagens.listarPorConversa(conversa).size());
        assertEquals("recente", mensagens.listarPorConversa(conversa).get(0).texto());
    }

    @Test
    public void entradaMalIntencionadaEGravadaComoTextoENaoExecutada() throws SQLException {
        ConversaDao dao = new ConversaDao(con);
        String ataque = "x'); DROP TABLE conversa; --";
        long id = dao.iniciar(Origem.TESTE_USUARIO, ataque, LocalDateTime.now());
        assertEquals(ataque, dao.buscar(id).orElseThrow().rotulo());
        assertEquals("a tabela continua existindo", 1, contar("SELECT COUNT(*) FROM conversa"));
    }

    @Test
    public void auditoriaContaEventos() {
        ConversaDao conversas = new ConversaDao(con);
        AuditoriaDao auditoria = new AuditoriaDao(con);
        long id = conversas.iniciar(Origem.SIMULACAO, null, LocalDateTime.now());
        auditoria.registrar(id, "CONSENTIMENTO_CONCEDIDO", "x", LocalDateTime.now());
        auditoria.registrar(id, "DADO_SENSIVEL_DETECTADO", "CPF", LocalDateTime.now());
        auditoria.registrar(id, "DADO_SENSIVEL_DETECTADO", "CODIGO", LocalDateTime.now());
        assertEquals(2, auditoria.contar("DADO_SENSIVEL_DETECTADO"));
        assertEquals(2, auditoria.contarPorEvento().size());
        assertEquals(3, auditoria.listarEventos(id).size());
    }
}
