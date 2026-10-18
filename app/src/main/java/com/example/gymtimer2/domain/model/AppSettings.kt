package com.example.gymtimer2.domain.model

enum class ThemeMode { LIGHT, DARK }

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
    val accentColor: AccentColor = AccentColor.PURPLE
)
