package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cobre entradas opcionais e limites do título sem depender do Android. */
class RunTitleTest {
    /** Salvar sem nome mantém a identificação por data disponível para a interface. */
    @Test
    fun emptyAndWhitespaceTitlesRemainOptional() {
        assertNull(RunTitle.normalize(null))
        assertNull(RunTitle.normalize(""))
        assertNull(RunTitle.normalize(" \n\t "))
    }

    /** Espaços internos pertencem ao nome; espaços nas bordas não são salvos. */
    @Test
    fun savingTrimsOuterWhitespace() {
        assertEquals("Corrida no parque", RunTitle.normalize("  Corrida no parque  "))
    }

    /** Mesmo chamadas fora da UI devem respeitar o limite persistido. */
    @Test
    fun overlyLongTitleRespectsLengthLimit() {
        assertEquals("A".repeat(80), RunTitle.normalize("A".repeat(100)))
    }
}
