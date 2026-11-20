package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.analise.Estatistica;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/** Valores de referência calculados de forma independente (Python: statistics e fórmula de Wilson). */
public class EstatisticaTest {
    private static final double TOLERANCIA = 1e-6;
    private static final List<Integer> AMOSTRA = List.of(2, 4, 4, 4, 5, 5, 7, 9);

    @Test
    public void mediaMedianaEDesvioPadrao() {
        assertEquals(5.0, Estatistica.media(AMOSTRA), TOLERANCIA);
        assertEquals(4.5, Estatistica.mediana(AMOSTRA), TOLERANCIA);
        assertEquals(2.138089935299395, Estatistica.desvioPadrao(AMOSTRA), TOLERANCIA);
        assertEquals(2.0, Estatistica.minimo(AMOSTRA), TOLERANCIA);
        assertEquals(9.0, Estatistica.maximo(AMOSTRA), TOLERANCIA);
    }

    @Test
    public void medianaDeQuantidadeImparEOrdemQualquer() {
        assertEquals(3.0, Estatistica.mediana(List.of(5, 1, 3)), TOLERANCIA);
    }

    @Test
    public void desvioPadraoDeUmUnicoValorEZero() {
        assertEquals(0.0, Estatistica.desvioPadrao(List.of(7)), TOLERANCIA);
    }

    @Test
    public void intervaloDeWilson() {
        assertArrayEquals(new double[] {0.49015684672072335, 0.9433190520193067}, Estatistica.intervaloWilson(8, 10), TOLERANCIA);
        assertArrayEquals(new double[] {0.0, 0.2775401687666166}, Estatistica.intervaloWilson(0, 10), TOLERANCIA);
        assertArrayEquals(new double[] {0.7224598312333834, 1.0}, Estatistica.intervaloWilson(10, 10), TOLERANCIA);
        assertArrayEquals(new double[] {0.08507538491591576, 0.43334714240266087}, Estatistica.intervaloWilson(4, 19), TOLERANCIA);
    }

    @Test
    public void intervaloDaMediaUsaTDeStudent() {
        double[] ic = Estatistica.intervaloDaMedia(List.of(4, 5, 6));
        assertArrayEquals(new double[] {2.51566179167704, 7.48433820832296}, ic, 1e-4);
    }

    @Test
    public void proporcaoSemObservacoesEZero() {
        assertEquals(0.0, Estatistica.proporcao(0, 0), TOLERANCIA);
        assertEquals(0.25, Estatistica.proporcao(1, 4), TOLERANCIA);
    }

    @Test
    public void listaVaziaELancadaComoErro() {
        assertThrows(IllegalArgumentException.class, () -> Estatistica.media(List.of()));
    }
}
