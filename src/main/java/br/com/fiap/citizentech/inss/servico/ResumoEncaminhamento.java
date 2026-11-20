package br.com.fiap.citizentech.inss.servico;

import br.com.fiap.citizentech.inss.modelo.Checklist;
import br.com.fiap.citizentech.inss.modelo.Exigencia;
import br.com.fiap.citizentech.inss.modelo.ItemChecklist;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/** Monta o resumo que o cidadão leva à pessoa do INSS, para que o caso chegue organizado. */
public final class ResumoEncaminhamento {
    private static final int LIMITE = 1400;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ResumoEncaminhamento() {
    }

    public static String montar(MotivoEncaminhamento motivo, Exigencia exigencia, Checklist checklist,
                                String relato, LocalDate hoje) {
        StringBuilder sb = new StringBuilder();
        sb.append("Motivo do encaminhamento: ").append(motivo.name()).append('\n');
        if (exigencia != null) {
            sb.append("Exigência: ").append(exigencia.getTipo().getTitulo())
                    .append(" | prazo registrado ").append(exigencia.getDataPrazo().format(DATA))
                    .append(" (").append(exigencia.diasRestantes(hoje)).append(" dia(s) restante(s))\n");
        }
        if (checklist != null && !checklist.itens().isEmpty()) {
            String prontos = checklist.prontos().stream().map(ItemChecklist::getNome).collect(Collectors.joining("; "));
            sb.append("Documentos prontos: ").append(prontos.isEmpty() ? "nenhum" : prontos).append('\n');
            String pendentes = checklist.itens().stream().filter(i -> !i.getStatus().pronto())
                    .map(i -> i.getNome() + " [" + i.getStatus().descricao() + "]")
                    .collect(Collectors.joining("; "));
            sb.append("Documentos pendentes: ").append(pendentes.isEmpty() ? "nenhum" : pendentes).append('\n');
        }
        if (relato != null && !relato.isBlank()) {
            sb.append("Relato do cidadão: ").append(relato).append('\n');
        }
        String texto = sb.toString().stripTrailing();
        return texto.length() <= LIMITE ? texto : texto.substring(0, LIMITE - 3) + "...";
    }
}
