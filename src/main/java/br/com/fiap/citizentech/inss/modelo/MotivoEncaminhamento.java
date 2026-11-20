package br.com.fiap.citizentech.inss.modelo;

/**
 * Por que o assistente parou a automação. Cada motivo tem um destino fixo,
 * o que torna a regra auditável e testável.
 */
public enum MotivoEncaminhamento {
    SOLICITADO_PELO_CIDADAO(Encaminhamento.CENTRAL_135,
            "Você pediu para falar com uma pessoa."),
    CONSENTIMENTO_NEGADO(Encaminhamento.MEU_INSS,
            "Sem a sua autorização eu não consulto o seu pedido. Você mesmo pode consultar quando quiser."),
    AUTENTICACAO_NAO_CONCLUIDA(Encaminhamento.MEU_INSS,
            "O login não foi concluído, então não consegui consultar o seu pedido."),
    INTEGRACAO_INDISPONIVEL(Encaminhamento.MEU_INSS,
            "No momento não consigo consultar pedidos por aqui, e não vou fingir que consultei."),
    REPRESENTANTE_NAO_HABILITADO(Encaminhamento.CENTRAL_135,
            "Seu acesso como representante ainda não está habilitado para este atendimento. "
                    + "Por segurança, não mostro dados do pedido de outra pessoa."),
    PEDIDO_INDEFERIDO(Encaminhamento.MEU_INSS,
            "O pedido consta como indeferido. A decisão fica registrada no Meu INSS, e o recurso "
                    + "é feito pelos canais oficiais (Meu INSS, 135 ou agência). Eu não decido nem protocolo recurso."),
    DADOS_DIVERGENTES(Encaminhamento.AGENCIA_APS,
            "Há divergência nos dados do pedido. Isso precisa ser conferido por uma pessoa do INSS."),
    CASO_COMPLEXO(Encaminhamento.AGENCIA_APS,
            "Esse tipo de exigência precisa de avaliação individual por uma pessoa do INSS."),
    PRAZO_VENCIDO(Encaminhamento.AGENCIA_APS,
            "O prazo registrado já passou. Procure o INSS para saber se ainda há como regularizar."),
    DOCUMENTO_SEM_SOLUCAO(Encaminhamento.CENTRAL_135,
            "Há documento que você não sabe se serve ou que está ilegível. O INSS pode indicar as alternativas aceitas."),
    NENHUMA_EXIGENCIA_ENCONTRADA(Encaminhamento.CENTRAL_135,
            "Não encontrei exigência pendente neste pedido. Se você recebeu uma notificação, confirme com o INSS.");

    private final Encaminhamento destino;
    private final String explicacao;

    MotivoEncaminhamento(Encaminhamento destino, String explicacao) {
        this.destino = destino;
        this.explicacao = explicacao;
    }

    public Encaminhamento destino() {
        return destino;
    }

    public String explicacao() {
        return explicacao;
    }
}
