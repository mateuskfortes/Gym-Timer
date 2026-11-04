@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunGear
import com.example.gymtimer2.ui.screens.runs.RunGearCatalog
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.history.RunEditDetailsDialog
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Confirma que a edição começa com os metadados salvos e envia os valores novos. */
class RunEditDetailsDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val savedDetails = mutableListOf<RunSaveDetails>()
    private val run = RunModel(
        id = 41,
        startedAtEpochMs = 1L,
        finishedAtEpochMs = 2L,
        durationMs = 1_000L,
        distanceMeters = 100.0,
        status = RunStatus.COMPLETED,
        currentSegment = 0,
        title = "Corrida salva",
        description = "Nota original",
        gearName = "Tênis antigo",
        activityType = RunActivityType.RUN,
        isCommute = true,
        perceivedExertion = 4,
        activityTags = setOf(RunActivityTag.RACE)
    )

    @Test
    fun startsWithSavedDetailsAndSavesChanges() {
        composeRule.setContent {
            GymTimerTheme {
                RunEditDetailsDialog(run, onSave = { savedDetails.add(it) }, onDismiss = {})
            }
        }

        composeRule.onNodeWithText("Corrida salva").assertIsDisplayed()
        composeRule.onNodeWithText("Tênis antigo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(
                R.string.run_activity_type_select,
                text(R.string.run_activity_type)
            )
        ).performClick()
        composeRule.onNodeWithText(text(R.string.run_activity_type_walk)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.run_activity_type_hike)).performClick()
        composeRule.onNodeWithText(text(R.string.run_title_label)).performTextClearance()
        composeRule.onNodeWithText(text(R.string.run_title_label)).performTextInput("Corrida ajustada")
        composeRule.onNodeWithText(text(R.string.run_gear_label)).performScrollTo().performTextClearance()
        composeRule.onNodeWithText(text(R.string.run_gear_label)).performTextInput("Tênis novo")
        composeRule.onNodeWithText("9").performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.run_activity_tag_workout)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    RunSaveDetails(
                        "Corrida ajustada",
                        "Nota original",
                        true,
                        9,
                        RunActivityType.HIKE,
                        setOf(RunActivityTag.RACE, RunActivityTag.WORKOUT),
                        gearName = "Tênis novo"
                    )
                ),
                savedDetails
            )
        }
    }

    @Test
    fun editsPrivateNotes() {
        val runWithoutDetails = run.copy(
            title = null,
            description = null,
            gearName = null,
            isCommute = false,
            perceivedExertion = null,
            activityTags = emptySet()
        )
        composeRule.setContent {
            GymTimerTheme {
                RunEditDetailsDialog(runWithoutDetails, onSave = { savedDetails.add(it) }, onDismiss = {})
            }
        }

        composeRule.onNodeWithText(text(R.string.run_private_notes_label)).performScrollTo()
            .performTextInput("Nota pessoal nova")
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(RunSaveDetails(activityType = RunActivityType.RUN, privateNotes = "Nota pessoal nova")),
                savedDetails
            )
        }
    }

    @Test
    fun createsEquipmentAndSavesItsCatalogId() {
        val runWithoutGear = run.copy(gearName = null, gearId = null)
        composeRule.setContent {
            GymTimerTheme {
                RunEditDetailsDialog(
                    run = runWithoutGear,
                    gearCatalog = RunGearCatalog(create = { name, onCreated ->
                        onCreated(RunGear(42L, name, 0.0, false))
                    }),
                    onSave = { savedDetails.add(it) },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.run_gear_label)).performScrollTo().performTextInput("Tênis leve")
        composeRule.onNodeWithText(text(R.string.run_gear_browse)).performClick()
        composeRule.onNodeWithText(text(R.string.run_gear_add)).performClick()
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()

        composeRule.runOnIdle {
            assertEquals("Tênis leve", savedDetails.single().gearName)
            assertEquals(42L, savedDetails.single().gearId)
        }
    }

    @Test
    fun setsDefaultAndSelectsAnExistingCatalogEquipment() {
        var defaultGearId: Long? = null
        val gear = RunGear(19L, "Tênis de rodagem", 3_200.0, false)
        composeRule.setContent {
            GymTimerTheme {
                RunEditDetailsDialog(
                    run = run.copy(gearName = null, gearId = null),
                    gearCatalog = RunGearCatalog(items = listOf(gear), setDefault = { defaultGearId = it }),
                    onSave = { savedDetails.add(it) },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.run_gear_browse)).performClick()
        composeRule.onNodeWithText(text(R.string.run_gear_make_default)).performClick()
        composeRule.onNodeWithText("Tênis de rodagem").performClick()
        composeRule.onNodeWithText(text(R.string.run_save_action)).performClick()

        composeRule.runOnIdle {
            assertEquals(19L, defaultGearId)
            assertEquals(19L, savedDetails.single().gearId)
            assertEquals("Tênis de rodagem", savedDetails.single().gearName)
        }
    }

    @Test
    fun retiredEquipmentCanBeReactivatedFromTheCatalog() {
        var retiredGearChange: Pair<Long, Boolean>? = null
        val gear = RunGear(55L, "Tênis arquivado", 7_000.0, false, true)
        composeRule.setContent {
            GymTimerTheme {
                RunEditDetailsDialog(
                    run = run.copy(gearName = null, gearId = null),
                    gearCatalog = RunGearCatalog(
                        items = listOf(gear),
                        setRetired = { id, retired -> retiredGearChange = id to retired }
                    ),
                    onSave = { savedDetails.add(it) },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.run_gear_browse)).performClick()
        composeRule.onNodeWithText(text(R.string.run_gear_show_retired)).performClick()
        composeRule.onNodeWithText(text(R.string.run_gear_reactivate)).performClick()

        composeRule.runOnIdle { assertEquals(55L to false, retiredGearChange) }
    }

    private fun text(resourceId: Int) = composeRule.activity.getString(resourceId)
}
