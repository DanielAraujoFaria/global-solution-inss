package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.modelo.Exigencia;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Pedido;
import br.com.fiap.citizentech.inss.modelo.StatusExigencia;
import br.com.fiap.citizentech.inss.modelo.StatusPedido;
import br.com.fiap.citizentech.inss.modelo.TipoAcesso;
import br.com.fiap.citizentech.inss.modelo.TipoExigencia;
import br.com.fiap.citizentech.inss.servico.AvaliadorDeCaso;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/** Tabela de decisão de quando a automação deve parar. A ordem das regras é a prioridade. */
public class AvaliadorDeCasoTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 18);

    private static Pedido pedido(StatusPedido status, boolean divergente) {
        return new Pedido(1L, 1L, "FICT", "Benefício", status, divergente, HOJE.minusDays(30));
    }

    private static Exigencia exigencia(boolean exigeAnaliseHumana, int diasParaPrazo) {
        TipoExigencia tipo = new TipoExigencia(1L, "X", "t", "o", "e", 30, exigeAnaliseHumana);
        return new Exigencia(1L, 1L, tipo, "texto", HOJE.minusDays(5), HOJE.plusDays(diasParaPrazo),
                StatusExigencia.ABERTA, null);
    }

    private static Optional<MotivoEncaminhamento> avaliar(TipoAcesso acesso, Pedido p, Exigencia e) {
        return AvaliadorDeCaso.avaliar(acesso, p, e, HOJE);
    }

    @Test
    public void casoNormalSegueParaAChecklist() {
        assertEquals(Optional.empty(), avaliar(TipoAcesso.TITULAR, pedido(StatusPedido.COM_EXIGENCIA, false), exigencia(false, 10)));
        assertEquals(Optional.empty(), avaliar(TipoAcesso.REPRESENTANTE_HABILITADO, pedido(StatusPedido.COM_EXIGENCIA, false), exigencia(false, 10)));
    }

    @Test
    public void representanteNaoHabilitadoTemPrioridadeSobreTudo() {
        assertEquals(Optional.of(MotivoEncaminhamento.REPRESENTANTE_NAO_HABILITADO),
                avaliar(TipoAcesso.REPRESENTANTE_NAO_HABILITADO, pedido(StatusPedido.INDEFERIDO, true), exigencia(true, -9)));
        assertEquals(Optional.of(MotivoEncaminhamento.REPRESENTANTE_NAO_HABILITADO),
                avaliar(TipoAcesso.REPRESENTANTE_NAO_HABILITADO, null, null));
    }

    @Test
    public void indeferidoVemAntesDeDivergenciaComplexidadeEPrazo() {
        assertEquals(Optional.of(MotivoEncaminhamento.PEDIDO_INDEFERIDO),
                avaliar(TipoAcesso.TITULAR, pedido(StatusPedido.INDEFERIDO, true), exigencia(true, -9)));
    }

    @Test
    public void divergenciaVemAntesDeComplexidadeEPrazo() {
        assertEquals(Optional.of(MotivoEncaminhamento.DADOS_DIVERGENTES),
                avaliar(TipoAcesso.TITULAR, pedido(StatusPedido.COM_EXIGENCIA, true), exigencia(true, -9)));
    }

    @Test
    public void semExigenciaPendente() {
        assertEquals(Optional.of(MotivoEncaminhamento.NENHUMA_EXIGENCIA_ENCONTRADA),
                avaliar(TipoAcesso.TITULAR, pedido(StatusPedido.EM_ANALISE, false), null));
        assertEquals(Optional.of(MotivoEncaminhamento.NENHUMA_EXIGENCIA_ENCONTRADA),
                avaliar(TipoAcesso.TITULAR, null, null));
    }

    @Test
    public void casoComplexoVemAntesDoPrazoVencido() {
        assertEquals(Optional.of(MotivoEncaminhamento.CASO_COMPLEXO),
                avaliar(TipoAcesso.TITULAR, pedido(StatusPedido.COM_EXIGENCIA, false), exigencia(true, -9)));
    }

    @Test
    public void prazoVencidoSoQuandoPassouDeFato() {
        assertEquals(Optional.of(MotivoEncaminhamento.PRAZO_VENCIDO),
                avaliar(TipoAcesso.TITULAR, pedido(StatusPedido.COM_EXIGENCIA, false), exigencia(false, -1)));
        assertEquals("vence hoje: ainda dá tempo", Optional.empty(),
                avaliar(TipoAcesso.TITULAR, pedido(StatusPedido.COM_EXIGENCIA, false), exigencia(false, 0)));
    }

    @Test
    public void cadaMotivoTemODestinoOficialEsperado() {
        assertEquals("AGENCIA_APS", MotivoEncaminhamento.PRAZO_VENCIDO.destino().name());
        assertEquals("AGENCIA_APS", MotivoEncaminhamento.CASO_COMPLEXO.destino().name());
        assertEquals("AGENCIA_APS", MotivoEncaminhamento.DADOS_DIVERGENTES.destino().name());
        assertEquals("MEU_INSS", MotivoEncaminhamento.PEDIDO_INDEFERIDO.destino().name());
        assertEquals("MEU_INSS", MotivoEncaminhamento.INTEGRACAO_INDISPONIVEL.destino().name());
        assertEquals("CENTRAL_135", MotivoEncaminhamento.REPRESENTANTE_NAO_HABILITADO.destino().name());
    }
}
