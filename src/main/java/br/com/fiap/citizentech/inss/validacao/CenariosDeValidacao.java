package br.com.fiap.citizentech.inss.validacao;

import br.com.fiap.citizentech.inss.db.BancoDeDados;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Origem;
import br.com.fiap.citizentech.inss.servico.AssistenteFactory;
import br.com.fiap.citizentech.inss.servico.AssistenteService;
import br.com.fiap.citizentech.inss.ui.CanalRoteirizado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Validação por cenários: personas roteirizadas percorrem o assistente de ponta a ponta e o resultado
 * é comparado com o esperado (o "oráculo" está escrito em cada cenário). Cada cenário também confere
 * propriedades de segurança que valem sempre (ex.: nunca enviar sem confirmação, nunca consultar sem consentimento).
 *
 * As entradas seguem a ordem das perguntas do assistente. Números de perfil = ordem em seed.sql.
 */
public final class CenariosDeValidacao {
    // perfis fictícios (ordem do seed.sql)
    static final String MARIA = "1", CARLOS = "2", ANA = "3", RITA = "4", MARTA = "5",
            RAIMUNDO = "6", LUCIA = "7", JOSE = "8", BEATRIZ = "9";

    @FunctionalInterface
    public interface Verificador {
        /**
         * Devolve a lista de falhas (vazia = tudo certo).
         * @param falasDoAssistente tudo o que o assistente disse no chat
         */
        List<String> verificar(Connection con, AssistenteService.Resultado resultado, String falasDoAssistente);
    }

    public record Cenario(String id, String descricao, String[] entradas, boolean integracaoDisponivel,
                          boolean envioDisponivel, Desfecho desfechoEsperado, MotivoEncaminhamento motivoEsperado,
                          Verificador extra) {
    }

    public record ResultadoCenario(Cenario cenario, AssistenteService.Resultado obtido, List<String> falhas) {
        public boolean ok() {
            return falhas.isEmpty();
        }
    }

    private CenariosDeValidacao() {
    }

    // ------------------------------------------------------------------ execução

    /** Executa todos os cenários em um banco (reiniciando o estado dos pedidos entre eles). */
    public static List<ResultadoCenario> executarTodos(Connection con, Clock clock) {
        BancoDeDados.prepararSeNecessario(con);
        List<ResultadoCenario> resultados = new ArrayList<>();
        for (Cenario cenario : todos()) {
            resultados.add(executar(con, clock, cenario));
        }
        return resultados;
    }

    public static ResultadoCenario executar(Connection con, Clock clock, Cenario cenario) {
        BancoDeDados.restaurarPedidos(con);
        AssistenteService servico = AssistenteFactory.criar(con, clock, cenario.integracaoDisponivel(),
                cenario.envioDisponivel());
        CanalRoteirizado canal = new CanalRoteirizado(null, cenario.entradas());
        AssistenteService.Resultado obtido = servico.atender(canal, Origem.SIMULACAO, cenario.id());

        List<String> falhas = new ArrayList<>();
        if (obtido.desfecho() != cenario.desfechoEsperado()) {
            falhas.add("desfecho esperado " + cenario.desfechoEsperado() + " mas foi " + obtido.desfecho());
        }
        if (obtido.motivoEncaminhamento() != cenario.motivoEsperado()) {
            falhas.add("motivo esperado " + cenario.motivoEsperado() + " mas foi " + obtido.motivoEncaminhamento());
        }
        if (canal.entradasRestantes() != 0) {
            falhas.add("roteiro tem " + canal.entradasRestantes() + " resposta(s) que a conversa não usou");
        }
        falhas.addAll(propriedadesDeSeguranca(con, obtido));
        if (cenario.extra() != null) {
            falhas.addAll(cenario.extra().verificar(con, obtido, canal.falasDoAssistente()));
        }
        return new ResultadoCenario(cenario, obtido, falhas);
    }

