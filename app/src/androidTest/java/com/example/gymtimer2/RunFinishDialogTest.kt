@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import com.example.gymtimer2.ui.screens.runs.active.RunFinishDialog
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Verifica as escolhas de salvamento e a proteção contra descarte acidental. */
class RunFinishDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val savedTitles = mutableListOf<String?>()
    private var discarded = 0
    private var dismissed = 0

    /** O nome é opcional e normalizado antes de chegar à ação de salvamento. */
    @Test
    fun savesTrimmedTitle() {
        showDialog()
        composeRule.onNodeWithText(text(R.string.run_title_label)).performTextInput("  Corrida no parque  ")
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("Corrida no parque"), savedTitles)
            assertEquals(0, discarded)
        }
    }

    /** Uma corrida sem título também pode ser salva normalmente. */
    @Test
    fun savesWithoutTitle() {
        showDialog()
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()
        composeRule.runOnIdle { assertEquals(listOf<String?>(null), savedTitles) }
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
            assertEquals(emptyList<String?>(), savedTitles)
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
            assertEquals(emptyList<String?>(), savedTitles)
        }
    }

    /** Isola o formulário dos efeitos de serviço e persistência. */
    private fun showDialog() {
        composeRule.setContent {
            GymTimerTheme {
                RunFinishDialog(
                    onConfirm = { savedTitles.add(it) },
                    onDiscard = { discarded++ },
                    onDismiss = { dismissed++ }
                )
            }
        }
    }

    /** Usa os textos da mesma configuração de recursos apresentada pelo aplicativo. */
    private fun text(resourceId: Int) = composeRule.activity.getString(resourceId)
}
