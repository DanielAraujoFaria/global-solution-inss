package br.com.fiap.citizentech.inss.modelo;

/** O cidadão parou de responder (fim da entrada). Encerra a conversa como abandonada. */
public class FimDeEntradaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FimDeEntradaException() {
        super("Fim da entrada do cidadão");
    }
}
