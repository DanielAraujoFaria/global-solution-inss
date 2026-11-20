package br.com.fiap.citizentech.inss.modelo;

import java.util.Optional;

/** Pedido do cidadão junto com a exigência pendente (se houver). */
public record PedidoComExigencia(Pedido pedido, Exigencia exigenciaPendente) {
    public Optional<Exigencia> exigencia() {
        return Optional.ofNullable(exigenciaPendente);
    }
}
