package br.com.fiap.citizentech.inss.modelo;

import java.time.LocalDate;

public class Pedido extends Entidade {
    private final Long cidadaoId;
    private final String protocolo;
    private final String beneficio;
    private final StatusPedido status;
    private final boolean dadosDivergentes;
    private final LocalDate dataPedido;

    public Pedido(Long id, Long cidadaoId, String protocolo, String beneficio, StatusPedido status,
                  boolean dadosDivergentes, LocalDate dataPedido) {
        super(id);
        this.cidadaoId = cidadaoId;
        this.protocolo = protocolo;
        this.beneficio = beneficio;
        this.status = status;
        this.dadosDivergentes = dadosDivergentes;
        this.dataPedido = dataPedido;
    }

    public Long getCidadaoId() { return cidadaoId; }
    public String getProtocolo() { return protocolo; }
    public String getBeneficio() { return beneficio; }
    public StatusPedido getStatus() { return status; }
    public boolean temDadosDivergentes() { return dadosDivergentes; }
    public LocalDate getDataPedido() { return dataPedido; }
}
