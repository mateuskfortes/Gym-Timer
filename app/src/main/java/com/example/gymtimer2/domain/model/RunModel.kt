@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.domain.model

/** Estados que a sessão pode ter durante e depois da gravação. */
enum class RunStatus {
    STARTING,
    RUNNING,
    PAUSED,
    INTERRUPTED,
    COMPLETED
}

/** Dados salvos de uma corrida. O pace é calculado a partir do tempo e da distância. */
data class RunModel(
    /** Identificador local da sessão. */
    val id: Long,
    /** Instante em que a pessoa iniciou a corrida. */
    val startedAtEpochMs: Long,
    /** Instante de finalização, ou nulo enquanto a sessão estiver aberta. */
    val finishedAtEpochMs: Long?,
    /** Tempo ativo acumulado. Pausas não entram neste valor. */
    val durationMs: Long,
    /** Distância aceita pelo filtro GPS, em metros. */
    val distanceMeters: Double,
    /** Estado atual da sessão. */
    val status: RunStatus,
    /** Número do trecho atual. Um novo trecho começa após cada retomada. */
    val currentSegment: Int,
    /** Título opcional definido ao salvar a corrida. */
    val title: String? = null
) {
    /** Pace médio em segundos por quilômetro, ou nulo sem distância. */
    val averagePaceSecondsPerKm: Double?
        get() = RunMetrics.averagePaceSecondsPerKm(durationMs, distanceMeters)
}

/** Uma posição GPS salva para desenhar a rota. */
data class RunPointModel(
    /** Chave da coordenada salva. */
    val id: Long,
    /** Corrida proprietária deste ponto. */
    val runId: Long,
    /** Latitude em graus decimais. */
    val latitude: Double,
    /** Longitude em graus decimais. */
    val longitude: Double,
    /** Instante da leitura em milissegundos desde a época Unix. */
    val recordedAtEpochMs: Long,
    /** Precisão reportada pelo GPS em metros. */
    val accuracyMeters: Float,
    /** Trecho de rota associado a esta posição. */
    val segmentIndex: Int
)

/** Uma corrida e os pontos usados para desenhar sua rota. */
data class RunWithRoute(
    /** Métricas e estado da sessão. */
    val run: RunModel,
    /** Coordenadas ordenadas que formam a rota. */
    val points: List<RunPointModel>
)

/** Posição GPS em unidades do sistema Android. */
data class GpsPoint(
    /** Latitude em graus decimais. */
    val latitude: Double,
    /** Longitude em graus decimais. */
    val longitude: Double,
    /** Instante da leitura em milissegundos desde a época Unix. */
    val recordedAtEpochMs: Long,
    /** Precisão reportada pelo GPS em metros. */
    val accuracyMeters: Float
)

/** Cálculos puros usados pelo gravador e pela interface. */
object RunMetrics {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val MAX_ACCURACY_METERS = 25.0
    private const val MIN_DISTANCE_METERS = 2.0
    private const val MAX_SPEED_METERS_PER_SECOND = 15.0

    /** Retorna a distância se o ponto tiver precisão e velocidade válidas. */
    fun acceptedDistanceMeters(previous: GpsPoint?, current: GpsPoint): Double? {
        if (!current.hasValidAccuracy()) return null
        if (previous == null) return 0.0

        val elapsedMs = current.recordedAtEpochMs - previous.recordedAtEpochMs
        if (elapsedMs <= 0L) return null

        val distanceMeters = distanceMeters(previous, current)
        if (distanceMeters < MIN_DISTANCE_METERS) return null

        val speedMetersPerSecond = distanceMeters / (elapsedMs / 1_000.0)
        if (speedMetersPerSecond > MAX_SPEED_METERS_PER_SECOND) return null

        return distanceMeters
    }

    /** Calcula o pace médio. Milissegundos por metro equivalem a segundos por quilômetro. */
    fun averagePaceSecondsPerKm(durationMs: Long, distanceMeters: Double): Double? {
        if (durationMs <= 0L || distanceMeters <= 0.0) return null
        return durationMs / distanceMeters
    }

    /** Formata o tempo como mm:ss ou hh:mm:ss. */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /** Formata o pace e mostra um marcador quando ainda não há distância. */
    fun formatPace(secondsPerKm: Double?): String {
        if (secondsPerKm == null || !secondsPerKm.isFinite()) return "--:--/km"
        val totalSeconds = secondsPerKm.toLong().coerceAtLeast(0L)
        return "%d:%02d/km".format(totalSeconds / 60L, totalSeconds % 60L)
    }

    /** Descarta coordenadas inválidas e leituras com erro acima de 25 metros. */
    private fun GpsPoint.hasValidAccuracy(): Boolean =
        latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0 &&
            accuracyMeters.isFinite() && accuracyMeters >= 0f && accuracyMeters <= MAX_ACCURACY_METERS

    /** Calcula a distância geográfica entre duas coordenadas com Haversine. */
    private fun distanceMeters(first: GpsPoint, second: GpsPoint): Double {
        val latitudeDelta = Math.toRadians(second.latitude - first.latitude)
        val longitudeDelta = Math.toRadians(second.longitude - first.longitude)
        val firstLatitude = Math.toRadians(first.latitude)
        val secondLatitude = Math.toRadians(second.latitude)
        val haversine = kotlin.math.sin(latitudeDelta / 2).let { sinLatitude ->
            sinLatitude * sinLatitude +
                kotlin.math.cos(firstLatitude) * kotlin.math.cos(secondLatitude) *
                kotlin.math.sin(longitudeDelta / 2).let { sinLongitude -> sinLongitude * sinLongitude }
        }
        return 2 * EARTH_RADIUS_METERS * kotlin.math.asin(kotlin.math.sqrt(haversine.coerceIn(0.0, 1.0)))
    }
}
