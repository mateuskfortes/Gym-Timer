package com.example.gymtimer2.domain.model

/** Unidade de exibição; o clima permanece armazenado em Celsius. */
enum class RunTemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun fromCelsius(value: Double): Double = when (this) {
        CELSIUS -> value
        FAHRENHEIT -> value * 9.0 / 5.0 + 32.0
    }

    companion object {
        fun fromStoredValue(value: String?): RunTemperatureUnit =
            entries.firstOrNull { it.name == value } ?: CELSIUS
    }
}
