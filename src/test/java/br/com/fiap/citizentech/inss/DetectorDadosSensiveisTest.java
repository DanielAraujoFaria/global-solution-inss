package br.com.fiap.citizentech.inss;

import br.com.fiap.citizentech.inss.servico.DetectorDadosSensiveis;
import br.com.fiap.citizentech.inss.servico.DetectorDadosSensiveis.Analise;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DetectorDadosSensiveisTest {
    private final DetectorDadosSensiveis detector = new DetectorDadosSensiveis();

    @Test
    public void entradasNormaisNaoSaoMarcadas() {
        for (String texto : new String[] {"1", "3", "0", "Preciso de ajuda com o documento", "2026", "FICT-0001", ""}) {
            assertFalse("não deveria marcar: " + texto, detector.analisar(texto).sensivel());
        }
    }

    @Test
    public void senhaEDescartadaPorInteiro() {
        Analise a = detector.analisar("minha senha é abc987");
        assertTrue(a.sensivel());
        assertFalse(a.textoParaRegistro().contains("abc987"));
        assertEquals("CREDENCIAL", a.tipos().get(0));
    }

    @Test
    public void codigoDeVerificacaoPorExtensoEPorNumero() {
        assertTrue(detector.analisar("meu código de verificação é 9").sensivel());
        Analise a = detector.analisar("123456");
        assertTrue(a.sensivel());
        assertFalse(a.textoParaRegistro().contains("123456"));
    }

    @Test
    public void cpfComEPontuacaoOuSem() {
        for (String texto : new String[] {"meu cpf é 123.456.789-09", "12345678909"}) {
            Analise a = detector.analisar(texto);
            assertTrue(texto, a.sensivel());
            assertTrue(texto, a.tipos().contains("CPF"));
            assertFalse(texto, a.textoParaRegistro().matches(".*\\d{3}.*"));
        }
    }

    @Test
    public void numeroDeCartao() {
        Analise a = detector.analisar("cartão 4111 1111 1111 1111");
        assertTrue(a.sensivel());
        assertTrue(a.tipos().contains("CARTAO"));
        assertFalse(a.textoParaRegistro().contains("4111"));
    }
}
