package br.com.fiap.citizentech.inss.db;

import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Executa várias operações como uma unidade: ou todas são gravadas (commit) ou nenhuma (rollback).
 * Usado no envio do protocolo, que grava em três tabelas.
 */
public final class Transacao {
    @FunctionalInterface
    public interface Operacao {
        void executar() throws SQLException;
    }

    private Transacao() {
    }

    public static void executar(Connection con, Operacao operacao) {
        boolean autoCommitOriginal;
        try {
            autoCommitOriginal = con.getAutoCommit();
            con.setAutoCommit(false);
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível iniciar a transação", e);
        }
        try {
            operacao.executar();
            con.commit();
        } catch (Exception e) {
            try {
                con.rollback();
            } catch (SQLException ignorada) {
                e.addSuppressed(ignorada);
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new ErroDeBancoException("Transação desfeita (rollback)", e);
        } finally {
            try {
                con.setAutoCommit(autoCommitOriginal);
            } catch (SQLException ignorada) {
                // conexão já inutilizável; nada a fazer
            }
        }
    }
}
