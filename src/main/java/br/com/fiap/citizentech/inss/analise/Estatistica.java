package br.com.fiap.citizentech.inss.analise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estatística descritiva e inferencial básica, usada nos indicadores.
 * Implementada à mão (sem biblioteca) para que cada fórmula possa ser explicada no vídeo.
 */
public final class Estatistica {
    private static final double Z_95 = 1.96;
    /** Valores críticos da distribuição t de Student (bicaudal, 95%) para 1 a 30 graus de liberdade. */
    private static final double[] T_95 = {
            12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228,
            2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
            2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042};

    private Estatistica() {
    }

    public static double media(List<? extends Number> valores) {
        exigirNaoVazio(valores);
        double soma = 0;
        for (Number v : valores) {
            soma += v.doubleValue();
        }
        return soma / valores.size();
    }

    public static double mediana(List<? extends Number> valores) {
        exigirNaoVazio(valores);
        List<Double> ordenados = new ArrayList<>();
        for (Number v : valores) {
            ordenados.add(v.doubleValue());
        }
        Collections.sort(ordenados);
        int n = ordenados.size();
        return n % 2 == 1 ? ordenados.get(n / 2) : (ordenados.get(n / 2 - 1) + ordenados.get(n / 2)) / 2.0;
    }

    /** Desvio padrão amostral (divide por n-1). Com um único valor, devolve 0. */
    public static double desvioPadrao(List<? extends Number> valores) {
        exigirNaoVazio(valores);
        int n = valores.size();
        if (n < 2) {
            return 0;
        }
        double media = media(valores);
        double soma = 0;
        for (Number v : valores) {
            double d = v.doubleValue() - media;
            soma += d * d;
        }
        return Math.sqrt(soma / (n - 1));
    }

    public static double minimo(List<? extends Number> valores) {
        exigirNaoVazio(valores);
        return valores.stream().mapToDouble(Number::doubleValue).min().orElseThrow();
    }

    public static double maximo(List<? extends Number> valores) {
        exigirNaoVazio(valores);
        return valores.stream().mapToDouble(Number::doubleValue).max().orElseThrow();
    }

    /** Proporção em [0,1]; 0 quando não há observações. */
    public static double proporcao(long sucessos, long total) {
        return total == 0 ? 0 : (double) sucessos / total;
    }

    /**
     * Intervalo de confiança de 95% de Wilson para uma proporção. Funciona bem com amostras pequenas
     * (ao contrário da aproximação normal simples). Devolve {inferior, superior}.
     */
    public static double[] intervaloWilson(long sucessos, long total) {
        if (total == 0) {
            return new double[] {0, 0};
        }
        double p = (double) sucessos / total;
        double z2 = Z_95 * Z_95;
        double denominador = 1 + z2 / total;
        double centro = (p + z2 / (2.0 * total)) / denominador;
        double margem = Z_95 * Math.sqrt(p * (1 - p) / total + z2 / (4.0 * total * total)) / denominador;
        return new double[] {Math.max(0, centro - margem), Math.min(1, centro + margem)};
    }

    /** Intervalo de confiança de 95% para a média (t de Student). Devolve {inferior, superior}. */
    public static double[] intervaloDaMedia(List<? extends Number> valores) {
        exigirNaoVazio(valores);
        int n = valores.size();
        double media = media(valores);
        if (n < 2) {
            return new double[] {media, media};
        }
        int graus = n - 1;
        double t = graus <= T_95.length ? T_95[graus - 1] : Z_95;
        double margem = t * desvioPadrao(valores) / Math.sqrt(n);
        return new double[] {media - margem, media + margem};
    }

    private static void exigirNaoVazio(List<? extends Number> valores) {
        if (valores == null || valores.isEmpty()) {
            throw new IllegalArgumentException("Sem observações");
        }
    }
}
