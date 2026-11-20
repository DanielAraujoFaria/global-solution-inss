package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.modelo.Exigencia;
import br.com.fiap.citizentech.inss.modelo.StatusExigencia;
import br.com.fiap.citizentech.inss.modelo.TipoExigencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class ExigenciaDao extends DaoBase {
    private static final String SELECT = """
            SELECT e.id, e.pedido_id, e.texto_oficial, e.data_emissao, e.data_prazo, e.status, e.data_cumprimento,
                   t.id AS t_id, t.codigo, t.titulo, t.texto_oficial_exemplo, t.explicacao_simples,
                   t.prazo_padrao_dias, t.exige_analise_humana
              FROM exigencia e JOIN tipo_exigencia t ON t.id = e.tipo_exigencia_id
            """;

    public ExigenciaDao(Connection con) {
        super(con);
    }

    /** Exigência ainda não cumprida do pedido (a mais recente), se houver. */
    public Optional<Exigencia> buscarPendenteDoPedido(long pedidoId) {
        return consultar(SELECT + " WHERE e.pedido_id = ? AND e.status <> 'CUMPRIDA' ORDER BY e.data_emissao DESC, e.id DESC",
                ExigenciaDao::mapear, pedidoId).stream().findFirst();
    }

    public Optional<Exigencia> buscarPorId(long id) {
        return consultar(SELECT + " WHERE e.id = ?", ExigenciaDao::mapear, id).stream().findFirst();
    }

    public long inserir(long pedidoId, long tipoId, String textoOficial, LocalDate emissao, LocalDate prazo) {
        return inserir("INSERT INTO exigencia (pedido_id, tipo_exigencia_id, texto_oficial, data_emissao, data_prazo) "
                + "VALUES (?, ?, ?, ?, ?)", pedidoId, tipoId, textoOficial, emissao, prazo);
    }

    public void atualizarStatus(long id, StatusExigencia status, LocalDate dataCumprimento) {
        atualizar("UPDATE exigencia SET status = ?, data_cumprimento = ? WHERE id = ?", status, dataCumprimento, id);
    }

    public void registrarLembrete(long id, int diasAntes) {
        atualizar("UPDATE exigencia SET lembrete_consentido = TRUE, lembrete_dias_antes = ? WHERE id = ?", diasAntes, id);
    }

    public Optional<Integer> buscarLembreteDiasAntes(long id) {
        return consultar("SELECT lembrete_dias_antes FROM exigencia WHERE id = ? AND lembrete_consentido = TRUE",
                rs -> rs.getInt(1), id).stream().findFirst();
    }

    private static Exigencia mapear(ResultSet rs) throws SQLException {
        TipoExigencia tipo = new TipoExigencia(rs.getLong("t_id"), rs.getString("codigo"), rs.getString("titulo"),
                rs.getString("texto_oficial_exemplo"), rs.getString("explicacao_simples"),
                rs.getInt("prazo_padrao_dias"), rs.getBoolean("exige_analise_humana"));
        return new Exigencia(rs.getLong("id"), rs.getLong("pedido_id"), tipo, rs.getString("texto_oficial"),
                data(rs, "data_emissao"), data(rs, "data_prazo"),
                StatusExigencia.valueOf(rs.getString("status")), data(rs, "data_cumprimento"));
    }
}
