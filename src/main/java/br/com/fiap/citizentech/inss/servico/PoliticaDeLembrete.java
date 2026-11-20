package br.com.fiap.citizentech.inss.servico;

import java.util.Optional;

/** Define com quantos dias de antecedência o lembrete (consentido) deve ser enviado. */
public final class PoliticaDeLembrete {
    public static final int ANTECEDENCIA_MAXIMA_DIAS = 3;

    private PoliticaDeLembrete() {
    }

    /**
     * Até 3 dias antes do prazo. Se faltam menos de 2 dias não há tempo útil para lembrar:
     * nesse caso não se oferece lembrete e o cidadão é orientado a procurar o INSS.
     */
    public static Optional<Integer> diasParaLembrete(long diasRestantes) {
        if (diasRestantes < 2) {
            return Optional.empty();
        }
        return Optional.of((int) Math.min(ANTECEDENCIA_MAXIMA_DIAS, diasRestantes - 1));
    }
}
