package br.com.fiap.citizentech.inss.modelo;

/** Resposta do cidadão às perguntas finais (clareza e satisfação de 1 a 5). */
public record Avaliacao(int clareza, int satisfacao, boolean sabeProximoPasso) {
    public Avaliacao {
        if (clareza < 1 || clareza > 5 || satisfacao < 1 || satisfacao > 5) {
            throw new IllegalArgumentException("Notas devem estar entre 1 e 5");
        }
    }
}
