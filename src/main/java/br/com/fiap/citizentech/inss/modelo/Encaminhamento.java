package br.com.fiap.citizentech.inss.modelo;

/** Canais oficiais para os quais o assistente encaminha. */
public enum Encaminhamento {
    MEU_INSS("Meu INSS (site ou aplicativo)", false),
    CENTRAL_135("Central de atendimento 135", true),
    AGENCIA_APS("Agência da Previdência Social (APS), com agendamento pelo 135 ou pelo Meu INSS", true);

    private final String descricao;
    private final boolean atendimentoHumano;

    Encaminhamento(String descricao, boolean atendimentoHumano) {
        this.descricao = descricao;
        this.atendimentoHumano = atendimentoHumano;
    }

    public String descricao() {
        return descricao;
    }

    /** Verdadeiro quando o destino é uma pessoa (135 ou agência). */
    public boolean atendimentoHumano() {
        return atendimentoHumano;
    }
}
