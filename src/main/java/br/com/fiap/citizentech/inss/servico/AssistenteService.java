package br.com.fiap.citizentech.inss.servico;

import br.com.fiap.citizentech.inss.dao.AuditoriaDao;
import br.com.fiap.citizentech.inss.dao.AvaliacaoDao;
import br.com.fiap.citizentech.inss.dao.ChecklistDao;
import br.com.fiap.citizentech.inss.dao.CidadaoDao;
import br.com.fiap.citizentech.inss.dao.ConversaDao;
import br.com.fiap.citizentech.inss.dao.ExigenciaDao;
import br.com.fiap.citizentech.inss.dao.MensagemDao;
import br.com.fiap.citizentech.inss.db.Transacao;
import br.com.fiap.citizentech.inss.modelo.AcessoNegadoException;
import br.com.fiap.citizentech.inss.modelo.Autor;
import br.com.fiap.citizentech.inss.modelo.Avaliacao;
import br.com.fiap.citizentech.inss.modelo.Checklist;
import br.com.fiap.citizentech.inss.modelo.Cidadao;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.modelo.Encaminhamento;
import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;
import br.com.fiap.citizentech.inss.modelo.Escopo;
import br.com.fiap.citizentech.inss.modelo.Exigencia;
import br.com.fiap.citizentech.inss.modelo.FimDeEntradaException;
import br.com.fiap.citizentech.inss.modelo.ItemChecklist;
import br.com.fiap.citizentech.inss.modelo.MotivoContato;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Origem;
import br.com.fiap.citizentech.inss.modelo.Pedido;
import br.com.fiap.citizentech.inss.modelo.PedidoComExigencia;
import br.com.fiap.citizentech.inss.modelo.StatusExigencia;
import br.com.fiap.citizentech.inss.modelo.StatusItem;
import br.com.fiap.citizentech.inss.modelo.TipoAcesso;
import br.com.fiap.citizentech.inss.servico.AutenticacaoSimulada.Sessao;
import br.com.fiap.citizentech.inss.ui.Canal;

import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orquestra o atendimento: conversa, consentimento, autenticação, consulta, checklist,
 * confirmação e encaminhamento. Cada passo grava no banco (conversa, mensagens, checklist, auditoria).
 *
 * O serviço NUNCA decide direito a benefício, NUNCA pede senha e NUNCA envia nada sem confirmação explícita.
 */
public class AssistenteService {
    private static final String SAUDACAO = """
            Olá! Eu sou o assistente de serviços do INSS no WhatsApp oficial.
            (Protótipo acadêmico da FIAP: todos os dados são fictícios.)

            Combinado de segurança: eu nunca peço senha, código de verificação nem pagamento por aqui.
            A decisão sobre o seu benefício é sempre do INSS; eu ajudo você a entender e a organizar.""";

    private static final String ORIENTACAO_RAPIDA = """
            Enquanto isso, o que vale para qualquer exigência: em regra, o prazo para cumprir é de 30 dias \
            contados da ciência (o prazo do seu caso aparece na própria exigência). \
            Se ela não for cumprida, o pedido pode ser indeferido ou encerrado.""";

    private static final String ORIENTACAO_GERAL = """
            Exigência é quando o INSS precisa de um documento ou de um esclarecimento para continuar a análise do pedido.
            - Onde ver: no Meu INSS (site ou aplicativo) ou pelo telefone 135.
            - Prazo: em regra, 30 dias contados da ciência. Se precisar de mais tempo, pergunte ao INSS sobre pedir prorrogação, antes de o prazo acabar.
            - Como cumprir: pelo Meu INSS (opção de cumprir exigência) ou presencialmente, conforme o caso.
            - Cuidado com golpes: desconfie de quem pedir senha, código ou pagamento. Eu não peço.""";

    /** Resultado de um atendimento, útil para testes e relatórios. */
    public record Resultado(long conversaId, Desfecho desfecho, MotivoEncaminhamento motivoEncaminhamento) {
    }

    /** Estado de uma conversa em andamento. */
    private static final class Contexto {
        final Canal canal;
        final long conversaId;
        Sessao sessao;
        Cidadao cidadao;
        PedidoComExigencia alvo;
        Checklist checklist;
        String relato;
        boolean finalizada;
        Resultado resultado;

