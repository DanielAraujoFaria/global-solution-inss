package br.com.fiap.citizentech.inss.servico;

import br.com.fiap.citizentech.inss.modelo.Exigencia;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Pedido;
import br.com.fiap.citizentech.inss.modelo.StatusPedido;
import br.com.fiap.citizentech.inss.modelo.TipoAcesso;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Regras de quando a automação deve parar e passar para uma pessoa. É uma função pura
 * (mesmas entradas, mesma saída), por isso é fácil de testar e de auditar.
 * A ordem das verificações é a ordem de prioridade.
 */
public final class AvaliadorDeCaso {
    private AvaliadorDeCaso() {
    }

    /** Verificação feita logo após o login, antes de consultar qualquer dado. */
    public static Optional<MotivoEncaminhamento> bloqueioPorAcesso(TipoAcesso acesso) {
        return acesso.podeUsarAutomacao() ? Optional.empty() : Optional.of(MotivoEncaminhamento.REPRESENTANTE_NAO_HABILITADO);
    }

    public static Optional<MotivoEncaminhamento> avaliar(TipoAcesso acesso, Pedido pedido, Exigencia exigencia,
                                                          LocalDate hoje) {
        Optional<MotivoEncaminhamento> bloqueio = bloqueioPorAcesso(acesso);
        if (bloqueio.isPresent()) {
            return bloqueio;
        }
        if (pedido == null) {
            return Optional.of(MotivoEncaminhamento.NENHUMA_EXIGENCIA_ENCONTRADA);
        }
        if (pedido.getStatus() == StatusPedido.INDEFERIDO) {
            return Optional.of(MotivoEncaminhamento.PEDIDO_INDEFERIDO);
        }
        if (pedido.temDadosDivergentes()) {
            return Optional.of(MotivoEncaminhamento.DADOS_DIVERGENTES);
        }
        if (exigencia == null) {
            return Optional.of(MotivoEncaminhamento.NENHUMA_EXIGENCIA_ENCONTRADA);
        }
        if (exigencia.getTipo().exigeAnaliseHumana()) {
            return Optional.of(MotivoEncaminhamento.CASO_COMPLEXO);
        }
        if (exigencia.estaVencida(hoje)) {
            return Optional.of(MotivoEncaminhamento.PRAZO_VENCIDO);
        }
        return Optional.empty();
    }
}
