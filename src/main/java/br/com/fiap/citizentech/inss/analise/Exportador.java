package br.com.fiap.citizentech.inss.analise;

import br.com.fiap.citizentech.inss.dao.ConversaDao;
import br.com.fiap.citizentech.inss.modelo.ConversaRegistro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exporta os dados para CSV (separador ';', UTF-8 com BOM) para abrir no Excel/Power BI e montar gráficos.
 * Não exporta mensagens nem dados pessoais: só campos já anonimizados.
 */
public final class Exportador {
    private Exportador() {
    }

    public static List<Path> exportar(Connection con, Path pasta) throws IOException, SQLException {
        Files.createDirectories(pasta);
        List<Path> gerados = new ArrayList<>();

        StringBuilder conversas = new StringBuilder(
                "conversa_id;origem;rotulo;desfecho;encaminhamento;motivo_encaminhamento;"
                        + "resolvida_sem_atendimento_adicional;duracao_segundos;checklist_obrigatorios;checklist_prontos\n");
        for (ConversaRegistro c : new ConversaDao(con).listarTodas()) {
            conversas.append(c.id()).append(';').append(c.origem()).append(';')
                    .append(c.rotulo() == null ? "" : c.rotulo()).append(';').append(c.desfecho()).append(';')
                    .append(c.encaminhamento() == null ? "" : c.encaminhamento()).append(';')
                    .append(c.motivoEncaminhamento() == null ? "" : c.motivoEncaminhamento()).append(';')
                    .append(c.desfecho().resolvidaSemAtendimentoAdicional()).append(';')
                    .append(String.format(Locale.ROOT, "%.2f", c.duracaoSegundos())).append(';')
                    .append(c.checklistObrigatorios() == null ? "" : c.checklistObrigatorios()).append(';')
                    .append(c.checklistObrigatoriosProntos() == null ? "" : c.checklistObrigatoriosProntos()).append('\n');
        }
        gerados.add(gravar(pasta.resolve("conversas.csv"), conversas));

        StringBuilder avaliacoes = new StringBuilder("conversa_id;origem;clareza;satisfacao;sabe_proximo_passo\n");
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT a.conversa_id, c.origem, a.clareza, a.satisfacao, a.sabe_proximo_passo
                  FROM avaliacao a JOIN conversa c ON c.id = a.conversa_id ORDER BY a.id""");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                avaliacoes.append(rs.getLong(1)).append(';').append(rs.getString(2)).append(';')
                        .append(rs.getInt(3)).append(';').append(rs.getInt(4)).append(';')
                        .append(rs.getBoolean(5)).append('\n');
            }
        }
        gerados.add(gravar(pasta.resolve("avaliacoes.csv"), avaliacoes));
        return gerados;
    }

    private static Path gravar(Path arquivo, StringBuilder conteudo) throws IOException {
        Files.writeString(arquivo, "﻿" + conteudo, StandardCharsets.UTF_8);
        return arquivo;
    }
}