        Contexto(Canal canal, long conversaId) {
            this.canal = canal;
            this.conversaId = conversaId;
        }
    }

    private final Connection con;
    private final Clock clock;
    private final InssGateway gateway;
    private final AutenticacaoSimulada autenticacao;
    private final ExplicadorExigencia explicador;
    private final DetectorDadosSensiveis detector = new DetectorDadosSensiveis();

    private final CidadaoDao cidadaoDao;
    private final ExigenciaDao exigenciaDao;
    private final ChecklistDao checklistDao;
    private final ConversaDao conversaDao;
    private final MensagemDao mensagemDao;
    private final AvaliacaoDao avaliacaoDao;
    private final AuditoriaDao auditoriaDao;

    public AssistenteService(Connection con, Clock clock, InssGateway gateway, AutenticacaoSimulada autenticacao,
                             ExplicadorExigencia explicador) {
        this.con = con;
        this.clock = clock;
        this.gateway = gateway;
        this.autenticacao = autenticacao;
        this.explicador = explicador;
        this.cidadaoDao = new CidadaoDao(con);
        this.exigenciaDao = new ExigenciaDao(con);
        this.checklistDao = new ChecklistDao(con);
        this.conversaDao = new ConversaDao(con);
        this.mensagemDao = new MensagemDao(con);
        this.avaliacaoDao = new AvaliacaoDao(con);
        this.auditoriaDao = new AuditoriaDao(con);
    }

    // ------------------------------------------------------------------ ponto de entrada

    public Resultado atender(Canal canal, Origem origem, String rotulo) {
        long conversaId = conversaDao.iniciar(origem, rotulo, agora());
        Contexto c = new Contexto(canal, conversaId);
        Resultado resultado;
        try {
            resultado = conduzir(c);
        } catch (FimDeEntradaException e) {
            if (c.finalizada) {
                resultado = c.resultado;
            } else {
                conversaDao.finalizar(conversaId, Desfecho.ABANDONADA, null, null, agora());
                auditar(c, "CONVERSA_FINALIZADA", Desfecho.ABANDONADA.name());
                resultado = new Resultado(conversaId, Desfecho.ABANDONADA, null);
            }
        }
        autenticacao.encerrar(c.sessao);
        return resultado;
    }

    private Resultado conduzir(Contexto c) {
        dizer(c, SAUDACAO);
        while (true) {
            int opcao = menu(c, "Como posso ajudar?",
                    "Entender e resolver uma pendência (exigência) do meu pedido",
                    "Falar com uma pessoa do INSS",
                    "Ver orientações gerais sobre exigências",
                    "Sair");
            switch (opcao) {
                case 1 -> {
                    return fluxoPendencia(c);
                }
                case 2 -> {
                    return encaminhar(c, MotivoEncaminhamento.SOLICITADO_PELO_CIDADAO);
                }
                case 3 -> dizer(c, ORIENTACAO_GERAL);
                default -> {
                    dizer(c, "Tudo bem! Quando precisar, é só chamar. Até logo.");
                    return encerrar(c, Desfecho.ABANDONADA, null, null, null);
                }
            }
        }
    }

    // ------------------------------------------------------------------ fluxo principal

