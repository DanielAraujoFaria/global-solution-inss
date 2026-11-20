package br.com.fiap.citizentech.inss.modelo;

/** Pessoa fictícia que usa o assistente (nenhum dado real). */
public class Cidadao extends Entidade {
    private final String nomeFicticio;
    private final String faixaEtaria;
    private final TipoAcesso tipoAcesso;
    private final Long representaId;

    public Cidadao(Long id, String nomeFicticio, String faixaEtaria, TipoAcesso tipoAcesso, Long representaId) {
        super(id);
        this.nomeFicticio = nomeFicticio;
        this.faixaEtaria = faixaEtaria;
        this.tipoAcesso = tipoAcesso;
        this.representaId = representaId;
    }

    public String getNomeFicticio() { return nomeFicticio; }
    public String getFaixaEtaria() { return faixaEtaria; }
    public TipoAcesso getTipoAcesso() { return tipoAcesso; }
    public Long getRepresentaId() { return representaId; }

    /** Primeiro nome, usado para tratar a pessoa com proximidade. */
    public String primeiroNome() {
        int espaco = nomeFicticio.indexOf(' ');
        return espaco < 0 ? nomeFicticio : nomeFicticio.substring(0, espaco);
    }
}
