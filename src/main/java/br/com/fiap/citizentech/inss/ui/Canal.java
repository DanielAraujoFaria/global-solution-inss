package br.com.fiap.citizentech.inss.ui;

/**
 * Canal de conversa entre o cidadão e o assistente. O serviço não sabe se está no terminal,
 * em um teste automatizado ou, no futuro, na API do WhatsApp: basta outra implementação.
 */
public interface Canal {
    /** Mensagem do assistente (balão no WhatsApp). */
    void assistente(String texto);

    /** Etapa que acontece FORA do chat, no navegador seguro (login gov.br simulado). */
    void navegador(String texto);

    /** Próxima entrada do cidadão, ou {@code null} se ele parou de responder. */
    String cidadao();
}