    private Resultado fluxoPendencia(Contexto c) {
        MotivoContato[] motivos = MotivoContato.values();
        String[] textos = new String[motivos.length];
        for (int i = 0; i < motivos.length; i++) {
            textos[i] = motivos[i].texto();
        }
        MotivoContato motivo = motivos[menu(c, "O que está mais difícil para você?", textos) - 1];
        conversaDao.registrarMotivoContato(c.conversaId, motivo);
        dizer(c, motivo.dica());

        dizer(c, "Se quiser, escreva em uma frase o que está acontecendo (não escreva senha, código nem CPF). "
                + "Para pular, envie 0.");
        c.relato = lerRelato(c);
        dizer(c, ORIENTACAO_RAPIDA);

        if (!gateway.integracaoDisponivel()) {
            auditar(c, "INTEGRACAO_INDISPONIVEL", "consulta não realizada");
            return encaminhar(c, MotivoEncaminhamento.INTEGRACAO_INDISPONIVEL);
        }

        String escopos = EnumSet.allOf(Escopo.class).stream().map(Escopo::descricao).collect(Collectors.joining(", "));
        dizer(c, "Para olhar o SEU pedido eu preciso da sua autorização. Vou consultar só: " + escopos
                + ".\nO login é feito no navegador seguro do gov.br, fora do WhatsApp. Eu não vejo nem guardo a sua senha.");
        if (menu(c, "Você autoriza?", "Autorizo e quero entrar com gov.br", "Não autorizo") == 2) {
            auditar(c, "CONSENTIMENTO_NEGADO", "");
            return encaminhar(c, MotivoEncaminhamento.CONSENTIMENTO_NEGADO);
        }
        auditar(c, "CONSENTIMENTO_CONCEDIDO", "escopos=" + escopos);

        for (int tentativa = 1; tentativa <= 3 && c.sessao == null; tentativa++) {
            c.sessao = loginSimulado(c);
            if (c.sessao == null && tentativa < 3) {
                int op = menu(c, "Não consegui concluir o login. O que prefere?",
                        "Tentar de novo", "Consultar pelo Meu INSS ou pelo 135");
                if (op == 2) {
                    break;
                }
            }
        }
        if (c.sessao == null) {
            return encaminhar(c, MotivoEncaminhamento.AUTENTICACAO_NAO_CONCLUIDA);
        }
        return seguirComSessao(c);
    }

    /** Etapa fora do chat. No protótipo, a pessoa escolhe um perfil fictício e autoriza os escopos. */
    private Sessao loginSimulado(Contexto c) {
        dizer(c, "Vou abrir o link oficial do gov.br (simulado): https://sso.acesso.gov.br/simulado\n"
                + "Entre por lá, no navegador, e volte aqui. Nunca digite a senha do gov.br no WhatsApp.");
        List<Cidadao> perfis = cidadaoDao.listar();
        StringBuilder sb = new StringBuilder("Login gov.br (SIMULADO). Escolha o perfil fictício para entrar:\n");
        for (int i = 0; i < perfis.size(); i++) {
            Cidadao p = perfis.get(i);
            sb.append("  ").append(i + 1).append(") ").append(p.getNomeFicticio())
                    .append(" [").append(p.getTipoAcesso().rotulo()).append("]\n");
        }
        sb.append("  0) Cancelar o login");
        c.canal.navegador(sb.toString());
        int escolha = lerNumero(c, 0, perfis.size(), false);
        if (escolha == 0) {
            auditar(c, "AUTENTICACAO_CANCELADA", "");
            return null;
        }
        Cidadao quem = perfis.get(escolha - 1);
        c.canal.navegador("O Assistente INSS pede autorização para consultar: situação do pedido, texto da exigência, "
                + "prazo e protocolo.\n  1) Autorizar\n  2) Negar");
        if (lerNumero(c, 1, 2, false) == 2) {
            auditar(c, "AUTORIZACAO_NEGADA", "");
            return null;
        }
        Sessao sessao = autenticacao.iniciarSessao(quem, EnumSet.allOf(Escopo.class));
        conversaDao.vincularAutenticacao(c.conversaId, quem.getId(), autenticacao.referenciaSegura(sessao));
        c.cidadao = quem;
        auditar(c, "AUTENTICACAO_CONCLUIDA", "sessão de " + AutenticacaoSimulada.VALIDADE.toMinutes() + " min");
        c.canal.navegador("Login concluído. Pode voltar para a conversa no WhatsApp.");
        return sessao;
    }

