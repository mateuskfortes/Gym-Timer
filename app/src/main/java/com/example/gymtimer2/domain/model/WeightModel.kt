package com.example.gymtimer2.domain.model

data class WeightModel(
    val value: Int,
    val unit: WeightUnit
) {
    val plainText: String
        get() = "$value ${unit.label}"
}