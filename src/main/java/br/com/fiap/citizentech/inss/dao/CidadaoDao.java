package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.modelo.Cidadao;
import br.com.fiap.citizentech.inss.modelo.TipoAcesso;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CidadaoDao extends DaoBase {
    public CidadaoDao(Connection con) {
        super(con);
    }

    public Optional<Cidadao> buscarPorId(long id) {
        return consultar("SELECT * FROM cidadao WHERE id = ?", CidadaoDao::mapear, id).stream().findFirst();
    }

    public List<Cidadao> listar() {
        return consultar("SELECT * FROM cidadao ORDER BY id", CidadaoDao::mapear);
    }

    public long inserir(Cidadao c) {
        return inserir("INSERT INTO cidadao (nome_ficticio, faixa_etaria, tipo_acesso, representa_id) VALUES (?, ?, ?, ?)",
                c.getNomeFicticio(), c.getFaixaEtaria(), c.getTipoAcesso(), c.getRepresentaId());
    }

    private static Cidadao mapear(ResultSet rs) throws SQLException {
        return new Cidadao(rs.getLong("id"), rs.getString("nome_ficticio"), rs.getString("faixa_etaria"),
                TipoAcesso.valueOf(rs.getString("tipo_acesso")), longOuNulo(rs, "representa_id"));
    }
}
