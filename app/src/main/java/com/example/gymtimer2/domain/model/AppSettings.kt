package com.example.gymtimer2.domain.model

enum class ThemeMode { LIGHT, DARK }

enum class RunStartEndHideDistance(val meters: Int) {
    NONE(0),
    METERS_200(200),
    METERS_400(400),
    METERS_600(600),
    METERS_800(800),
    METERS_1000(1_000),
    METERS_1200(1_200),
    METERS_1400(1_400),
    METERS_1600(1_600);

    companion object {
        fun fromMeters(meters: Int): RunStartEndHideDistance =
            entries.firstOrNull { it.meters == meters } ?: NONE
    }
}

const val DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS = 30_000L

enum class AccentColor(val lightArgb: Int, val darkArgb: Int) {
    PURPLE(0xFF67419B.toInt(), 0xFFB594E3.toInt()),
    INDIGO(0xFF414FA0.toInt(), 0xFFA9B4FF.toInt()),
    BLUE(0xFF1769AA.toInt(), 0xFF83BDFA.toInt()),
    CYAN(0xFF007C91.toInt(), 0xFF77D3E7.toInt()),
    TEAL(0xFF087A70.toInt(), 0xFF7DD4C7.toInt()),
    GREEN(0xFF367B43.toInt(), 0xFF91D69B.toInt()),
    LIME(0xFF60771E.toInt(), 0xFFBDDC75.toInt()),
    YELLOW(0xFF8A6810.toInt(), 0xFFF0D47B.toInt()),
    ORANGE(0xFFAA570A.toInt(), 0xFFF5B675.toInt()),
    RED(0xFFB33B38.toInt(), 0xFFFF9991.toInt()),
    PINK(0xFFA23C70.toInt(), 0xFFF5A0C6.toInt()),
    BROWN(0xFF79573B.toInt(), 0xFFCCA98B.toInt());

    fun argb(darkTheme: Boolean): Int = if (darkTheme) darkArgb else lightArgb

    companion object {
        fun fromStoredValue(value: String?): AccentColor =
            entries.firstOrNull { it.name == value } ?: PURPLE
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentColor: AccentColor = AccentColor.PURPLE,
    val runDistanceUnit: RunDistanceUnit = RunDistanceUnit.METRIC,
    val runOffRouteAlertsEnabled: Boolean = true,
    val runOffRouteNotificationsEnabled: Boolean = true,
    val runAutoPause: Boolean = false,
    val runKeepScreenAwake: Boolean = false,
    val runScreenDimTimeoutMs: Long = DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS,
    val runShowControlsOnLockScreen: Boolean = false,
    val runManualLapsEnabled: Boolean = false,
    val runStepRateSensorEnabled: Boolean = true,
    val runLiveSegmentsEnabled: Boolean = true,
    val runDisplayAllTurnCues: Boolean = false,
    val runUpcomingTurnCueCount: Int = 1,
    val runBodyMassKg: Double? = null,
    val runWeatherEnabled: Boolean = false,
    val runTemperatureUnit: RunTemperatureUnit = RunTemperatureUnit.CELSIUS,
    val runStartEndHideDistance: RunStartEndHideDistance = RunStartEndHideDistance.NONE
) {
    val runRouteNotificationsEnabled: Boolean
        get() = runOffRouteAlertsEnabled && runOffRouteNotificationsEnabled
}
