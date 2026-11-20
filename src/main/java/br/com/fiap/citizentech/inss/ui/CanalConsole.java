package br.com.fiap.citizentech.inss.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;

/** Conversa interativa no terminal (simula o WhatsApp). */
public class CanalConsole extends CanalTexto {
    private final BufferedReader entrada;

    public CanalConsole(BufferedReader entrada, PrintStream saida) {
        super(saida);
        this.entrada = entrada;
    }

    @Override
    protected String lerLinha() {
        try {
            return entrada.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
