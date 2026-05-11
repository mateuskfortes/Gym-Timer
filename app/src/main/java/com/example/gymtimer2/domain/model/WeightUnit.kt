package com.example.gymtimer2.domain.model

enum class WeightUnit(
    val code: Int,
    val label: String
) {
    TOTAL_KG(0, "kg"),
    MACHINE_STACK_UNITS(1, "slots");

    companion object {
        fun fromCode(code: Int): WeightUnit {
            return entries.firstOrNull { it.code == code } ?: TOTAL_KG
        }
    }
}