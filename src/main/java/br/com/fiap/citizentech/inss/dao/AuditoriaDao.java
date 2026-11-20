package br.com.fiap.citizentech.inss.dao;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Trilha de auditoria: consentimentos, autenticação, consultas e confirmações. */
public class AuditoriaDao extends DaoBase {
    public AuditoriaDao(Connection con) {
        super(con);
    }

    public long registrar(long conversaId, String evento, String detalhe, LocalDateTime agora) {
        return inserir("INSERT INTO auditoria (conversa_id, evento, detalhe, ocorrido_em) VALUES (?, ?, ?, ?)",
                conversaId, evento, detalhe, agora);
    }

    public List<String> listarEventos(long conversaId) {
        return consultar("SELECT evento FROM auditoria WHERE conversa_id = ? ORDER BY id",
                rs -> rs.getString(1), conversaId);
    }

    public long contar(String evento) {
        return consultar("SELECT COUNT(*) FROM auditoria WHERE evento = ?", rs -> rs.getLong(1), evento).get(0);
    }

    public Map<String, Long> contarPorEvento() {
        Map<String, Long> mapa = new TreeMap<>();
        consultar("SELECT evento, COUNT(*) FROM auditoria GROUP BY evento",
                rs -> mapa.put(rs.getString(1), rs.getLong(2)));
        return mapa;
    }
}