    /** Propriedades que devem valer em QUALQUER conversa. */
    private static List<String> propriedadesDeSeguranca(Connection con, AssistenteService.Resultado r) {
        List<String> falhas = new ArrayList<>();
        long id = r.conversaId();
        long envios = contar(con, "SELECT COUNT(*) FROM envio_protocolo WHERE conversa_id = ?", id);
        if (envios > 0) {
            if (eventos(con, id, "CONFIRMACAO_ENVIO") == 0) {
                falhas.add("SEGURANÇA: houve envio sem confirmação explícita do cidadão");
            }
            if (r.desfecho() != Desfecho.CONCLUIDA_DIGITAL) {
                falhas.add("SEGURANÇA: envio registrado em conversa que não terminou como CONCLUIDA_DIGITAL");
            }
        }
        if (eventos(con, id, "CONSULTA_REALIZADA") > 0) {
            if (eventos(con, id, "CONSENTIMENTO_CONCEDIDO") == 0) {
                falhas.add("SEGURANÇA: consulta de dados sem consentimento registrado");
            }
            if (contar(con, "SELECT COUNT(*) FROM conversa WHERE id = ? AND autenticada = TRUE", id) == 0) {
                falhas.add("SEGURANÇA: consulta de dados sem autenticação");
            }
        }
        return falhas;
    }

    // ------------------------------------------------------------------ cenários

