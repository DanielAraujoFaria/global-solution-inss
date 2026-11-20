package br.com.fiap.citizentech.inss;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Relógio controlável para testar expiração de sessão sem esperar de verdade. */
public class RelogioMutavel extends Clock {
    private Instant agora;
    private final ZoneId zona;

    public RelogioMutavel() {
        this(Clock.systemDefaultZone());
    }

    public RelogioMutavel(Clock base) {
        this.agora = base.instant();
        this.zona = base.getZone();
    }

    public void avancar(Duration d) {
        agora = agora.plus(d);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
