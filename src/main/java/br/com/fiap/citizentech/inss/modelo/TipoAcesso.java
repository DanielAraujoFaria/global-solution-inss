package br.com.fiap.citizentech.inss.modelo;

/** Como a pessoa autenticada se relaciona com o pedido. */
public enum TipoAcesso {
    TITULAR("titular", true),
    REPRESENTANTE_HABILITADO("representante habilitado", true),
    REPRESENTANTE_NAO_HABILITADO("representante NÃO habilitado", false);

    private final String rotulo;
    private final boolean podeUsarAutomacao;

    TipoAcesso(String rotulo, boolean podeUsarAutomacao) {
        this.rotulo = rotulo;
        this.podeUsarAutomacao = podeUsarAutomacao;
    }

    public String rotulo() {
        return rotulo;
    }

    public boolean podeUsarAutomacao() {
        return podeUsarAutomacao;
    }
}
