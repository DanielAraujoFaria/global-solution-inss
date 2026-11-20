package br.com.fiap.citizentech.inss.ui;

import java.io.PrintStream;

/** Base dos canais de texto: define como as mensagens aparecem; as subclasses definem de onde vem a entrada. */
public abstract class CanalTexto implements Canal {
    protected final PrintStream saida;

    protected CanalTexto(PrintStream saida) {
        this.saida = saida;
    }

    protected abstract String lerLinha();

    protected boolean ecoDaEntrada() {
        return false;
    }

    @Override
    public void assistente(String texto) {
        imprimir("Assistente INSS", texto);
    }

    @Override
    public void navegador(String texto) {
        imprimir("Navegador seguro gov.br - SIMULADO", texto);
    }

    @Override
    public String cidadao() {
        if (saida != null && !ecoDaEntrada()) {
            saida.print("[Você] > ");
            saida.flush();
        }
        String linha = lerLinha();
        if (linha != null && saida != null && ecoDaEntrada()) {
            saida.println("[Você] > " + linha);
            saida.println();
        }
        return linha;
    }

    protected void imprimir(String quem, String texto) {
        if (saida == null) {
            return;
        }
        saida.println("[" + quem + "]");
        for (String linha : texto.split("\n", -1)) {
            saida.println("  " + linha);
        }
        saida.println();
    }
}
