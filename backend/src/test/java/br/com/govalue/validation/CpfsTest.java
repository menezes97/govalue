package br.com.govalue.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfsTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "11144477735", "12345678909"})
    void aceitaCpfValido(String cpf) {
        assertTrue(Cpfs.valido(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "12345678900", "1234567890", "123456789012", "abc"})
    void rejeitaCpfComDigitoOuTamanhoErrado(String cpf) {
        assertFalse(Cpfs.valido(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "99999999999"})
    void rejeitaSequenciasRepetidas(String cpf) {
        assertFalse(Cpfs.valido(cpf));
    }

    @Test
    void rejeitaNuloOuVazio() {
        assertFalse(Cpfs.valido(null));
        assertFalse(Cpfs.valido(""));
    }

    @Test
    void normalizaRemovendoPontuacao() {
        assertEquals("52998224725", Cpfs.normalizar("529.982.247-25"));
    }
}
