package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.modelo.Pedido;
import br.com.fiap.citizentech.inss.modelo.StatusPedido;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PedidoDao extends DaoBase {
    public PedidoDao(Connection con) {
        super(con);
    }

    public List<Pedido> listarPorCidadao(long cidadaoId) {
        return consultar("SELECT * FROM pedido WHERE cidadao_id = ? ORDER BY data_pedido DESC, id",
                PedidoDao::mapear, cidadaoId);
    }

    public Optional<Pedido> buscarPorId(long id) {
        return consultar("SELECT * FROM pedido WHERE id = ?", PedidoDao::mapear, id).stream().findFirst();
    }

    private static Pedido mapear(ResultSet rs) throws SQLException {
        return new Pedido(rs.getLong("id"), rs.getLong("cidadao_id"), rs.getString("protocolo"),
                rs.getString("beneficio"), StatusPedido.valueOf(rs.getString("status")),
                rs.getBoolean("dados_divergentes"), data(rs, "data_pedido"));
    }
}
