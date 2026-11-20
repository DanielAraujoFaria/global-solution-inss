package br.com.fiap.citizentech.inss.dao;

import br.com.fiap.citizentech.inss.modelo.ConversaRegistro;
import br.com.fiap.citizentech.inss.modelo.Desfecho;
import br.com.fiap.citizentech.inss.modelo.Encaminhamento;
import br.com.fiap.citizentech.inss.modelo.MotivoContato;
import br.com.fiap.citizentech.inss.modelo.MotivoEncaminhamento;
import br.com.fiap.citizentech.inss.modelo.Origem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ConversaDao extends DaoBase {
    public ConversaDao(Connection con) {
        super(con);
    }

    public long iniciar(Origem origem, String rotulo, LocalDateTime inicio) {
        return inserir("INSERT INTO conversa (origem, rotulo, inicio) VALUES (?, ?, ?)", origem, rotulo, inicio);
    }

    public void registrarMotivoContato(long id, MotivoContato motivo) {
        atualizar("UPDATE conversa SET motivo_contato = ? WHERE id = ?", motivo, id);
    }

    /** Guarda só uma referência (hash) da sessão de autenticação, nunca o token. */
    public void vincularAutenticacao(long id, long cidadaoId, String referenciaSegura) {
        atualizar("UPDATE conversa SET cidadao_id = ?, autenticada = TRUE, ref_autenticacao = ? WHERE id = ?",
                cidadaoId, referenciaSegura, id);
    }

    public void vincularExigencia(long id, long exigenciaId) {
        atualizar("UPDATE conversa SET exigencia_id = ? WHERE id = ?", exigenciaId, id);
    }

    /** Guarda um instantâneo da checklist desta conversa (para os indicadores). */
    public void registrarChecklist(long id, int obrigatorios, int obrigatoriosProntos) {
        atualizar("UPDATE conversa SET checklist_obrigatorios = ?, checklist_obrigatorios_prontos = ? WHERE id = ?",
                obrigatorios, obrigatoriosProntos, id);
    }

    public void finalizar(long id, Desfecho desfecho, MotivoEncaminhamento motivo, String resumo, LocalDateTime fim) {
        Encaminhamento destino = motivo == null ? null : motivo.destino();
        atualizar("UPDATE conversa SET desfecho = ?, encaminhamento = ?, motivo_encaminhamento = ?, "
                + "resumo_encaminhamento = ?, fim = ? WHERE id = ?", desfecho, destino, motivo, resumo, fim, id);
    }

    public Optional<ConversaRegistro> buscar(long id) {
        return consultar("SELECT * FROM conversa WHERE id = ?", ConversaDao::mapear, id).stream().findFirst();
    }

    public List<ConversaRegistro> listar(Origem origem) {
        return consultar("SELECT * FROM conversa WHERE origem = ? ORDER BY id", ConversaDao::mapear, origem);
    }

    public List<ConversaRegistro> listarTodas() {
        return consultar("SELECT * FROM conversa ORDER BY id", ConversaDao::mapear);
    }

    private static Integer inteiroOuNulo(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor;
    }

    private static ConversaRegistro mapear(ResultSet rs) throws SQLException {
        String enc = rs.getString("encaminhamento");
        String mot = rs.getString("motivo_encaminhamento");
        return new ConversaRegistro(rs.getLong("id"), Origem.valueOf(rs.getString("origem")), rs.getString("rotulo"),
                longOuNulo(rs, "cidadao_id"), longOuNulo(rs, "exigencia_id"),
                inteiroOuNulo(rs, "checklist_obrigatorios"), inteiroOuNulo(rs, "checklist_obrigatorios_prontos"),
                rs.getBoolean("autenticada"),
                dataHora(rs, "inicio"), dataHora(rs, "fim"), Desfecho.valueOf(rs.getString("desfecho")),
                enc == null ? null : Encaminhamento.valueOf(enc),
                mot == null ? null : MotivoEncaminhamento.valueOf(mot),
                rs.getString("resumo_encaminhamento"));
    }
}
