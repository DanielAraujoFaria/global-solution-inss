package br.com.fiap.citizentech.inss.analise;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Mostra o conteúdo gravado no banco (usado no vídeo para provar a persistência). */
public final class ResumoDoBanco {
    private static final int LARGURA_MAXIMA = 34;

    private ResumoDoBanco() {
    }

    public static void imprimir(Connection con, PrintStream out) throws SQLException {
        tabela(con, out, "Conversas (mais recentes)",
                "SELECT id, origem, desfecho, encaminhamento, motivo_encaminhamento AS motivo, autenticada "
                        + "FROM conversa ORDER BY id DESC LIMIT 5");
        tabela(con, out, "Exigências",
                "SELECT e.id, p.protocolo, t.codigo AS tipo, e.status, e.data_prazo, e.data_cumprimento "
                        + "FROM exigencia e JOIN pedido p ON p.id = e.pedido_id "
                        + "JOIN tipo_exigencia t ON t.id = e.tipo_exigencia_id ORDER BY e.id");
        tabela(con, out, "Checklist (itens já respondidos)",
                "SELECT i.id, i.exigencia_id AS exig, i.nome, i.obrigatorio AS obrig, i.status "
                        + "FROM item_checklist i WHERE i.atualizado_em IS NOT NULL ORDER BY i.id LIMIT 12");
        tabela(con, out, "Envios (protocolos simulados)",
                "SELECT id, exigencia_id AS exig, conversa_id AS conversa, protocolo_simulado AS protocolo, enviado_em "
                        + "FROM envio_protocolo ORDER BY id DESC LIMIT 5");
        tabela(con, out, "Auditoria da última conversa",
                "SELECT evento, detalhe FROM auditoria WHERE conversa_id = (SELECT MAX(id) FROM conversa) ORDER BY id");
    }

    private static void tabela(Connection con, PrintStream out, String titulo, String sql) throws SQLException {
        out.println("--- " + titulo + " ---");
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int colunas = md.getColumnCount();
            List<String[]> linhas = new ArrayList<>();
            int[] largura = new int[colunas];
            String[] cabecalho = new String[colunas];
            for (int i = 0; i < colunas; i++) {
                cabecalho[i] = md.getColumnLabel(i + 1).toLowerCase();
                largura[i] = cabecalho[i].length();
            }
            while (rs.next()) {
                String[] linha = new String[colunas];
                for (int i = 0; i < colunas; i++) {
                    Object v = rs.getObject(i + 1);
                    String texto = v == null ? "-" : String.valueOf(v);
                    linha[i] = texto.length() > LARGURA_MAXIMA ? texto.substring(0, LARGURA_MAXIMA - 1) + "…" : texto;
                    largura[i] = Math.max(largura[i], linha[i].length());
                }
                linhas.add(linha);
            }
            imprimirLinha(out, cabecalho, largura);
            for (String[] linha : linhas) {
                imprimirLinha(out, linha, largura);
            }
            if (linhas.isEmpty()) {
                out.println("(sem registros)");
            }
        }
        out.println();
    }

    private static void imprimirLinha(PrintStream out, String[] celulas, int[] largura) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < celulas.length; i++) {
            sb.append(String.format("%-" + largura[i] + "s", celulas[i])).append("  ");
        }
        out.println(sb.toString().stripTrailing());
    }
}
