package br.com.fiap.citizentech.inss.servico;

import java.sql.Connection;
import java.time.Clock;

/** Monta o assistente com as suas dependências (fiação em um só lugar). */
public final class AssistenteFactory {
    private AssistenteFactory() {
    }

    public static AssistenteService criar(Connection con, Clock clock, boolean integracaoDisponivel,
                                          boolean envioDisponivel) {
        AutenticacaoSimulada autenticacao = new AutenticacaoSimulada(clock);
        InssGateway gateway = new InssGatewayFicticio(con, autenticacao, clock, integracaoDisponivel, envioDisponivel);
        return new AssistenteService(con, clock, gateway, autenticacao, new ExplicadorPorCatalogo());
    }

    public static AssistenteService criar(Connection con) {
        return criar(con, Clock.systemDefaultZone(), true, true);
    }
}
