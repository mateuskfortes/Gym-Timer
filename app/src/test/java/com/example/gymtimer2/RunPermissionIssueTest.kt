package com.example.gymtimer2

import android.Manifest
import com.example.gymtimer2.ui.screens.runs.permissions.RunStartIssue
import com.example.gymtimer2.ui.screens.runs.permissions.runLocationPermissionIssue
import com.example.gymtimer2.ui.screens.runs.permissions.runPermissionsToRequest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunPermissionIssueTest {
    @Test
    fun preciseLocationNeedsNoRecoveryDialog() {
        assertNull(runLocationPermissionIssue(preciseLocationGranted = true, approximateLocationGranted = true))
    }

    @Test
    fun approximateLocationGetsItsOwnRecoveryDialog() {
        assertEquals(
            RunStartIssue.APPROXIMATE_LOCATION,
            runLocationPermissionIssue(preciseLocationGranted = false, approximateLocationGranted = true)
        )
    }

    @Test
    fun deniedLocationGetsSettingsRecoveryDialog() {
        assertEquals(
            RunStartIssue.LOCATION_DENIED,
            runLocationPermissionIssue(preciseLocationGranted = false, approximateLocationGranted = false)
        )
    }

    @Test
    fun preparationRequestsLocationWithoutOptionalPermissions() {
        assertArrayEquals(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            runPermissionsToRequest(
                preciseLocationGranted = false,
                optionalPermissions = listOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ),
                requestOptionalPermissions = false
            )
        )
    }

    @Test
    fun initialRequestIncludesMissingOptionalPermissions() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACTIVITY_RECOGNITION
            ),
            runPermissionsToRequest(
                preciseLocationGranted = false,
                optionalPermissions = listOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ),
                requestOptionalPermissions = true
            )
        )
    }
}
