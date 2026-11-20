package br.com.fiap.citizentech.inss.modelo;

/** Tentativa de acessar dado sem autenticação, consentimento, escopo ou titularidade. */
public class AcessoNegadoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
