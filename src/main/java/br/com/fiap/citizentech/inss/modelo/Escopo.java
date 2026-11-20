package br.com.fiap.citizentech.inss.modelo;

/** O que o cidadão autoriza o assistente a consultar (princípio da finalidade e da necessidade). */
public enum Escopo {
    SITUACAO_PEDIDO("situação do pedido"),
    TEXTO_EXIGENCIA("texto da exigência"),
    PRAZO("prazo da exigência"),
    PROTOCOLO("número do protocolo");

    private final String descricao;

    Escopo(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
