package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.modelo.Avaliacao;
import br.com.fiap.citizentech.inss.modelo.Origem;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class AvaliacaoDao extends DaoBase {
    public AvaliacaoDao(Connection con) {
        super(con);
    }

    public long inserir(long conversaId, Avaliacao a, LocalDateTime agora) {
        return inserir("INSERT INTO avaliacao (conversa_id, clareza, satisfacao, sabe_proximo_passo, avaliada_em) "
                + "VALUES (?, ?, ?, ?, ?)", conversaId, a.clareza(), a.satisfacao(), a.sabeProximoPasso(), agora);
    }

    public List<Avaliacao> listarPorOrigem(Origem origem) {
        return consultar("""
                SELECT a.clareza, a.satisfacao, a.sabe_proximo_passo
                  FROM avaliacao a JOIN conversa c ON c.id = a.conversa_id
                 WHERE c.origem = ? ORDER BY a.id
                """,
                rs -> new Avaliacao(rs.getInt(1), rs.getInt(2), rs.getBoolean(3)), origem);
    }
}
