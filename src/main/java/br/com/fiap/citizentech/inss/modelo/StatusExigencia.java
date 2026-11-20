package br.com.fiap.citizentech.inss.modelo;

public enum StatusExigencia {
    ABERTA, EM_ANDAMENTO, CUMPRIDA, ENCAMINHADA;

    /** Exigência que ainda precisa de ação do cidadão. */
    public boolean pendente() {
        return this != CUMPRIDA;
    }
}