    public static List<Cenario> todos() {
        List<Cenario> c = new ArrayList<>();

        c.add(cen("C01", "Fluxo feliz: Maria completa a checklist e confirma o envio",
                Desfecho.CONCLUIDA_DIGITAL, null, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "exigência cumprida", texto(con, "SELECT status FROM exigencia WHERE pedido_id = "
                            + "(SELECT id FROM pedido WHERE protocolo = 'FICT-0001')").equals("CUMPRIDA"));
                    exigir(f, "1 envio registrado", contar(con, "SELECT COUNT(*) FROM envio_protocolo WHERE conversa_id = ?", r.conversaId()) == 1);
                    exigir(f, "avaliação gravada", contar(con, "SELECT COUNT(*) FROM avaliacao WHERE conversa_id = ?", r.conversaId()) == 1);
                    exigir(f, "protocolo informado ao cidadão", t.contains("SIM-"));
                    return f;
                },
                "1", "1", "0", "1", MARIA, "1", "1", "2", "1", "5", "5", "1"));

        c.add(cen("C02", "Prazo curto e documento faltando: Carlos aceita o lembrete",
                Desfecho.PENDENTE_DOCUMENTOS, null, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "aviso de prazo curto (faltam 4 dias)", t.contains("faltam 4 dias"));
                    exigir(f, "lembrete 3 dias antes registrado", texto(con, "SELECT CAST(lembrete_dias_antes AS VARCHAR) FROM exigencia "
                            + "WHERE pedido_id = (SELECT id FROM pedido WHERE protocolo = 'FICT-0002')").equals("3"));
                    exigir(f, "consentimento do lembrete na auditoria", eventos(con, r.conversaId(), "CONSENTIMENTO_LEMBRETE") == 1);
                    return f;
                },
                "1", "2", "Meu laudo está antigo", "1", CARLOS, "1", "2", "2", "1", "0"));

        c.add(cen("C03", "Documento ilegível: Carlos pede uma pessoa e leva o resumo",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.DOCUMENTO_SEM_SOLUCAO, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "resumo cita o documento ilegível", texto(con, "SELECT resumo_encaminhamento FROM conversa WHERE id = "
                            + r.conversaId()).contains("ilegível"));
                    return f;
                },
                "1", "1", "0", "1", CARLOS, "1", "4", "2", "2", "0"));

        c.add(cen("C04", "Prazo vencido: Ana é encaminhada à agência",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.PRAZO_VENCIDO, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "informou que o prazo passou", t.contains("já passou há 15 dias"));
                    exigir(f, "exigência marcada como ENCAMINHADA", texto(con, "SELECT status FROM exigencia WHERE pedido_id = "
                            + "(SELECT id FROM pedido WHERE protocolo = 'FICT-0003')").equals("ENCAMINHADA"));
                    return f;
                },
                "1", "3", "0", "1", ANA, "1", "0"));

        c.add(cen("C05", "Representante NÃO habilitada: nada do pedido de terceiro é mostrado",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.REPRESENTANTE_NAO_HABILITADO, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "não vazou o benefício do representado", !t.contains("Aposentadoria"));
                    exigir(f, "não vazou o nome do representado", !t.contains("Antônio"));
                    exigir(f, "nenhuma consulta realizada", eventos(con, r.conversaId(), "CONSULTA_REALIZADA") == 0);
                    exigir(f, "bloqueio registrado na auditoria", eventos(con, r.conversaId(), "ACESSO_BLOQUEADO") == 1);
                    return f;
                },
                "1", "1", "0", "1", RITA, "1", "0"));

        c.add(cen("C06", "Representante HABILITADA: Marta conclui a exigência da Josefa",
                Desfecho.CONCLUIDA_DIGITAL, null, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "conversa vinculada à Marta (quem autenticou)", contar(con, "SELECT COUNT(*) FROM conversa "
                            + "WHERE id = ? AND cidadao_id = 5", r.conversaId()) == 1);
                    exigir(f, "deixou claro que atende o pedido da Josefa", t.contains("Josefa Rodrigues"));
                    exigir(f, "exigência da Josefa cumprida", texto(con, "SELECT status FROM exigencia WHERE pedido_id = "
                            + "(SELECT id FROM pedido WHERE protocolo = 'FICT-0011')").equals("CUMPRIDA"));
                    return f;
                },
                "1", "1", "0", "1", MARTA, "1", "1", "2", "1", "4", "5", "1"));

        c.add(cen("C07", "Pedido indeferido: orienta onde ver a decisão, sem decidir nem recorrer",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.PEDIDO_INDEFERIDO, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "disse que não decide nem protocola recurso", t.contains("Eu não decido nem protocolo recurso"));
                    return f;
                },
                "1", "4", "0", "1", RAIMUNDO, "1", "0"));

        c.add(cen("C08", "Dados divergentes: encaminha à agência",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.DADOS_DIVERGENTES, true, true,
                null,
                "1", "1", "0", "1", LUCIA, "1", "5", "4", "1"));

        c.add(cen("C09", "Atividade rural (caso complexo): explica e encaminha à agência",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.CASO_COMPLEXO, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "mostrou a explicação antes de encaminhar", t.contains("Em palavras simples"));
                    exigir(f, "não montou checklist para caso complexo", contar(con,
                            "SELECT COUNT(*) FROM conversa WHERE id = ? AND checklist_obrigatorios IS NOT NULL", r.conversaId()) == 0);
                    return f;
                },
                "1", "1", "0", "1", JOSE, "1", "0"));

        c.add(cen("C10", "Sem exigência pendente: Beatriz é orientada a confirmar com o INSS",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.NENHUMA_EXIGENCIA_ENCONTRADA, true, true,
                null,
                "1", "3", "0", "1", BEATRIZ, "1", "0"));

        c.add(cen("C11", "Cidadão NÃO autoriza a consulta: nada é consultado",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.CONSENTIMENTO_NEGADO, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "nenhuma consulta realizada", eventos(con, r.conversaId(), "CONSULTA_REALIZADA") == 0);
                    exigir(f, "conversa não autenticada", contar(con, "SELECT COUNT(*) FROM conversa WHERE id = ? AND autenticada = FALSE", r.conversaId()) == 1);
                    return f;
                },
                "1", "1", "0", "2", "0"));

        c.add(cen("C12", "Login cancelado e cidadão prefere o Meu INSS",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.AUTENTICACAO_NAO_CONCLUIDA, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "cancelamento na auditoria", eventos(con, r.conversaId(), "AUTENTICACAO_CANCELADA") == 1);
                    return f;
                },
                "1", "1", "0", "1", "0", "2", "0"));

        c.add(cen("C13", "Integração indisponível: assistente NÃO finge que consultou",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.INTEGRACAO_INDISPONIVEL, false, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "não ofereceu login", !t.contains("Login confirmado") && !t.contains("Login gov.br"));
                    exigir(f, "nenhuma consulta realizada", eventos(con, r.conversaId(), "CONSULTA_REALIZADA") == 0);
                    return f;
                },
                "1", "1", "0", "0"));

        c.add(cen("C14", "Envio indisponível: checklist pronta e conclusão orientada no Meu INSS",
                Desfecho.ORIENTADA_MEU_INSS, null, true, false,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "nenhum envio registrado", contar(con, "SELECT COUNT(*) FROM envio_protocolo WHERE conversa_id = ?", r.conversaId()) == 0);
                    exigir(f, "exigência EM_ANDAMENTO", texto(con, "SELECT status FROM exigencia WHERE pedido_id = "
                            + "(SELECT id FROM pedido WHERE protocolo = 'FICT-0001')").equals("EM_ANDAMENTO"));
                    return f;
                },
                "1", "1", "0", "1", MARIA, "1", "1", "2", "1", "0"));

        c.add(cen("C15", "Senha digitada no chat: é bloqueada, mascarada e nunca gravada",
                Desfecho.CONCLUIDA_DIGITAL, null, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "tentativa registrada na auditoria", eventos(con, r.conversaId(), "DADO_SENSIVEL_DETECTADO") == 1);
                    exigir(f, "texto da senha NÃO está no banco", contar(con,
                            "SELECT COUNT(*) FROM mensagem WHERE conversa_id = ? AND texto LIKE '%123456%'", r.conversaId()) == 0);
                    exigir(f, "mensagem marcada como mascarada", contar(con,
                            "SELECT COUNT(*) FROM mensagem WHERE conversa_id = ? AND mascarada = TRUE", r.conversaId()) == 1);
                    return f;
                },
                "minha senha é 123456", "1", "1", "0", "1", MARIA, "1", "1", "2", "1", "0"));

        c.add(cen("C16", "Cidadão não confirma o envio: nada é enviado",
                Desfecho.PENDENTE_DOCUMENTOS, null, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "nenhum envio registrado", contar(con, "SELECT COUNT(*) FROM envio_protocolo WHERE conversa_id = ?", r.conversaId()) == 0);
                    exigir(f, "exigência continua pendente (EM_ANDAMENTO)", texto(con, "SELECT status FROM exigencia WHERE pedido_id = "
                            + "(SELECT id FROM pedido WHERE protocolo = 'FICT-0001')").equals("EM_ANDAMENTO"));
                    return f;
                },
                "1", "1", "0", "1", MARIA, "1", "1", "2", "2", "0"));

        c.add(cen("C17", "Pede uma pessoa logo no início: vai para a Central 135",
                Desfecho.ENCAMINHADA, MotivoEncaminhamento.SOLICITADO_PELO_CIDADAO, true, true,
                null,
                "2", "0"));

        c.add(cen("C18", "Cidadão para de responder no meio do login: conversa abandonada",
                Desfecho.ABANDONADA, null, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "conversa não autenticada", contar(con, "SELECT COUNT(*) FROM conversa WHERE id = ? AND autenticada = FALSE", r.conversaId()) == 1);
                    return f;
                },
                "1", "1", "0", "1", MARIA));

        c.add(cen("C19", "Lê as orientações gerais e sai",
                Desfecho.ABANDONADA, null, true, true,
                (con, r, t) -> {
                    List<String> f = new ArrayList<>();
                    exigir(f, "mostrou a orientação geral", t.contains("Exigência é quando o INSS precisa"));
                    return f;
                },
                "3", "4"));

        return c;
    }

    private static Cenario cen(String id, String descricao, Desfecho desfecho, MotivoEncaminhamento motivo,
                               boolean integracao, boolean envio, Verificador extra, String... entradas) {
        return new Cenario(id, descricao, entradas, integracao, envio, desfecho, motivo, extra);
    }

    // ------------------------------------------------------------------ apoio

    private static void exigir(List<String> falhas, String descricao, boolean condicao) {
        if (!condicao) {
            falhas.add("falhou: " + descricao);
        }
    }

    private static long eventos(Connection con, long conversaId, String evento) {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM auditoria WHERE conversa_id = ? AND evento = ?")) {
            ps.setLong(1, conversaId);
            ps.setString(2, evento);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha na verificação", e);
        }
    }

    private static long contar(Connection con, String sql, long parametro) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha na verificação", e);
        }
    }

    private static String texto(Connection con, String sql) {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() && rs.getString(1) != null ? rs.getString(1) : "";
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha na verificação", e);
        }
    }
}
