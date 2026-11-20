package br.com.fiap.citizentech.inss.db;

import java.io.PrintStream;

/** Registro opcional das operações no banco, útil para mostrar o CRUD no vídeo técnico. */
public final class BdLog {
    private static PrintStream saida;

    private BdLog() {
    }

    public static void ativar(PrintStream destino) {
        saida = destino;
    }

    public static void desativar() {
        saida = null;
    }

    public static void registrar(String operacao, String tabela, String detalhe) {
        if (saida != null && !"mensagem".equals(tabela)) {
            saida.printf("      [BD] %-6s %-16s %s%n", operacao, tabela, detalhe);
        }
    }
}
