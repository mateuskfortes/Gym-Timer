package com.example.gymtimer2

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.gymtimer2.ui.screens.exercise_list.WorkoutDayMenu
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Verifica ações, fechamento e proteção das opções indisponíveis no menu de dias. */
class WorkoutDayMenuTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Criar e renomear fecham o menu; excluir só fica disponível quando autorizado pela tela. */
    @Test
    fun actionsCloseMenuAndRespectDeletionAvailability() {
        val canRename = mutableStateOf(false)
        val canDelete = mutableStateOf(false)
        var creates = 0
        var renames = 0
        var deletes = 0
        composeRule.setContent {
            GymTimerTheme {
                WorkoutDayMenu(
                    canRename = canRename.value, canDelete = canDelete.value,
                    onCreate = { creates++ }, onRename = { renames++ }, onDelete = { deletes++ }
                )
            }
        }
        val menuDescription = composeRule.activity.getString(R.string.workout_day_menu)
        val createLabel = composeRule.activity.getString(R.string.create_day)
        val renameLabel = composeRule.activity.getString(R.string.rename_day)
        val deleteLabel = composeRule.activity.getString(R.string.delete_day)
        composeRule.onNodeWithContentDescription(menuDescription).performClick()
        composeRule.onNodeWithText(deleteLabel).assertIsNotEnabled()
        composeRule.onNodeWithText(renameLabel).assertIsNotEnabled()
        composeRule.onNodeWithText(createLabel).performClick()
        composeRule.onNodeWithText(createLabel).assertDoesNotExist()
        composeRule.runOnIdle { canRename.value = true }
        composeRule.onNodeWithContentDescription(menuDescription).performClick()
        composeRule.onNodeWithText(renameLabel).performClick()
        composeRule.onNodeWithText(renameLabel).assertDoesNotExist()
        composeRule.runOnIdle { canDelete.value = true }
        composeRule.onNodeWithContentDescription(menuDescription).performClick()
        composeRule.onNodeWithText(deleteLabel).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(deleteLabel).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, creates)
            assertEquals(1, renames)
            assertEquals(1, deletes)
        }
    }
}
