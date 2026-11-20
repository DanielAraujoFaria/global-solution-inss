package br.com.fiap.citizentech.inss.servico;

import br.com.fiap.citizentech.inss.modelo.Exigencia;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Explicação baseada em catálogo revisado (determinística). Escolha de projeto: o texto oficial é sempre
 * mostrado sem alteração, e a explicação simples vem ao lado, nunca no lugar. Assim a linguagem simples
 * não muda o sentido do que o INSS pediu e não há risco de uma explicação inventada.
 */
public class ExplicadorPorCatalogo implements ExplicadorExigencia {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String explicar(Exigencia e, LocalDate hoje) {
        StringBuilder sb = new StringBuilder();
        sb.append("O que o INSS escreveu (texto oficial, sem alterações):\n")
                .append("  \"").append(e.getTextoOficial()).append("\"\n\n")
                .append("Em palavras simples:\n  ").append(e.getTipo().getExplicacaoSimples()).append("\n\n")
                .append(frasePrazo(e, hoje));
        return sb.toString();
    }

    static String frasePrazo(Exigencia e, LocalDate hoje) {
        long dias = e.diasRestantes(hoje);
        String data = e.getDataPrazo().format(DATA);
        if (dias < 0) {
            long atraso = -dias;
            return "Prazo registrado: " + data + ". Esse prazo já passou há " + atraso
                    + (atraso == 1 ? " dia." : " dias.");
        }
        if (dias == 0) {
            return "ATENÇÃO: o prazo registrado (" + data + ") termina HOJE.";
        }
        String faltam = dias == 1 ? "falta 1 dia" : "faltam " + dias + " dias";
        if (e.ehUrgente(hoje)) {
            return "ATENÇÃO: o prazo registrado é " + data + " e " + faltam + ". Vale correr.";
        }
        return "Prazo registrado: " + data + " (" + faltam + ").";
    }
}
