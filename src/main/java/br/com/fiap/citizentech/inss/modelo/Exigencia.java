package br.com.fiap.citizentech.inss.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Pendência registrada pelo INSS em um pedido. Concentra as regras de prazo. */
public class Exigencia extends Entidade {
    /** A partir de quantos dias restantes o assistente avisa que o prazo está curto. */
    public static final int LIMITE_URGENCIA_DIAS = 5;

    private final Long pedidoId;
    private final TipoExigencia tipo;
    private final String textoOficial;
    private final LocalDate dataEmissao;
    private final LocalDate dataPrazo;
    private StatusExigencia status;
    private LocalDate dataCumprimento;

    public Exigencia(Long id, Long pedidoId, TipoExigencia tipo, String textoOficial,
                     LocalDate dataEmissao, LocalDate dataPrazo, StatusExigencia status, LocalDate dataCumprimento) {
        super(id);
        this.pedidoId = pedidoId;
        this.tipo = tipo;
        this.textoOficial = textoOficial;
        this.dataEmissao = dataEmissao;
        this.dataPrazo = dataPrazo;
        this.status = status;
        this.dataCumprimento = dataCumprimento;
    }

    public Long getPedidoId() { return pedidoId; }
    public TipoExigencia getTipo() { return tipo; }
    public String getTextoOficial() { return textoOficial; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public LocalDate getDataPrazo() { return dataPrazo; }
    public StatusExigencia getStatus() { return status; }
    public LocalDate getDataCumprimento() { return dataCumprimento; }

    public void atualizarStatus(StatusExigencia novoStatus, LocalDate cumprimento) {
        this.status = novoStatus;
        this.dataCumprimento = cumprimento;
    }

    /** Dias entre hoje e o prazo (negativo quando já venceu; 0 = vence hoje). */
    public long diasRestantes(LocalDate hoje) {
        return ChronoUnit.DAYS.between(hoje, dataPrazo);
    }

    public boolean estaVencida(LocalDate hoje) {
        return diasRestantes(hoje) < 0;
    }

    public boolean ehUrgente(LocalDate hoje) {
        long dias = diasRestantes(hoje);
        return dias >= 0 && dias <= LIMITE_URGENCIA_DIAS;
    }
}
