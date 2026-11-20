package br.com.fiap.citizentech.inss.analise;

import br.com.fiap.citizentech.inss.modelo.Avaliacao;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Origem;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Gera o relatório de indicadores em texto, com a leitura (interpretação) de cada número. */
public final class RelatorioIndicadores {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private RelatorioIndicadores() {
    }

    public static String gerar(Connection con, Origem origem) {
        Indicadores ind = new Indicadores(con);
        StringBuilder sb = new StringBuilder();
        int conversas = ind.conversas(origem).size();
        int finalizadas = ind.finalizadas(origem).size();

        sb.append(String.format(PT_BR, "=== INDICADORES | origem: %s | %d conversas (%d finalizadas) ===%n",
                origem, conversas, finalizadas));
        sb.append(avisoDaOrigem(origem, conversas)).append('\n');
        if (conversas == 0) {
            return sb.append("Sem dados para esta origem.\n").toString();
        }

        sb.append("\n1) Desfechos das conversas\n");
        for (Map.Entry<Desfecho, Long> e : ind.porDesfecho(origem).entrySet()) {
            sb.append(String.format(PT_BR, "   - %-20s %d%n", e.getKey(), e.getValue()));
        }
        Map<MotivoEncaminhamento, Long> motivos = ind.porMotivo(origem);
        if (!motivos.isEmpty()) {
            sb.append("   Motivos de encaminhamento:\n");
            for (Map.Entry<MotivoEncaminhamento, Long> e : motivos.entrySet()) {
                sb.append(String.format(PT_BR, "   - %-30s %d%n", e.getKey(), e.getValue()));
            }
        }

        sb.append("\n2) Taxa de resolução sem atendimento adicional\n   ")
                .append(proporcao(ind.resolvidasSemAtendimentoAdicional(origem), finalizadas)).append('\n')
                .append("   Leitura: parcela das conversas em que a etapa terminou com protocolo (simulado) ou com a lista\n")
                .append("   pronta para concluir no Meu INSS. Não inclui casos que, pelas regras, devem ir para uma pessoa.\n");

        sb.append("\n3) Taxa de encaminhamento para atendimento humano (135 ou agência)\n   ")
                .append(proporcao(ind.encaminhadasParaPessoa(origem), finalizadas)).append('\n')
                .append("   Leitura: um valor alto NÃO é ruim por si só; casos complexos, prazo vencido e dados divergentes\n")
                .append("   devem ser encaminhados. O que se mede é se o encaminhamento foi o adequado (ver validação por cenários).\n");

        sb.append("\n4) Checklists concluídas (todos os documentos obrigatórios prontos)\n   ")
                .append(proporcao(ind.comChecklistCompleta(origem), ind.comChecklist(origem))).append('\n');

        List<Long> dias = ind.diasDaNotificacaoAteOEnvio(origem);
        sb.append("\n5) Dias entre a notificação (emissão da exigência) e o envio\n   ")
                .append(resumo(dias, "dias")).append('\n');

        if (origem == Origem.SIMULACAO) {
            sb.append("\n6) Duração, clareza, satisfação e \"sabe o próximo passo\"\n")
                    .append("   Não se aplicam à simulação: as respostas são roteirizadas, não de pessoas reais.\n")
                    .append("   Estes indicadores só valem na origem TESTE_USUARIO.\n");
        } else {
            sb.append("\n6) Duração da conversa até o desfecho\n   ").append(resumo(ind.duracoesEmSegundos(origem), "s")).append('\n');
            List<Avaliacao> avaliacoes = ind.avaliacoes(origem);
            sb.append("\n7) Clareza da exigência (1 a 5)\n   ")
                    .append(resumo(avaliacoes.stream().map(Avaliacao::clareza).toList(), "pontos")).append('\n');
            sb.append("\n8) Satisfação (1 a 5)\n   ")
                    .append(resumo(avaliacoes.stream().map(Avaliacao::satisfacao).toList(), "pontos")).append('\n');
            long sabem = avaliacoes.stream().filter(Avaliacao::sabeProximoPasso).count();
            sb.append("\n9) Pessoas que sabem qual é o próximo passo\n   ")
                    .append(proporcao(sabem, avaliacoes.size())).append('\n');
        }

        sb.append("\n10) Privacidade e segurança (toda a base)\n")
                .append(String.format(PT_BR, "   - Tentativas de enviar dado sensível no chat, bloqueadas: %d%n",
                        ind.tentativasDeEnvioDeDadoSensivelBloqueadas()))
                .append(String.format(PT_BR, "   - Mensagens de cidadãos com dado sensível gravado SEM máscara: %d (esperado: 0)%n",
                        ind.mensagensDeCidadaosComDadoSensivelSemMascara()));
        return sb.toString();
    }

    private static String avisoDaOrigem(Origem origem, int conversas) {
        return switch (origem) {
            case SIMULACAO -> "AVISO: conversas geradas por cenários automatizados (personas roteirizadas). "
                    + "Validam as REGRAS do sistema; não representam pessoas reais.";
            case TESTE_USUARIO -> conversas < 5
                    ? "AVISO: menos de 5 sessões. Amostra insuficiente para qualquer conclusão; use só como ilustração."
                    : conversas < 30
                    ? "AVISO: amostra pequena (n < 30). Interprete com cautela; os intervalos de confiança são largos."
                    : "Amostra com 30 ou mais sessões.";
            case DEMONSTRACAO -> "AVISO: conversas de demonstração (não são amostra de usuários; as respostas do modo --demo são do roteiro).";
        };
    }

    private static String proporcao(long sucessos, long total) {
        if (total == 0) {
            return "sem dados";
        }
        double[] ic = Estatistica.intervaloWilson(sucessos, total);
        return String.format(PT_BR, "%d de %d = %.1f%%  (IC 95%% de Wilson: %.1f%% a %.1f%%)",
                sucessos, total, 100 * Estatistica.proporcao(sucessos, total), 100 * ic[0], 100 * ic[1]);
    }

    private static String resumo(List<? extends Number> valores, String unidade) {
        if (valores.isEmpty()) {
            return "sem dados";
        }
        StringBuilder sb = new StringBuilder(String.format(PT_BR,
                "n = %d | média %.2f | mediana %.2f | desvio padrão %.2f | mín %.2f | máx %.2f (%s)",
                valores.size(), Estatistica.media(valores), Estatistica.mediana(valores),
                Estatistica.desvioPadrao(valores), Estatistica.minimo(valores), Estatistica.maximo(valores), unidade));
        if (valores.size() >= 2) {
            double[] ic = Estatistica.intervaloDaMedia(valores);
            sb.append(String.format(PT_BR, "%n   IC 95%% da média: %.2f a %.2f", ic[0], ic[1]));
        }
        return sb.toString();
    }
}
