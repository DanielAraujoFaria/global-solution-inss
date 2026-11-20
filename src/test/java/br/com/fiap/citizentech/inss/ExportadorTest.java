package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.analise.Exportador;
import br.com.fiap.citizentech.inss.servico.AssistenteFactory;
import br.com.fiap.citizentech.inss.modelo.Origem;
import br.com.fiap.citizentech.inss.ui.CanalRoteirizado;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExportadorTest extends TesteBase {
    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    @Test
    public void exportaCsvSemMensagensNemDadosPessoais() throws Exception {
        AssistenteFactory.criar(con).atender(
                new CanalRoteirizado(null, "minha senha é 123456", "1", "1", "0", "1", "1", "1", "1", "2", "1", "5", "4", "1"),
                Origem.TESTE_USUARIO, "P01");

        List<Path> arquivos = Exportador.exportar(con, pasta.getRoot().toPath());
        assertEquals(2, arquivos.size());

        String conversas = Files.readString(arquivos.get(0), StandardCharsets.UTF_8);
        String avaliacoes = Files.readString(arquivos.get(1), StandardCharsets.UTF_8);
        assertTrue(conversas.startsWith("﻿conversa_id;origem;rotulo;desfecho;"));
        assertEquals("cabeçalho + 1 conversa", 2, conversas.strip().split("\n").length);
        assertTrue(conversas.contains("TESTE_USUARIO;P01;CONCLUIDA_DIGITAL"));
        assertTrue(avaliacoes.contains(";TESTE_USUARIO;5;4;true"));
        for (String conteudo : new String[] {conversas, avaliacoes}) {
            assertFalse(conteudo.contains("123456"));
            assertFalse(conteudo.toLowerCase().contains("senha"));
            assertFalse(conteudo.contains("Maria"));
        }
    }
}
