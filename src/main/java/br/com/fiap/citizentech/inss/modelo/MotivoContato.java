package br.com.fiap.citizentech.inss.modelo;

/** Dificuldade que o cidadão declara no início. */
public enum MotivoContato {
    NAO_ENTENDI("Não entendi o que o INSS está pedindo",
            "Vou mostrar o texto oficial e explicar com palavras simples."),
    QUAIS_DOCUMENTOS("Não sei quais documentos mandar",
            "Vou montar uma lista só com o que o seu caso pede."),
    PRAZO("Tenho medo de perder o prazo",
            "Vou mostrar o prazo registrado e quantos dias faltam."),
    NAO_CONSIGO_ANEXAR("Não consigo anexar no Meu INSS",
            "Vou conferir com você o que está pronto e indicar o caminho no Meu INSS.");

    private final String texto;
    private final String dica;

    MotivoContato(String texto, String dica) {
        this.texto = texto;
        this.dica = dica;
    }

    public String texto() {
        return texto;
    }

    public String dica() {
        return dica;
    }
}
