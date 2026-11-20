package br.com.fiap.citizentech.inss.modelo;

/** Encapsula SQLException para não espalhar exceções verificadas pelas camadas. */
public class ErroDeBancoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ErroDeBancoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
