package br.com.fiap.citizentech.inss.modelo;

import java.time.Duration;
import java.time.LocalDateTime;

/** Linha da tabela conversa, usada em consultas e nos indicadores. */
public record ConversaRegistro(long id, Origem origem, String rotulo, Long cidadaoId, Long exigenciaId,
                               Integer checklistObrigatorios, Integer checklistObrigatoriosProntos,
                               boolean autenticada, LocalDateTime inicio, LocalDateTime fim, Desfecho desfecho,
                               Encaminhamento encaminhamento, MotivoEncaminhamento motivoEncaminhamento,
                               String resumoEncaminhamento) {

    /** Esta conversa chegou a montar uma checklist? */
    public boolean teveChecklist() {
        return checklistObrigatorios != null;
    }

    /** Todos os documentos obrigatórios estavam prontos ao fim da checklist? */
    public boolean checklistCompleta() {
        return teveChecklist() && checklistObrigatorios.equals(checklistObrigatoriosProntos);
    }

    public boolean finalizada() {
        return desfecho != Desfecho.EM_ANDAMENTO;
    }

    /** Duração da conversa em segundos (0 quando ainda não terminou). */
    public double duracaoSegundos() {
        return fim == null ? 0 : Duration.between(inicio, fim).toMillis() / 1000.0;
    }
}
