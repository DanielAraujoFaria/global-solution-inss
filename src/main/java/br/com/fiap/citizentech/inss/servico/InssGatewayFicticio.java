package br.com.fiap.citizentech.inss.servico;

import br.com.fiap.citizentech.inss.dao.CidadaoDao;
import br.com.fiap.citizentech.inss.dao.EnvioDao;
import br.com.fiap.citizentech.inss.dao.ExigenciaDao;
import br.com.fiap.citizentech.inss.dao.PedidoDao;
import br.com.fiap.citizentech.inss.modelo.AcessoNegadoException;
import br.com.fiap.citizentech.inss.modelo.Cidadao;
import br.com.fiap.citizentech.inss.modelo.Escopo;
import br.com.fiap.citizentech.inss.modelo.Exigencia;
import br.com.fiap.citizentech.inss.modelo.Pedido;
import br.com.fiap.citizentech.inss.modelo.PedidoComExigencia;
import br.com.fiap.citizentech.inss.modelo.TipoAcesso;

import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * "INSS de mentira" que lê os dados fictícios do banco. Aplica as mesmas travas que uma
 * integração real aplicaria: sessão válida, escopo autorizado e titularidade do pedido.
 */
public class InssGatewayFicticio implements InssGateway {
    private final CidadaoDao cidadaoDao;
    private final PedidoDao pedidoDao;
    private final ExigenciaDao exigenciaDao;
    private final EnvioDao envioDao;
    private final AutenticacaoSimulada autenticacao;
    private final Clock clock;
    private final boolean integracaoDisponivel;
    private final boolean envioDisponivel;

    public InssGatewayFicticio(Connection con, AutenticacaoSimulada autenticacao, Clock clock,
                               boolean integracaoDisponivel, boolean envioDisponivel) {
        this.cidadaoDao = new CidadaoDao(con);
        this.pedidoDao = new PedidoDao(con);
        this.exigenciaDao = new ExigenciaDao(con);
        this.envioDao = new EnvioDao(con);
        this.autenticacao = autenticacao;
        this.clock = clock;
        this.integracaoDisponivel = integracaoDisponivel;
        this.envioDisponivel = envioDisponivel;
    }

    @Override
    public boolean integracaoDisponivel() {
        return integracaoDisponivel;
    }

    @Override
    public boolean envioDisponivel() {
        return envioDisponivel;
    }

    @Override
    public List<PedidoComExigencia> consultarPedidos(AutenticacaoSimulada.Sessao sessao) {
        autenticacao.validar(sessao, Escopo.SITUACAO_PEDIDO);
        autenticacao.validar(sessao, Escopo.TEXTO_EXIGENCIA);
        autenticacao.validar(sessao, Escopo.PRAZO);
        long titularId = titularDoAcesso(sessao);
        return pedidoDao.listarPorCidadao(titularId).stream()
                .map(p -> new PedidoComExigencia(p, exigenciaDao.buscarPendenteDoPedido(p.getId()).orElse(null)))
                .toList();
    }

    @Override
    public String protocolarEnvio(AutenticacaoSimulada.Sessao sessao, long conversaId, long exigenciaId) {
        autenticacao.validar(sessao, Escopo.PROTOCOLO);
        long titularId = titularDoAcesso(sessao);
        Exigencia exigencia = exigenciaDao.buscarPorId(exigenciaId)
                .orElseThrow(() -> new AcessoNegadoException("Exigência inexistente"));
        Pedido pedido = pedidoDao.buscarPorId(exigencia.getPedidoId())
                .orElseThrow(() -> new AcessoNegadoException("Pedido inexistente"));
        if (pedido.getCidadaoId() != titularId) {
            throw new AcessoNegadoException("Exigência não pertence a este acesso");
        }
        String protocolo = "SIM-" + LocalDateTime.now(clock).getYear() + "-"
                + String.format("%06d", envioDao.contar() + 1);
        envioDao.inserir(exigenciaId, conversaId, protocolo, LocalDateTime.now(clock));
        return protocolo;
    }

    /** Titular cujos pedidos esta sessão pode ver: o próprio, ou o representado se a representação estiver habilitada. */
    private long titularDoAcesso(AutenticacaoSimulada.Sessao sessao) {
        Cidadao quem = cidadaoDao.buscarPorId(sessao.cidadaoId())
                .orElseThrow(() -> new AcessoNegadoException("Cidadão desconhecido"));
        if (!quem.getTipoAcesso().podeUsarAutomacao()) {
            throw new AcessoNegadoException("Representação não habilitada");
        }
        if (quem.getTipoAcesso() == TipoAcesso.REPRESENTANTE_HABILITADO) {
            if (quem.getRepresentaId() == null) {
                throw new AcessoNegadoException("Representação sem titular vinculado");
            }
            return quem.getRepresentaId();
        }
        return quem.getId();
    }
}
