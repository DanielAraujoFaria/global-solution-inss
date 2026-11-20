package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.modelo.Checklist;
import br.com.fiap.citizentech.inss.modelo.ItemChecklist;
import br.com.fiap.citizentech.inss.modelo.StatusItem;

import java.sql.Connection;
import java.time.LocalDateTime;

public class ChecklistDao extends DaoBase {
    public ChecklistDao(Connection con) {
        super(con);
    }

    /** Copia os documentos-modelo do tipo de exigência para a checklist do cidadão (INSERT ... SELECT). */
    public int gerarAPartirDoModelo(long exigenciaId, long tipoExigenciaId) {
        return atualizar("INSERT INTO item_checklist (exigencia_id, nome, orientacao, obrigatorio) "
                + "SELECT ?, nome, orientacao, obrigatorio FROM documento_modelo "
                + "WHERE tipo_exigencia_id = ? ORDER BY ordem", exigenciaId, tipoExigenciaId);
    }

    public Checklist listar(long exigenciaId) {
        return new Checklist(consultar("""
                SELECT i.id, i.exigencia_id, i.nome, i.orientacao, i.obrigatorio, i.status
                  FROM item_checklist i WHERE i.exigencia_id = ? ORDER BY i.id
                """,
                rs -> new ItemChecklist(rs.getLong("id"), rs.getLong("exigencia_id"), rs.getString("nome"),
                        rs.getString("orientacao"), rs.getBoolean("obrigatorio"),
                        StatusItem.valueOf(rs.getString("status"))),
                exigenciaId));
    }

    public void atualizarStatus(long itemId, StatusItem status, LocalDateTime agora) {
        atualizar("UPDATE item_checklist SET status = ?, atualizado_em = ? WHERE id = ?", status, agora, itemId);
    }

    public int marcarProntosComoEnviados(long exigenciaId, LocalDateTime agora) {
        return atualizar("UPDATE item_checklist SET status = 'ENVIADO', atualizado_em = ? "
                + "WHERE exigencia_id = ? AND status = 'PRONTO'", agora, exigenciaId);
    }
}
