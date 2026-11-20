package br.com.fiap.citizentech.inss.modelo;

public enum StatusItem {
    PENDENTE("não tenho"),
    PRONTO("tenho e está legível"),
    DUVIDA("não sei se serve"),
    ILEGIVEL("tenho, mas está ilegível ou incompleto"),
    ENVIADO("enviado");

    private final String descricao;

    StatusItem(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    /** O documento já está em condições de ser enviado (ou foi enviado). */
    public boolean pronto() {
        return this == PRONTO || this == ENVIADO;
    }
}
