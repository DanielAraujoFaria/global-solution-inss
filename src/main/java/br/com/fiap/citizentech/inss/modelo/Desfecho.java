package br.com.fiap.citizentech.inss.modelo;

/** Como a conversa terminou. */
public enum Desfecho {
    EM_ANDAMENTO(false),
    /** Checklist completa, cidadão confirmou e o envio gerou protocolo (simulado). */
    CONCLUIDA_DIGITAL(true),
    /** Checklist completa; o cidadão conclui a etapa no Meu INSS. */
    ORIENTADA_MEU_INSS(true),
    /** Faltam documentos; o cidadão sai com a lista e, se quiser, um lembrete. */
    PENDENTE_DOCUMENTOS(false),
    /** Encaminhado para outro canal (ver {@link Encaminhamento}). */
    ENCAMINHADA(false),
    ABANDONADA(false);

    private final boolean resolvidaSemAtendimentoAdicional;

    Desfecho(boolean resolvidaSemAtendimentoAdicional) {
        this.resolvidaSemAtendimentoAdicional = resolvidaSemAtendimentoAdicional;
    }

    public boolean resolvidaSemAtendimentoAdicional() {
        return resolvidaSemAtendimentoAdicional;
    }
}
