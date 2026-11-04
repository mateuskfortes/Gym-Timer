package com.example.gymtimer2.domain.model

import kotlin.math.roundToInt

const val MIN_RUN_BODY_MASS_KG = 20.0
const val MAX_RUN_BODY_MASS_KG = 300.0

fun isValidRunBodyMassKg(bodyMassKg: Double): Boolean =
    bodyMassKg.isFinite() && bodyMassKg in MIN_RUN_BODY_MASS_KG..MAX_RUN_BODY_MASS_KG

fun parseRunBodyMassKg(value: String): Double? = value.trim()
    .replace(',', '.')
    .toDoubleOrNull()
    ?.takeIf(::isValidRunBodyMassKg)

/** Estima 1 kcal por kg de massa corporal e km percorrido; não reproduz o cálculo remoto do Strava. */
fun estimateRunCaloriesKcal(bodyMassKg: Double?, distanceMeters: Double): Int? {
    if (bodyMassKg == null || !isValidRunBodyMassKg(bodyMassKg)) return null
    if (!distanceMeters.isFinite() || distanceMeters <= 0.0) return null
    return (bodyMassKg * distanceMeters / 1_000.0).roundToInt()
}
