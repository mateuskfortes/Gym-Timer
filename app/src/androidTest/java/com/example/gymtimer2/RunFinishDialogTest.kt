@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.ui.screens.runs.active.RunFinishDialog
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Verifica as escolhas de salvamento e a proteção contra descarte acidental. */
class RunFinishDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val savedDetails = mutableListOf<RunSaveDetails>()
    private var discarded = 0
    private var dismissed = 0

    /** O nome é opcional e normalizado antes de chegar à ação de salvamento. */
    @Test
    fun savesTrimmedTitle() {
        showDialog()
        composeRule.onNodeWithText(text(R.string.run_title_label)).performTextInput("  Corrida no parque  ")
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(RunSaveDetails(title = "Corrida no parque")), savedDetails)
            assertEquals(0, discarded)
        }
    }

    /** Uma corrida sem título também pode ser salva normalmente. */
    @Test
    fun savesWithoutTitle() {
        showDialog()
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()
        composeRule.runOnIdle { assertEquals(listOf(RunSaveDetails()), savedDetails) }
    }

    /** Descrição e indicação de deslocamento seguem junto com os dados da atividade. */
    @Test
    fun savesDescriptionAndCommute() {
        showDialog()
        composeRule.onNodeWithText(text(R.string.run_description_label)).performTextInput("  Volta do trabalho  ")
        composeRule.onNodeWithText(text(R.string.run_commute_label)).performScrollTo().performTouchInput { click() }
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf(RunSaveDetails(description = "Volta do trabalho", isCommute = true)),
                savedDetails
            )
        }
    }

    @Test
    fun savesEquipmentName() {
        showDialog()
        composeRule.onNodeWithText(text(R.string.run_gear_label)).performScrollTo().performTextInput("Tênis de trilha")
        composeRule.onNodeWithText(text(R.string.run_private_notes_label)).performScrollTo()
            .performTextInput("Evitar esforço excessivo")
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf(RunSaveDetails(gearName = "Tênis de trilha", privateNotes = "Evitar esforço excessivo")),
                savedDetails
            )
        }
    }

    /** A escala é opcional e permite retirar uma seleção. */
    @Test
    fun savesPerceivedExertionAndAllowsClearingIt() {
        showDialog()
        composeRule.onNodeWithText("8").performScrollTo().performClick()
        composeRule.onNodeWithText("8").performScrollTo().performClick()
        composeRule.onNodeWithText("6").performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(RunSaveDetails(perceivedExertion = 6)), savedDetails)
        }
    }

    /** Voltar da confirmação preserva o rascunho; apenas a segunda confirmação descarta. */
    @Test
    fun discardRequiresConfirmationAndGoingBackPreservesTitle() {
        showDialog()
        composeRule.onNodeWithText(text(R.string.run_title_label)).performTextInput("Treino leve")
        composeRule.onNodeWithText(text(R.string.run_discard_action)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.run_discard_title)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, discarded) }
        composeRule.onNodeWithText(text(R.string.run_discard_back_action)).performClick()
        composeRule.onNodeWithText(text(R.string.run_title_label)).assertTextContains("Treino leve")
        composeRule.onNodeWithText(text(R.string.run_discard_action)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.run_discard_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(1, discarded)
            assertEquals(emptyList<RunSaveDetails>(), savedDetails)
        }
    }

    /** Cancelar não dispara salvamento nem descarte. */
    @Test
    fun cancelLeavesSessionUntouched() {
        showDialog()
        composeRule.onNodeWithText(text(R.string.cancel_button)).performClick()
        composeRule.runOnIdle {
            assertEquals(1, dismissed)
            assertEquals(0, discarded)
            assertEquals(emptyList<RunSaveDetails>(), savedDetails)
        }
    }

    /** Uma sessão sem distância pode ser retomada sem salvar nem descartar. */
    @Test
    fun emptyActivityCanResume() {
        showDialog(isEmptyActivity = true)
        composeRule.onNodeWithText(text(R.string.run_empty_activity_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.run_resume_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(1, dismissed)
            assertEquals(0, discarded)
            assertEquals(emptyList<RunSaveDetails>(), savedDetails)
        }
    }

    /** Descartar a sessão sem distância ainda exige confirmação e permite voltar. */
    @Test
    fun emptyActivityDiscardRequiresConfirmation() {
        showDialog(isEmptyActivity = true)
        composeRule.onNodeWithText(text(R.string.run_discard_action)).performClick()
        composeRule.onNodeWithText(text(R.string.run_discard_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.run_discard_back_action)).performClick()
        composeRule.onNodeWithText(text(R.string.run_empty_activity_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.run_discard_action)).performClick()
        composeRule.onNodeWithText(text(R.string.run_discard_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.run_discard_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(1, discarded)
            assertEquals(emptyList<RunSaveDetails>(), savedDetails)
        }
    }

    /** Isola o formulário dos efeitos de serviço e persistência. */
    private fun showDialog(isEmptyActivity: Boolean = false) {
        composeRule.setContent {
            GymTimerTheme {
                RunFinishDialog(
                    onConfirm = { savedDetails.add(it) },
                    onDiscard = { discarded++ },
                    onDismiss = { dismissed++ },
                    isEmptyActivity = isEmptyActivity
                )
            }
        }
    }

    /** Usa os textos da mesma configuração de recursos apresentada pelo aplicativo. */
    private fun text(resourceId: Int) = composeRule.activity.getString(resourceId)
}