    private Resultado seguirComSessao(Contexto c) {
        dizer(c, "Oi, " + c.cidadao.primeiroNome() + "! Login confirmado. Vou consultar só o que você autorizou.");
        Optional<MotivoEncaminhamento> bloqueio = AvaliadorDeCaso.bloqueioPorAcesso(c.cidadao.getTipoAcesso());
        if (bloqueio.isPresent()) {
            auditar(c, "ACESSO_BLOQUEADO", bloqueio.get().name());
            return encaminhar(c, bloqueio.get());
        }

        List<PedidoComExigencia> pedidos;
        try {
            pedidos = gateway.consultarPedidos(c.sessao);
        } catch (AcessoNegadoException e) {
            auditar(c, "ACESSO_NEGADO", e.getMessage());
            return encaminhar(c, MotivoEncaminhamento.AUTENTICACAO_NAO_CONCLUIDA);
        }
        auditar(c, "CONSULTA_REALIZADA", "pedidos=" + pedidos.size());
        if (pedidos.isEmpty()) {
            return encaminhar(c, MotivoEncaminhamento.NENHUMA_EXIGENCIA_ENCONTRADA);
        }

        c.alvo = escolherPedido(c, pedidos);
        Pedido pedido = c.alvo.pedido();
        Exigencia exigencia = c.alvo.exigenciaPendente();
        if (c.cidadao.getTipoAcesso() == TipoAcesso.REPRESENTANTE_HABILITADO) {
            cidadaoDao.buscarPorId(pedido.getCidadaoId())
                    .ifPresent(t -> dizer(c, "Você está atendendo o pedido de " + t.getNomeFicticio() + " (representação habilitada)."));
        }
        dizer(c, "Pedido " + pedido.getProtocolo() + " - " + pedido.getBeneficio() + ".");
        if (exigencia != null) {
            conversaDao.vincularExigencia(c.conversaId, exigencia.getId());
        }

        Optional<MotivoEncaminhamento> parar =
                AvaliadorDeCaso.avaliar(c.cidadao.getTipoAcesso(), pedido, exigencia, hoje());
        if (parar.isPresent()) {
            MotivoEncaminhamento m = parar.get();
            if (exigencia != null && (m == MotivoEncaminhamento.CASO_COMPLEXO || m == MotivoEncaminhamento.PRAZO_VENCIDO)) {
                dizer(c, explicador.explicar(exigencia, hoje()));
            }
            return encaminhar(c, m);
        }

        dizer(c, explicador.explicar(exigencia, hoje()));
        return conduzirChecklist(c, exigencia);
    }

    private PedidoComExigencia escolherPedido(Contexto c, List<PedidoComExigencia> pedidos) {
        if (pedidos.size() == 1) {
            return pedidos.get(0);
        }
        String[] opcoes = pedidos.stream()
                .map(p -> p.pedido().getProtocolo() + " - " + p.pedido().getBeneficio()).toArray(String[]::new);
        return pedidos.get(menu(c, "Encontrei mais de um pedido. Qual deles?", opcoes) - 1);
    }

    // ------------------------------------------------------------------ checklist

    private Resultado conduzirChecklist(Contexto c, Exigencia exigencia) {
        Checklist lista = checklistDao.listar(exigencia.getId());
        if (lista.itens().isEmpty()) {
            checklistDao.gerarAPartirDoModelo(exigencia.getId(), exigencia.getTipo().getId());
            lista = checklistDao.listar(exigencia.getId());
        }
        if (exigencia.getStatus() == StatusExigencia.ABERTA) {
            exigenciaDao.atualizarStatus(exigencia.getId(), StatusExigencia.EM_ANDAMENTO, null);
            exigencia.atualizarStatus(StatusExigencia.EM_ANDAMENTO, null);
        }
        c.checklist = lista;

        dizer(c, "Vamos montar a sua lista de documentos. Para cada item, diga como você está.");
        int total = lista.itens().size();
        int numero = 0;
        for (ItemChecklist item : lista.itens()) {
            numero++;
            dizer(c, "Item " + numero + " de " + total + ": " + item.getNome()
                    + (item.isObrigatorio() ? "  [obrigatório]" : "  [opcional]")
                    + "\nDica: " + item.getOrientacao());
            int op = menu(c, null, "Tenho e está legível", "Não tenho", "Não sei se serve",
                    "Tenho, mas está ilegível ou incompleto");
            StatusItem novo = switch (op) {
                case 1 -> StatusItem.PRONTO;
                case 2 -> StatusItem.PENDENTE;
                case 3 -> StatusItem.DUVIDA;
                default -> StatusItem.ILEGIVEL;
            };
            checklistDao.atualizarStatus(item.getId(), novo, agora());
            item.setStatus(novo);
            dizer(c, switch (novo) {
                case PRONTO -> "Anotado.";
                case PENDENTE -> "Sem problema. Como conseguir: " + item.getOrientacao();
                case DUVIDA -> "Boa pergunta. Confirme com o INSS se serve (135 ou Meu INSS). Enquanto isso: " + item.getOrientacao();
                case ILEGIVEL -> "Tente refazer a foto ou a digitalização: boa luz, sem cortar as bordas, frente e verso. "
                        + "Se não conseguir, pergunte ao INSS quais alternativas são aceitas.";
                default -> "Anotado.";
            });
        }
        c.checklist = new Checklist(lista.itens());
        int obrigatorios = (int) lista.itens().stream().filter(ItemChecklist::isObrigatorio).count();
        conversaDao.registrarChecklist(c.conversaId, obrigatorios, obrigatorios - c.checklist.obrigatoriosPendentes().size());
        return c.checklist.obrigatoriosProntos()
                ? confirmarEnvio(c, exigencia)
                : orientarPendencias(c, exigencia);
    }

