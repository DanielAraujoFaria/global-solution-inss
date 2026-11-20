package br.com.fiap.citizentech.inss.servico;

import br.com.fiap.citizentech.inss.modelo.Exigencia;

import java.time.LocalDate;

/**
 * Transforma uma exigência em explicação simples. É uma interface para que a implementação possa
 * mudar (hoje, catálogo revisado; no futuro, um modelo de linguagem com revisão humana)
 * sem alterar o restante do sistema.
 */
public interface ExplicadorExigencia {
    String explicar(Exigencia exigencia, LocalDate hoje);
}
