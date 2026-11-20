package br.com.fiap.citizentech.inss.dao;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Optional;

public class EnvioDao extends DaoBase {
    public EnvioDao(Connection con) {
        super(con);
    }

    public long inserir(long exigenciaId, long conversaId, String protocoloSimulado, LocalDateTime agora) {
        return inserir("INSERT INTO envio_protocolo (exigencia_id, conversa_id, protocolo_simulado, "
                + "confirmado_pelo_cidadao, enviado_em) VALUES (?, ?, ?, TRUE, ?)",
                exigenciaId, conversaId, protocoloSimulado, agora);
    }

    public long contar() {
        return consultar("SELECT COUNT(*) FROM envio_protocolo", rs -> rs.getLong(1)).get(0);
    }

    public Optional<String> buscarProtocoloPorExigencia(long exigenciaId) {
        return consultar("SELECT protocolo_simulado FROM envio_protocolo WHERE exigencia_id = ?",
                rs -> rs.getString(1), exigenciaId).stream().findFirst();
    }
}