    private Resultado confirmarEnvio(Contexto c, Exigencia exigencia) {
        String prontos = c.checklist.prontos().stream().map(i -> "  - " + i.getNome()).collect(Collectors.joining("\n"));
        dizer(c, "Tudo o que é obrigatório está pronto. Isto é o que seria enviado ao INSS:\n" + prontos
                + "\n\n(Neste protótipo nenhum arquivo é transmitido de verdade. Em produção você anexaria cada "
                + "documento aqui e eles seguiriam direto para o INSS.)");
        if (menu(c, "Você confirma o envio?", "Confirmo o envio", "Não, quero revisar antes") == 2) {
            dizer(c, "Tudo bem, nada foi enviado. A sua lista fica salva; é só voltar quando quiser.");
            return encerrar(c, Desfecho.PENDENTE_DOCUMENTOS, null, null, null);
        }
        auditar(c, "CONFIRMACAO_ENVIO", "exigencia=" + exigencia.getId());

        if (!gateway.envioDisponivel()) {
            exigenciaDao.atualizarStatus(exigencia.getId(), StatusExigencia.EM_ANDAMENTO, null);
            dizer(c, "Neste momento o envio por aqui não está disponível, mas a sua lista está pronta. "
                    + "Entre no Meu INSS (site ou aplicativo), abra o seu pedido, escolha a opção de cumprir a "
                    + "exigência e anexe estes documentos.");
            return encerrar(c, Desfecho.ORIENTADA_MEU_INSS, null, null, null);
        }

        String[] protocolo = new String[1];
        try {
            Transacao.executar(con, () -> {
                protocolo[0] = gateway.protocolarEnvio(c.sessao, c.conversaId, exigencia.getId());
                checklistDao.marcarProntosComoEnviados(exigencia.getId(), agora());
                exigenciaDao.atualizarStatus(exigencia.getId(), StatusExigencia.CUMPRIDA, hoje());
            });
        } catch (AcessoNegadoException | ErroDeBancoException e) {
            auditar(c, "ENVIO_FALHOU", e.getClass().getSimpleName());
            dizer(c, "Não consegui registrar o envio e nada foi enviado. Você pode concluir pelo Meu INSS.");
            return encaminhar(c, MotivoEncaminhamento.INTEGRACAO_INDISPONIVEL);
        }
        exigencia.atualizarStatus(StatusExigencia.CUMPRIDA, hoje());
        auditar(c, "ENVIO_REGISTRADO", protocolo[0]);
        dizer(c, "Pronto! Envio registrado. Protocolo (simulado): " + protocolo[0] + ".\n"
                + "Para acompanhar o andamento: Meu INSS ou telefone 135. A decisão sobre o pedido continua sendo do INSS.");
        return encerrar(c, Desfecho.CONCLUIDA_DIGITAL, null, null, null);
    }

