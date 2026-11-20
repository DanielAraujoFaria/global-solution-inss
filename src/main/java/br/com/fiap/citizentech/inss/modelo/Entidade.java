package br.com.fiap.citizentech.inss.modelo;

/** Base das classes persistidas: todas têm identificador gerado pelo banco. */
public abstract class Entidade {
    private Long id;

    protected Entidade(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
