package br.com.fiap.citizentech.inss.db;

import br.com.fiap.citizentech.inss.modelo.ErroDeBancoException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexões JDBC. O protótipo usa H2 em arquivo (nada para instalar).
 * Para trocar de banco (ex.: Oracle da FIAP) basta informar outra URL, usuário e senha por
 * propriedades do sistema (-Dinss.db.url=...) ou variáveis de ambiente (INSS_DB_URL, INSS_DB_USER, INSS_DB_PASSWORD)
 * e incluir o driver no classpath.
 */
public final class Conexao {
    public static final String URL_PADRAO = "jdbc:h2:./dados/assistente_inss";

    private Conexao() {
    }

    public static Connection abrir() {
        String url = configuracao("inss.db.url", "INSS_DB_URL", URL_PADRAO);
        String usuario = configuracao("inss.db.user", "INSS_DB_USER", "sa");
        String senha = configuracao("inss.db.password", "INSS_DB_PASSWORD", "");
        return abrir(url, usuario, senha);
    }

    public static Connection abrir(String url, String usuario, String senha) {
        try {
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível conectar ao banco: " + url, e);
        }
    }

    /** Banco em memória, isolado por nome (usado nos testes e nas simulações). */
    public static Connection emMemoria(String nome) {
        return abrir("jdbc:h2:mem:" + nome + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    private static String configuracao(String propriedade, String variavel, String padrao) {
        String valor = System.getProperty(propriedade);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variavel);
        }
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }
}