    private Resultado orientarPendencias(Contexto c, Exigencia exigencia) {
        String faltam = c.checklist.obrigatoriosPendentes().stream()
                .map(i -> "  - " + i.getNome() + " [" + i.getStatus().descricao() + "]")
                .collect(Collectors.joining("\n"));
        dizer(c, "Ainda falta para completar:\n" + faltam);

        Optional<Integer> lembrete = PoliticaDeLembrete.diasParaLembrete(exigencia.diasRestantes(hoje()));
        List<String> opcoes = new ArrayList<>();
        List<String> acoes = new ArrayList<>();
        if (lembrete.isPresent()) {
            opcoes.add("Quero um lembrete " + lembrete.get() + " dia(s) antes do prazo");
            acoes.add("LEMBRETE");
        } else {
            dizer(c, "Faltam poucos dias para o prazo. Não vale esperar: procure o INSS (135 ou agência) o quanto antes.");
        }
        opcoes.add("Falar com uma pessoa do INSS (levo o resumo do meu caso)");
        acoes.add("HUMANO");
        opcoes.add("Encerrar por aqui (a lista fica salva)");
        acoes.add("ENCERRAR");

        String acao = acoes.get(menu(c, "O que prefere fazer agora?", opcoes.toArray(String[]::new)) - 1);
        switch (acao) {
            case "LEMBRETE" -> {
                exigenciaDao.registrarLembrete(exigencia.getId(), lembrete.orElseThrow());
                auditar(c, "CONSENTIMENTO_LEMBRETE", lembrete.get() + " dia(s) antes");
                dizer(c, "Combinado, lembrete registrado. (No protótipo isso só fica gravado; nenhum aviso real é enviado.)");
                return encerrar(c, Desfecho.PENDENTE_DOCUMENTOS, null, null, null);
            }
            case "HUMANO" -> {
                return encaminhar(c, c.checklist.temDuvidaOuIlegivel()
                        ? MotivoEncaminhamento.DOCUMENTO_SEM_SOLUCAO
                        : MotivoEncaminhamento.SOLICITADO_PELO_CIDADAO);
            }
            default -> {
                dizer(c, "Tudo bem. A sua lista fica salva. Volte quando tiver os documentos.");
                return encerrar(c, Desfecho.PENDENTE_DOCUMENTOS, null, null, null);
            }
        }
    }

    // ------------------------------------------------------------------ encaminhamento e encerramento

    private Resultado encaminhar(Contexto c, MotivoEncaminhamento motivo) {
        Exigencia exigencia = c.alvo == null ? null : c.alvo.exigenciaPendente();
        Encaminhamento destino = motivo.destino();
        dizer(c, motivo.explicacao() + "\n\nPróximo passo: " + instrucao(destino));

        String resumo = null;
        if (destino.atendimentoHumano() && (exigencia != null || c.relato != null)) {
            resumo = ResumoEncaminhamento.montar(motivo, exigencia, c.checklist, c.relato, hoje());
            dizer(c, "Leve este resumo para a pessoa que atender você, assim o seu caso chega organizado:\n" + resumo);
        }
        return encerrar(c, Desfecho.ENCAMINHADA, motivo, resumo, exigencia);
    }

    private static String instrucao(Encaminhamento destino) {
        return switch (destino) {
            case MEU_INSS -> "entre no Meu INSS (site ou aplicativo) com a sua conta gov.br e procure o seu pedido.";
            case CENTRAL_135 -> "ligue para o 135 e conte o seu caso.";
            case AGENCIA_APS -> "agende o atendimento na agência pelo 135 ou pelo Meu INSS e leve um documento de "
                    + "identificação e os documentos da exigência.";
        };
    }

    private Resultado encerrar(Contexto c, Desfecho desfecho, MotivoEncaminhamento motivo, String resumo,
                               Exigencia exigenciaEncaminhada) {
        LocalDateTime fim = agora();
        Transacao.executar(con, () -> {
            if (exigenciaEncaminhada != null && desfecho == Desfecho.ENCAMINHADA
                    && exigenciaEncaminhada.getStatus().pendente()) {
                exigenciaDao.atualizarStatus(exigenciaEncaminhada.getId(), StatusExigencia.ENCAMINHADA, null);
                exigenciaEncaminhada.atualizarStatus(StatusExigencia.ENCAMINHADA, null);
            }
            conversaDao.finalizar(c.conversaId, desfecho, motivo, resumo, fim);
        });
        auditar(c, "CONVERSA_FINALIZADA", desfecho.name() + (motivo == null ? "" : " / " + motivo.name()));
        c.finalizada = true;
        c.resultado = new Resultado(c.conversaId, desfecho, motivo);
        if (desfecho != Desfecho.ABANDONADA) {
            coletarAvaliacao(c);
        }
        return c.resultado;
    }

