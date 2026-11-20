package br.com.fiap.citizentech.inss.servico;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Impede que credenciais e dados sensíveis digitados no chat sejam guardados.
 * Prefere errar para o lado da segurança: pode haver falso positivo (ex.: um número de 6 dígitos
 * qualquer), mas não deixa uma senha ou um código de verificação chegar ao banco.
 */
public class DetectorDadosSensiveis {
    public static final String AVISO =
            "Por segurança, não envie senha, código de verificação, CPF ou número de cartão por aqui. "
                    + "Eu nunca peço isso no chat. Apaguei essa informação do registro.";

    public record Analise(boolean sensivel, String textoParaRegistro, List<String> tipos) {
    }

    private static final Pattern CREDENCIAL = Pattern.compile(
            "(?i)\\b(senha|token|c[oó]digo\\s+de\\s+(acesso|verifica[cç][aã]o|autentica[cç][aã]o|seguran[cç]a))\\b");
    private static final Pattern CARTAO = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern CODIGO = Pattern.compile("\\b\\d{6}\\b");

    public Analise analisar(String texto) {
        if (texto == null || texto.isBlank()) {
            return new Analise(false, "", List.of());
        }
        if (CREDENCIAL.matcher(texto).find()) {
            // Mensagem inteira é descartada: mascarar só a palavra deixaria a senha no texto.
            return new Analise(true, "[mensagem ocultada: possível credencial]", List.of("CREDENCIAL"));
        }
        List<String> tipos = new ArrayList<>();
        String mascarado = texto;
        if (CARTAO.matcher(mascarado).find()) {
            mascarado = CARTAO.matcher(mascarado).replaceAll("[CARTAO REMOVIDO]");
            tipos.add("CARTAO");
        }
        if (CPF.matcher(mascarado).find()) {
            mascarado = CPF.matcher(mascarado).replaceAll("[CPF REMOVIDO]");
            tipos.add("CPF");
        }
        if (CODIGO.matcher(mascarado).find()) {
            mascarado = CODIGO.matcher(mascarado).replaceAll("[CODIGO REMOVIDO]");
            tipos.add("CODIGO");
        }
        return new Analise(!tipos.isEmpty(), mascarado, tipos);
    }
}
