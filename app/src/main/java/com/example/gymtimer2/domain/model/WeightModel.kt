package com.example.gymtimer2.domain.model

import java.io.Serializable

data class WeightModel(
    val value: Int,
    val unit: WeightUnit
): Serializable {
    val plainText: String
        get() = "$value ${unit.label}"
}