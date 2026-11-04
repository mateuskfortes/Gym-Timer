package com.example.gymtimer2.ui.screens.runs.format

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunTemperatureUnit
import com.example.gymtimer2.domain.model.RunMetrics
import java.util.Locale
import java.text.NumberFormat

val LocalRunDistanceUnit = staticCompositionLocalOf { RunDistanceUnit.METRIC }
val LocalRunTemperatureUnit = staticCompositionLocalOf { RunTemperatureUnit.CELSIUS }

/** Formata métricas e unidades usadas pelas telas de corrida. */
internal object RunFormatting {
    fun distanceLabel(
        context: Context,
        distanceMeters: Double,
        unit: RunDistanceUnit,
        fractionDigits: Int = 2
    ): String {
        val number = formatNumber(unit.distanceFromMeters(distanceMeters), fractionDigits)
        val unitLabel = context.getString(
            if (unit == RunDistanceUnit.METRIC) R.string.run_unit_kilometers else R.string.run_unit_miles
        )
        return context.getString(R.string.run_number_with_unit, number, unitLabel)
    }

    @Composable
    fun distanceLabel(distanceMeters: Double, fractionDigits: Int = 2): String {
        val unit = LocalRunDistanceUnit.current
        val number = formatNumber(unit.distanceFromMeters(distanceMeters), fractionDigits)
        val unitLabel = stringResource(
            if (unit == RunDistanceUnit.METRIC) R.string.run_unit_kilometers else R.string.run_unit_miles
        )
        return stringResource(R.string.run_number_with_unit, number, unitLabel)
    }

    @Composable
    fun elevationLabel(elevationMeters: Double, fractionDigits: Int = 0): String {
        val unit = LocalRunDistanceUnit.current
        val number = formatNumber(unit.elevationFromMeters(elevationMeters), fractionDigits)
        val unitLabel = stringResource(
            if (unit == RunDistanceUnit.METRIC) R.string.run_unit_meters else R.string.run_unit_feet
        )
        return stringResource(R.string.run_number_with_unit, number, unitLabel)
    }

    @Composable
    fun shortDistanceLabel(distanceMeters: Double): String {
        val unit = LocalRunDistanceUnit.current
        val number = formatNumber(
            if (unit == RunDistanceUnit.METRIC) distanceMeters else unit.feetFromMeters(distanceMeters),
            0
        )
        val unitLabel = stringResource(
            if (unit == RunDistanceUnit.METRIC) R.string.run_unit_meters else R.string.run_unit_feet
        )
        return stringResource(R.string.run_number_with_unit, number, unitLabel)
    }

    @Composable
    fun adaptiveDistanceLabel(distanceMeters: Double): String =
        if (distanceMeters >= LocalRunDistanceUnit.current.metersPerUnit) {
            distanceLabel(distanceMeters, 1)
        } else {
            shortDistanceLabel(distanceMeters)
        }

    @Composable
    fun speedLabel(speedKilometersPerHour: Double): String {
        val unit = LocalRunDistanceUnit.current
        val number = formatNumber(unit.speedFromKilometersPerHour(speedKilometersPerHour), 1)
        val unitLabel = stringResource(
            if (unit == RunDistanceUnit.METRIC) R.string.run_unit_kilometers_per_hour else R.string.run_unit_miles_per_hour
        )
        return stringResource(R.string.run_number_with_unit, number, unitLabel)
    }

    fun speedLabel(context: Context, speedKilometersPerHour: Double, unit: RunDistanceUnit): String {
        val number = formatNumber(unit.speedFromKilometersPerHour(speedKilometersPerHour), 1)
        val unitLabel = context.getString(
            if (unit == RunDistanceUnit.METRIC) R.string.run_unit_kilometers_per_hour else R.string.run_unit_miles_per_hour
        )
        return context.getString(R.string.run_number_with_unit, number, unitLabel)
    }

    @Composable
    fun pace(secondsPerKm: Double?): String = RunMetrics.formatPace(secondsPerKm, LocalRunDistanceUnit.current)

    @Composable
    fun distanceUnitLabel(): String = stringResource(
        if (LocalRunDistanceUnit.current == RunDistanceUnit.METRIC) R.string.run_unit_kilometers else R.string.run_unit_miles
    )

    @Composable
    fun elevationUnitLabel(): String = stringResource(
        if (LocalRunDistanceUnit.current == RunDistanceUnit.METRIC) R.string.run_unit_meters else R.string.run_unit_feet
    )

    @Composable
    fun distanceUnitName(): String = stringResource(
        if (LocalRunDistanceUnit.current == RunDistanceUnit.METRIC) R.string.run_unit_split_kilometers
        else R.string.run_unit_split_miles
    )

    fun workoutDistanceInputValue(distanceMeters: Double, unit: RunDistanceUnit): String =
        formatInputNumber(unit.distanceFromMeters(distanceMeters), 4)

    fun workoutElevationInputValue(elevationMeters: Double, unit: RunDistanceUnit): String =
        formatInputNumber(
            unit.elevationFromMeters(elevationMeters),
            if (unit == RunDistanceUnit.METRIC) 2 else 1
        )

    private fun formatNumber(value: Double, fractionDigits: Int): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = fractionDigits.coerceAtLeast(0)
            maximumFractionDigits = fractionDigits.coerceAtLeast(0)
        }.format(value)

    private fun formatInputNumber(value: Double, maximumFractionDigits: Int): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            isGroupingUsed = false
            this.maximumFractionDigits = maximumFractionDigits
        }.format(value)

}
