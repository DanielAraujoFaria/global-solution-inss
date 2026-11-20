package br.com.fiap.citizentech.inss.modelo;

public class ItemChecklist extends Entidade {
    private final Long exigenciaId;
    private final String nome;
    private final String orientacao;
    private final boolean obrigatorio;
    private StatusItem status;

    public ItemChecklist(Long id, Long exigenciaId, String nome, String orientacao,
                         boolean obrigatorio, StatusItem status) {
        super(id);
        this.exigenciaId = exigenciaId;
        this.nome = nome;
        this.orientacao = orientacao;
        this.obrigatorio = obrigatorio;
        this.status = status;
    }

    public Long getExigenciaId() { return exigenciaId; }
    public String getNome() { return nome; }
    public String getOrientacao() { return orientacao; }
    public boolean isObrigatorio() { return obrigatorio; }
    public StatusItem getStatus() { return status; }
    public void setStatus(StatusItem status) { this.status = status; }
}
