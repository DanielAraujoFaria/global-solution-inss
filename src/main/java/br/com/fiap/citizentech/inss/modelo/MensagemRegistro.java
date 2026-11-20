package br.com.fiap.citizentech.inss.modelo;

import java.time.LocalDateTime;

public record MensagemRegistro(long id, long conversaId, Autor autor, String texto, boolean mascarada,
                               LocalDateTime enviadaEm) {
}
