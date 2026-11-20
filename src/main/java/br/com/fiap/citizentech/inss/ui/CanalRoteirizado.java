package br.com.fiap.citizentech.inss.ui;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Canal com respostas pré-definidas (fila de entradas). Serve para testes automatizados,
 * simulações e para o modo --demo do vídeo. Guarda a transcrição de tudo que foi dito.
 */
public class CanalRoteirizado extends CanalTexto {
    private final Deque<String> entradas = new ArrayDeque<>();
    private final List<String> transcricao = new ArrayList<>();
    private final List<String> falasDoAssistente = new ArrayList<>();

    /** @param saida onde mostrar a conversa (null = silencioso) */
    public CanalRoteirizado(PrintStream saida, String... respostas) {
        super(saida);
        entradas.addAll(List.of(respostas));
    }

    @Override
    protected String lerLinha() {
        String proxima = entradas.pollFirst();
        if (proxima != null) {
            transcricao.add("CIDADAO: " + proxima);
        }
        return proxima;
    }

    @Override
    protected boolean ecoDaEntrada() {
        return true;
    }

    @Override
    public void assistente(String texto) {
        falasDoAssistente.add(texto);
        transcricao.add("ASSISTENTE: " + texto);
        super.assistente(texto);
    }

    @Override
    public void navegador(String texto) {
        transcricao.add("NAVEGADOR: " + texto);
        super.navegador(texto);
    }

    /** Só o que o assistente disse no chat (sem a tela simulada do gov.br). Usado nas verificações de conteúdo. */
    public String falasDoAssistente() {
        return String.join("\n", falasDoAssistente);
    }

    public String transcricao() {
        return String.join("\n", transcricao);
    }

    /** Respostas que o roteiro não usou (indica roteiro mais longo que a conversa). */
    public int entradasRestantes() {
        return entradas.size();
    }
}
