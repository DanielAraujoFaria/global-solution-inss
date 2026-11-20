package br.com.fiap.citizentech.inss.analise;

import br.com.fiap.citizentech.inss.dao.AuditoriaDao;
import br.com.fiap.citizentech.inss.dao.AvaliacaoDao;
import br.com.fiap.citizentech.inss.dao.ConversaDao;
import br.com.fiap.citizentech.inss.dao.DaoBase;
import br.com.fiap.citizentech.inss.dao.MensagemDao;
import br.com.fiap.citizentech.inss.modelo.Autor;
import br.com.fiap.citizentech.inss.modelo.Avaliacao;
import br.com.fiap.citizentech.inss.modelo.ConversaRegistro;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.modelo.MensagemRegistro;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Origem;
import br.com.fiap.citizentech.inss.servico.DetectorDadosSensiveis;

import java.sql.Connection;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo dos indicadores a partir do banco. As contagens agregadas usam SQL (GROUP BY/COUNT);
 * as estatísticas descritivas usam {@link Estatistica}.
 */
public class Indicadores extends DaoBase {
    private final ConversaDao conversaDao;
    private final AvaliacaoDao avaliacaoDao;
    private final MensagemDao mensagemDao;
    private final AuditoriaDao auditoriaDao;
    private final DetectorDadosSensiveis detector = new DetectorDadosSensiveis();

    public Indicadores(Connection con) {
        super(con);
        this.conversaDao = new ConversaDao(con);
        this.avaliacaoDao = new AvaliacaoDao(con);
        this.mensagemDao = new MensagemDao(con);
        this.auditoriaDao = new AuditoriaDao(con);
    }

    public List<ConversaRegistro> conversas(Origem origem) {
        return conversaDao.listar(origem);
    }

    public List<ConversaRegistro> finalizadas(Origem origem) {
        return conversas(origem).stream().filter(ConversaRegistro::finalizada).toList();
    }

    /** Conversas por desfecho (SQL GROUP BY). */
    public Map<Desfecho, Long> porDesfecho(Origem origem) {
        Map<Desfecho, Long> mapa = new EnumMap<>(Desfecho.class);
        consultar("SELECT desfecho, COUNT(*) FROM conversa WHERE origem = ? GROUP BY desfecho",
                rs -> mapa.put(Desfecho.valueOf(rs.getString(1)), rs.getLong(2)), origem);
        return mapa;
    }

    /** Encaminhamentos por motivo (SQL GROUP BY). */
    public Map<MotivoEncaminhamento, Long> porMotivo(Origem origem) {
        Map<MotivoEncaminhamento, Long> mapa = new EnumMap<>(MotivoEncaminhamento.class);
        consultar("SELECT motivo_encaminhamento, COUNT(*) FROM conversa "
                        + "WHERE origem = ? AND motivo_encaminhamento IS NOT NULL GROUP BY motivo_encaminhamento",
                rs -> mapa.put(MotivoEncaminhamento.valueOf(rs.getString(1)), rs.getLong(2)), origem);
        return mapa;
    }

    /** Conversas encerradas com a etapa resolvida sem precisar de atendimento adicional. */
    public long resolvidasSemAtendimentoAdicional(Origem origem) {
        return finalizadas(origem).stream().filter(c -> c.desfecho().resolvidaSemAtendimentoAdicional()).count();
    }

    public long encaminhadasParaPessoa(Origem origem) {
        return finalizadas(origem).stream()
                .filter(c -> c.encaminhamento() != null && c.encaminhamento().atendimentoHumano()).count();
    }

    public long comChecklist(Origem origem) {
        return conversas(origem).stream().filter(ConversaRegistro::teveChecklist).count();
    }

    public long comChecklistCompleta(Origem origem) {
        return conversas(origem).stream().filter(ConversaRegistro::checklistCompleta).count();
    }

    /** Dias entre a emissão da exigência e o envio registrado (notificação até cumprimento). */
    public List<Long> diasDaNotificacaoAteOEnvio(Origem origem) {
        return consultar("""
                SELECT e.data_emissao, CAST(v.enviado_em AS DATE)
                  FROM envio_protocolo v
                  JOIN exigencia e ON e.id = v.exigencia_id
                  JOIN conversa c ON c.id = v.conversa_id
                 WHERE c.origem = ?
                """,
                rs -> ChronoUnit.DAYS.between(rs.getObject(1, java.time.LocalDate.class),
                        rs.getObject(2, java.time.LocalDate.class)),
                origem);
    }

    /** Duração (segundos) das conversas que chegaram a um desfecho diferente de abandono. */
    public List<Double> duracoesEmSegundos(Origem origem) {
        return finalizadas(origem).stream().filter(c -> c.desfecho() != Desfecho.ABANDONADA)
                .map(ConversaRegistro::duracaoSegundos).toList();
    }

    public List<Avaliacao> avaliacoes(Origem origem) {
        return avaliacaoDao.listarPorOrigem(origem);
    }

    // ---- privacidade e segurança ----

    public long tentativasDeEnvioDeDadoSensivelBloqueadas() {
        return auditoriaDao.contar("DADO_SENSIVEL_DETECTADO");
    }

    /**
     * Auditoria dos próprios dados: varre as mensagens de cidadãos e conta as que ainda contêm
     * dado sensível sem máscara. O valor esperado é 0.
     */
    public long mensagensDeCidadaosComDadoSensivelSemMascara() {
        long total = 0;
        for (MensagemRegistro m : mensagemDao.listarTodas()) {
            if (m.autor() == Autor.CIDADAO && !m.mascarada() && detector.analisar(m.texto()).sensivel()) {
                total++;
            }
        }
        return total;
    }
}
