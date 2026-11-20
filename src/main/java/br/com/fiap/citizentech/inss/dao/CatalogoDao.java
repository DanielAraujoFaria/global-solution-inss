package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.modelo.TipoExigencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Consultas ao catálogo de tipos de exigência. */
public class CatalogoDao extends DaoBase {
    public CatalogoDao(Connection con) {
        super(con);
    }

    public List<TipoExigencia> listarTipos() {
        return consultar("SELECT * FROM tipo_exigencia ORDER BY id", CatalogoDao::mapear);
    }

    public Optional<TipoExigencia> buscarPorCodigo(String codigo) {
        return consultar("SELECT * FROM tipo_exigencia WHERE codigo = ?", CatalogoDao::mapear, codigo)
                .stream().findFirst();
    }

    static TipoExigencia mapear(ResultSet rs) throws SQLException {
        return new TipoExigencia(rs.getLong("id"), rs.getString("codigo"), rs.getString("titulo"),
                rs.getString("texto_oficial_exemplo"), rs.getString("explicacao_simples"),
                rs.getInt("prazo_padrao_dias"), rs.getBoolean("exige_analise_humana"));
    }
}
