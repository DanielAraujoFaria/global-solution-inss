package br.com.fiap.citizentech.inss.modelo;

public class TipoExigencia extends Entidade {
    private final String codigo;
    private final String titulo;
    private final String textoOficialExemplo;
    private final String explicacaoSimples;
    private final int prazoPadraoDias;
    private final boolean exigeAnaliseHumana;

    public TipoExigencia(Long id, String codigo, String titulo, String textoOficialExemplo,
                         String explicacaoSimples, int prazoPadraoDias, boolean exigeAnaliseHumana) {
        super(id);
        this.codigo = codigo;
        this.titulo = titulo;
        this.textoOficialExemplo = textoOficialExemplo;
        this.explicacaoSimples = explicacaoSimples;
        this.prazoPadraoDias = prazoPadraoDias;
        this.exigeAnaliseHumana = exigeAnaliseHumana;
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public String getTextoOficialExemplo() { return textoOficialExemplo; }
    public String getExplicacaoSimples() { return explicacaoSimples; }
    public int getPrazoPadraoDias() { return prazoPadraoDias; }
    public boolean exigeAnaliseHumana() { return exigeAnaliseHumana; }
}
