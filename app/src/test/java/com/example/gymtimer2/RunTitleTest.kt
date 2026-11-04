package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunSaveDetails
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

    @Test
    fun equipmentNameIsTrimmedOptionalAndLimited() {
        assertEquals("Tênis", RunSaveDetails(gearName = "  Tênis  ").normalized().gearName)
        assertNull(RunSaveDetails(gearName = "  ").normalized().gearName)
        assertEquals(
            RunSaveDetails.MAX_GEAR_NAME_LENGTH,
            RunSaveDetails(gearName = "T".repeat(100)).normalized().gearName?.length
        )
        assertEquals(12L, RunSaveDetails(gearName = "Tênis", gearId = 12L).normalized().gearId)
        assertNull(RunSaveDetails(gearName = " ", gearId = 12L).normalized().gearId)
    }

    @Test
    fun privateNotesAreTrimmedOptionalAndLimited() {
        assertEquals("Anotar hidratação", RunSaveDetails(privateNotes = "  Anotar hidratação  ").normalized().privateNotes)
        assertNull(RunSaveDetails(privateNotes = "  ").normalized().privateNotes)
        assertEquals(
            RunSaveDetails.MAX_PRIVATE_NOTES_LENGTH,
            RunSaveDetails(privateNotes = "N".repeat(2_100)).normalized().privateNotes?.length
        )
    }
}