    private void coletarAvaliacao(Contexto c) {
        dizer(c, "Última coisa, bem rápida (envie 0 para pular).\nDe 1 a 5, o quanto ficou claro o que o INSS pede de você? "
                + "(1 = nada claro, 5 = muito claro)");
        int clareza = lerNumero(c, 0, 5, true);
        if (clareza == 0) {
            dizer(c, "Tudo bem. Obrigado por usar o assistente!");
            return;
        }
        dizer(c, "De 1 a 5, o quanto você ficou satisfeito(a) com este atendimento?");
        int satisfacao = lerNumero(c, 1, 5, true);
        boolean sabe = menu(c, "Você sabe qual é o seu próximo passo?", "Sim", "Ainda não") == 1;
        avaliacaoDao.inserir(c.conversaId, new Avaliacao(clareza, satisfacao, sabe), agora());
        dizer(c, "Obrigado! Sua resposta ajuda a melhorar o serviço.");
    }

    // ------------------------------------------------------------------ entrada e saída

    private void dizer(Contexto c, String texto) {
        c.canal.assistente(texto);
        registrarMensagem(c, Autor.ASSISTENTE, texto, false);
    }

    private int menu(Contexto c, String titulo, String... opcoes) {
        StringBuilder sb = new StringBuilder();
        if (titulo != null) {
            sb.append(titulo).append('\n');
        }
        for (int i = 0; i < opcoes.length; i++) {
            sb.append("  ").append(i + 1).append(") ").append(opcoes[i]).append('\n');
        }
        dizer(c, sb.toString().stripTrailing());
        return lerNumero(c, 1, opcoes.length, true);
    }

    /**
     * Lê um número de min a max. No chat, toda entrada passa pelo detector de dados sensíveis e só a
     * versão mascarada é gravada. Fora do chat (navegador seguro) nada é gravado como mensagem.
     */
    private int lerNumero(Contexto c, int min, int max, boolean noChat) {
        while (true) {
            String bruto = c.canal.cidadao();
            if (bruto == null) {
                throw new FimDeEntradaException();
            }
            bruto = bruto.trim();
            if (noChat) {
                DetectorDadosSensiveis.Analise analise = detector.analisar(bruto);
                registrarMensagem(c, Autor.CIDADAO, analise.textoParaRegistro(), analise.sensivel());
                if (analise.sensivel()) {
                    auditar(c, "DADO_SENSIVEL_DETECTADO", String.join(",", analise.tipos()));
                    dizer(c, DetectorDadosSensiveis.AVISO);
                    continue;
                }
            }
            try {
                int valor = Integer.parseInt(bruto);
                if (valor >= min && valor <= max) {
                    return valor;
                }
            } catch (NumberFormatException ignorada) {
                // cai na mensagem de ajuda abaixo
            }
            String ajuda = "Não entendi. Responda só com um número de " + min + " a " + max + ".";
            if (noChat) {
                dizer(c, ajuda);
            } else {
                c.canal.navegador(ajuda);
            }
        }
    }

    /** Texto livre opcional (0 ou vazio = pular). Dados sensíveis são descartados, nunca guardados. */
    private String lerRelato(Contexto c) {
        String bruto = c.canal.cidadao();
        if (bruto == null) {
            throw new FimDeEntradaException();
        }
        bruto = bruto.trim();
        DetectorDadosSensiveis.Analise analise = detector.analisar(bruto);
        registrarMensagem(c, Autor.CIDADAO, analise.textoParaRegistro(), analise.sensivel());
        if (analise.sensivel()) {
            auditar(c, "DADO_SENSIVEL_DETECTADO", String.join(",", analise.tipos()));
            dizer(c, DetectorDadosSensiveis.AVISO + " Vamos seguir sem esse relato.");
            return null;
        }
        if (bruto.isEmpty() || bruto.equals("0")) {
            return null;
        }
        return bruto.length() > 200 ? bruto.substring(0, 200) : bruto;
    }

    private void registrarMensagem(Contexto c, Autor autor, String texto, boolean mascarada) {
        String t = texto.length() > 2000 ? texto.substring(0, 2000) : texto;
        mensagemDao.inserir(c.conversaId, autor, t, mascarada, agora());
    }

    private void auditar(Contexto c, String evento, String detalhe) {
        auditoriaDao.registrar(c.conversaId, evento, detalhe, agora());
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
