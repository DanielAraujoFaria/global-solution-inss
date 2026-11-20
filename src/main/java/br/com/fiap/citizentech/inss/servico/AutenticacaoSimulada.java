package br.com.fiap.citizentech.inss.servico;

import br.com.fiap.citizentech.inss.modelo.AcessoNegadoException;
import br.com.fiap.citizentech.inss.modelo.Cidadao;
import br.com.fiap.citizentech.inss.modelo.Escopo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Simula o login gov.br com consentimento por escopos e sessão temporária.
 * Nenhuma senha existe no protótipo: o "login" acontece fora do chat e devolve apenas um token efêmero.
 */
public class AutenticacaoSimulada {
    public static final Duration VALIDADE = Duration.ofMinutes(10);

    /** Sessão autorizada. O token não aparece no toString para não vazar em logs. */
    public record Sessao(String token, long cidadaoId, Set<Escopo> escopos, Instant expiraEm) {
        @Override
        public String toString() {
            return "Sessao[cidadaoId=" + cidadaoId + ", escopos=" + escopos + ", expiraEm=" + expiraEm + "]";
        }
    }

    private final Clock clock;
    private final Map<String, Sessao> sessoes = new HashMap<>();

    public AutenticacaoSimulada(Clock clock) {
        this.clock = clock;
    }

    public Sessao iniciarSessao(Cidadao cidadao, Set<Escopo> escopos) {
        Sessao sessao = new Sessao(UUID.randomUUID().toString(), cidadao.getId(), Set.copyOf(escopos),
                clock.instant().plus(VALIDADE));
        sessoes.put(sessao.token(), sessao);
        return sessao;
    }

    /** Falha com {@link AcessoNegadoException} se a sessão não existe, expirou ou não cobre o escopo. */
    public void validar(Sessao sessao, Escopo exigido) {
        Sessao registrada = sessao == null ? null : sessoes.get(sessao.token());
        if (registrada == null) {
            throw new AcessoNegadoException("Sessão desconhecida ou encerrada");
        }
        if (!clock.instant().isBefore(registrada.expiraEm())) {
            sessoes.remove(registrada.token());
            throw new AcessoNegadoException("Sessão expirada");
        }
        if (!registrada.escopos().contains(exigido)) {
            throw new AcessoNegadoException("Escopo não autorizado: " + exigido.descricao());
        }
    }

    public void encerrar(Sessao sessao) {
        if (sessao != null) {
            sessoes.remove(sessao.token());
        }
    }

    /** Referência não reversível da sessão, segura para guardar no banco. */
    public String referenciaSegura(Sessao sessao) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(sessao.token().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
