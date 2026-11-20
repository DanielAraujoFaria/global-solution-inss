package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.modelo.Autor;
import br.com.fiap.citizentech.inss.modelo.MensagemRegistro;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class MensagemDao extends DaoBase {
    public MensagemDao(Connection con) {
        super(con);
    }

    public long inserir(long conversaId, Autor autor, String texto, boolean mascarada, LocalDateTime agora) {
        return inserir("INSERT INTO mensagem (conversa_id, autor, texto, mascarada, enviada_em) VALUES (?, ?, ?, ?, ?)",
                conversaId, autor, texto, mascarada, agora);
    }

    public List<MensagemRegistro> listarPorConversa(long conversaId) {
        return consultar("SELECT * FROM mensagem WHERE conversa_id = ? ORDER BY id", MensagemDao::mapear, conversaId);
    }

    public List<MensagemRegistro> listarTodas() {
        return consultar("SELECT * FROM mensagem ORDER BY id", MensagemDao::mapear);
    }

    /** Política de retenção: remove mensagens anteriores ao limite. Retorna quantas foram apagadas. */
    public int purgarAnterioresA(LocalDateTime limite) {
        return atualizar("DELETE FROM mensagem WHERE enviada_em < ?", limite);
    }

    private static MensagemRegistro mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new MensagemRegistro(rs.getLong("id"), rs.getLong("conversa_id"), Autor.valueOf(rs.getString("autor")),
                rs.getString("texto"), rs.getBoolean("mascarada"), dataHora(rs, "enviada_em"));
    }
}
