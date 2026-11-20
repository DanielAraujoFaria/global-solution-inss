package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.analise.Indicadores;
import br.com.fiap.citizentech.inss.analise.RelatorioIndicadores;
import br.com.fiap.citizentech.inss.dao.AvaliacaoDao;
import br.com.fiap.citizentech.inss.dao.ConversaDao;
import br.com.fiap.citizentech.inss.dao.MensagemDao;
import br.com.fiap.citizentech.inss.modelo.Autor;
import br.com.fiap.citizentech.inss.modelo.Avaliacao;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Origem;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Confere os indicadores contra uma amostra pequena criada à mão (valores conhecidos).
 * Estes dados existem só dentro do teste; não são resultados de usuários reais.
 */
public class IndicadoresTest extends TesteBase {
    private ConversaDao conversas;
    private AvaliacaoDao avaliacoes;
    private Indicadores indicadores;

    @Before
    public void montarAmostra() {
        conversas = new ConversaDao(con);
        avaliacoes = new AvaliacaoDao(con);
        indicadores = new Indicadores(con);
        LocalDateTime t0 = LocalDateTime.of(2026, 11, 18, 10, 0);

        int[] clareza = {4, 5, 3, 4, 5};
        int[] satisfacao = {5, 5, 2, 4, 5};
        boolean[] sabe = {true, true, false, true, true};
        Desfecho[] desfechos = {Desfecho.CONCLUIDA_DIGITAL, Desfecho.CONCLUIDA_DIGITAL, Desfecho.PENDENTE_DOCUMENTOS,
                Desfecho.ENCAMINHADA, Desfecho.ABANDONADA};
        for (int i = 0; i < 5; i++) {
            long id = conversas.iniciar(Origem.TESTE_USUARIO, "P0" + (i + 1), t0);
            if (desfechos[i] == Desfecho.ENCAMINHADA) {
                conversas.finalizar(id, desfechos[i], MotivoEncaminhamento.PRAZO_VENCIDO, null, t0.plusSeconds(60));
            } else {
                conversas.finalizar(id, desfechos[i], null, null, t0.plusSeconds(60 + 30L * i));
            }
            avaliacoes.inserir(id, new Avaliacao(clareza[i], satisfacao[i], sabe[i]), t0.plusMinutes(5));
        }
        // uma conversa de simulação: não pode entrar na conta dos testes com pessoas
        long sim = conversas.iniciar(Origem.SIMULACAO, "C01", t0);
        conversas.finalizar(sim, Desfecho.CONCLUIDA_DIGITAL, null, null, t0.plusSeconds(1));
    }

    @Test
    public void contagensPorOrigemESeparadasDaSimulacao() {
        assertEquals(5, indicadores.conversas(Origem.TESTE_USUARIO).size());
        assertEquals(1, indicadores.conversas(Origem.SIMULACAO).size());
        assertEquals(2, indicadores.resolvidasSemAtendimentoAdicional(Origem.TESTE_USUARIO));
        assertEquals(1, indicadores.encaminhadasParaPessoa(Origem.TESTE_USUARIO));
        assertEquals(1, (long) indicadores.porDesfecho(Origem.TESTE_USUARIO).get(Desfecho.ABANDONADA));
        assertEquals(2, (long) indicadores.porDesfecho(Origem.TESTE_USUARIO).get(Desfecho.CONCLUIDA_DIGITAL));
    }

    @Test
    public void duracaoIgnoraConversasAbandonadas() {
        // 4 conversas com desfecho (a abandonada fica de fora): 60s, 90s, 120s, 150s
        var duracoes = indicadores.duracoesEmSegundos(Origem.TESTE_USUARIO);
        assertEquals(4, duracoes.size());
        assertEquals(60.0, duracoes.get(0), 0.001);
    }

    @Test
    public void relatorioTrazOsNumerosEsperadosEOAvisoDeAmostraPequena() {
        String relatorio = RelatorioIndicadores.gerar(con, Origem.TESTE_USUARIO);
        assertTrue(relatorio, relatorio.contains("AVISO: amostra pequena (n < 30)"));
        assertTrue(relatorio, relatorio.contains("média 4,20"));                  // clareza: 4,2 (Python)
        assertTrue(relatorio, relatorio.contains("desvio padrão 0,84"));          // 0,8367 (Python)
        assertTrue(relatorio, relatorio.contains("4 de 5 = 80,0%"));              // sabem o próximo passo
        assertTrue(relatorio, relatorio.contains("2 de 5 = 40,0%"));              // resolvidas sem atendimento adicional
    }

    @Test
    public void relatorioDaSimulacaoNaoMostraNotasDePessoas() {
        String relatorio = RelatorioIndicadores.gerar(con, Origem.SIMULACAO);
        assertTrue(relatorio.contains("Não se aplicam à simulação"));
        assertTrue(!relatorio.contains("Clareza da exigência"));
    }

    @Test
    public void menosDeCincoSessoesGeraAvisoDeAmostraInsuficiente() {
        conversas.iniciar(Origem.DEMONSTRACAO, "d", LocalDateTime.now());
        String relatorio = RelatorioIndicadores.gerar(con, Origem.DEMONSTRACAO);
        assertTrue(relatorio.contains("conversas de demonstração"));
    }

    @Test
    public void auditoriaDePrivacidadeEncontraDadoSensivelSemMascara() {
        MensagemDao mensagens = new MensagemDao(con);
        long conversa = conversas.iniciar(Origem.SIMULACAO, null, LocalDateTime.now());
        assertEquals(0, indicadores.mensagensDeCidadaosComDadoSensivelSemMascara());
        // mascarada: ok
        mensagens.inserir(conversa, Autor.CIDADAO, "[CPF REMOVIDO]", true, LocalDateTime.now());
        // texto normal: ok
        mensagens.inserir(conversa, Autor.CIDADAO, "preciso de ajuda", false, LocalDateTime.now());
        // fala do assistente com número de protocolo: não é dado do cidadão
        mensagens.inserir(conversa, Autor.ASSISTENTE, "Protocolo SIM-2026-000123", false, LocalDateTime.now());
        assertEquals(0, indicadores.mensagensDeCidadaosComDadoSensivelSemMascara());
        // vazamento: a auditoria precisa enxergar
        mensagens.inserir(conversa, Autor.CIDADAO, "meu cpf é 123.456.789-09", false, LocalDateTime.now());
        assertEquals(1, indicadores.mensagensDeCidadaosComDadoSensivelSemMascara());
    }
}
