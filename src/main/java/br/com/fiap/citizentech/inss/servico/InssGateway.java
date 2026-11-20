package br.com.fiap.citizentech.inss.servico;

import br.com.fiap.citizentech.inss.modelo.PedidoComExigencia;

import java.util.List;

/**
 * Porta de integração com o sistema oficial do INSS. No protótipo há uma implementação fictícia;
 * em produção seria substituída por uma integração autorizada e homologada com o órgão.
 */
public interface InssGateway {
    /** A consulta a pedidos está disponível? Se não, o assistente não finge que consultou. */
    boolean integracaoDisponivel();

    /** Existe função oficial para anexar e protocolar a resposta? */
    boolean envioDisponivel();

    /** Pedidos (e exigências pendentes) que a sessão autenticada tem direito de ver. */
    List<PedidoComExigencia> consultarPedidos(AutenticacaoSimulada.Sessao sessao);

    /** Registra o envio e devolve o número de protocolo. Deve ser chamado dentro de uma transação. */
    String protocolarEnvio(AutenticacaoSimulada.Sessao sessao, long conversaId, long exigenciaId);
}
