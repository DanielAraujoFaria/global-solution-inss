package br.com.fiap.citizentech.inss.db;

import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Cria a estrutura (DDL) e carrega os dados fictícios a partir dos scripts em /db. */
public final class BancoDeDados {
    private BancoDeDados() {
    }

    /** Cria as tabelas e carrega o catálogo e os perfis fictícios, se o banco ainda estiver vazio. */
    public static void prepararSeNecessario(Connection con) {
        if (!estruturaExiste(con)) {
            executarScript(con, "/db/schema.sql");
            executarScript(con, "/db/seed.sql");
        }
    }

    /** Apaga tudo e recria (usado na simulação e nos testes). */
    public static void reiniciar(Connection con) {
        try (Statement st = con.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha ao limpar o banco", e);
        }
        executarScript(con, "/db/schema.sql");
        executarScript(con, "/db/seed.sql");
    }

    /**
     * Volta as exigências e as checklists ao estado inicial SEM apagar conversas, avaliações e auditoria.
     * Serve para que cada participante de um teste comece da mesma situação.
     */
    public static void restaurarPedidos(Connection con) {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM item_checklist");
            st.executeUpdate("UPDATE exigencia SET status = 'ABERTA', data_cumprimento = NULL, "
                    + "lembrete_consentido = FALSE, lembrete_dias_antes = NULL");
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha ao restaurar os pedidos", e);
        }
    }

    public static boolean estruturaExiste(Connection con) {
        try (ResultSet rs = con.getMetaData().getTables(null, null, "CIDADAO", new String[] {"TABLE"})) {
            return rs.next();
        } catch (SQLException e) {
            throw new ErroDeBancoException("Falha ao verificar o banco", e);
        }
    }

    static void executarScript(Connection con, String recurso) {
        String conteudo;
        try (InputStream in = BancoDeDados.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new ErroDeBancoException("Script não encontrado: " + recurso, null);
            }
            conteudo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ErroDeBancoException("Falha ao ler " + recurso, e);
        }
        for (String comando : dividirComandos(conteudo)) {
            try (Statement st = con.createStatement()) {
                st.execute(comando);
            } catch (SQLException e) {
                throw new ErroDeBancoException("Erro em " + recurso + ": " + comando, e);
            }
        }
    }

    /** Separa os comandos por ';' ignorando comentários (--) e textos entre aspas simples. */
    public static List<String> dividirComandos(String script) {
        List<String> comandos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean emTexto = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (emTexto) {
                atual.append(c);
                if (c == '\'') {
                    if (i + 1 < script.length() && script.charAt(i + 1) == '\'') {
                        atual.append('\'');
                        i++;
                    } else {
                        emTexto = false;
                    }
                }
            } else if (c == '\'') {
                emTexto = true;
                atual.append(c);
            } else if (c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;
                }
                atual.append('\n');
            } else if (c == ';') {
                adicionarSeNaoVazio(comandos, atual);
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        adicionarSeNaoVazio(comandos, atual);
        return comandos;
    }

    private static void adicionarSeNaoVazio(List<String> comandos, StringBuilder sb) {
        String comando = sb.toString().trim();
        if (!comando.isEmpty()) {
            comandos.add(comando);
        }
    }
}
