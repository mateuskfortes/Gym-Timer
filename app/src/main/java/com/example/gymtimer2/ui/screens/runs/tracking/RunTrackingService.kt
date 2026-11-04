@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.gymtimer2.ui.screens.runs.notifications.RUN_NOTIFICATION_CHANNEL_ID
import com.example.gymtimer2.ui.screens.runs.notifications.RUN_ROUTE_ALERT_NOTIFICATION_CHANNEL_ID
import com.example.gymtimer2.R
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.data.repository.RunWeatherRepository
import com.example.gymtimer2.domain.model.RunGpsFilter
import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunGpsAutoResumeDetector
import com.example.gymtimer2.domain.model.RunElevationReading
import com.example.gymtimer2.domain.model.runElevationReadingOrNull
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.withCurrentRunCadence
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunMotionTransition
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.elapsedDurationAt
import com.example.gymtimer2.domain.model.runGpsStatus
import com.example.gymtimer2.GymApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/** Grava posições GPS enquanto a corrida está ativa, mesmo com a tela bloqueada. */
class RunTrackingService : Service() {
    private val notifications by lazy(LazyThreadSafetyMode.NONE) {
        val settings = (application as GymApplication).container.appSettingsRepository
        RunTrackingNotifications(
            context = this,
            distanceUnit = { settings.settings.value.runDistanceUnit },
            currentRunId = { runId },
            currentRun = { notificationRun },
            currentDurationMs = ::currentDurationMs
        )
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commandMutex = Mutex()
    private var isEnding = false
    private lateinit var locationQueue: LocationQueue<RunLocationUpdate>
    private lateinit var locationUpdates: RunLocationUpdatesController
    private lateinit var runRepository: RunRepository
    private lateinit var runWeatherRepository: RunWeatherRepository
    // shortcut: one request per run; add retry handling before promising weather availability.
    private var weatherRequestedRunId: Long? = null
    private var durationJob: Job? = null
    @Volatile
    private var runId: Long? = null
    private var accumulatedDurationMs = 0L
    private var accumulatedElapsedDurationMs = 0L
    private var elapsedDurationAnchorElapsedMs = 0L
    private var notificationRun: RunModel? = null
    private var hasWorkout = false
    private var segmentStartedAtElapsedMs = 0L
    private val runGpsFilter = RunGpsFilter()
    private val gpsAutoResumeDetector = RunGpsAutoResumeDetector()
    private var lastAcceptedGpsPoint: GpsPoint? = null
    private var isGpsAutoResumeMonitoring = false
    private val routeCueController by lazy(LazyThreadSafetyMode.NONE) {
        val container = (application as GymApplication).container
        RunRouteCueController(
            runRepository = runRepository,
            plannedRouteRepository = container.plannedRouteRepository,
            settings = { container.appSettingsRepository.settings.value },
            onMuteStateChanged = RunTrackingState::updateRouteAlertMute,
            onRouteAlertNotification = notifications::showRouteAlert,
            onRouteAlertNotificationCleared = notifications::clearRouteAlert
        )
    }
    private val trackingSensors by lazy(LazyThreadSafetyMode.NONE) {
        RunTrackingSensors(
            context = this,
            scope = serviceScope,
            currentRunId = { runId },
            onMotionTransition = { transition, isCurrentGeneration ->
                commandMutex.withLock {
                    val settings = (application as GymApplication).container.appSettingsRepository.settings.value
                    if (!isCurrentGeneration()) return@withLock
                    if (isEnding) return@withLock
                    if (!settings.runAutoPause) return@withLock
                    if (notificationRun?.activityType?.usesAccelerometerAutoPause != true) return@withLock
                    val id = runId ?: return@withLock
                    when (transition) {
                        RunMotionTransition.PAUSE -> if (!autoPaused) autoPauseTracking(id)
                        RunMotionTransition.RESUME -> if (autoPaused) resumeTracking(id, motionStartsMoving = true)
                    }
                }
            },
            onStepRateSample = { reading, sample ->
                runRepository.appendStepRateSample(reading.runId, sample)
                RunTrackingState.updateStepRate(reading)
            },
            onStepRateCleared = { RunTrackingState.updateStepRate(null) }
        )
    }
    private var autoPaused = false

    override fun onCreate() {
        super.onCreate()
        val container = (application as GymApplication).container
        runRepository = container.runRepository
        runWeatherRepository = container.runWeatherRepository
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(
                    NotificationChannel(
                        RUN_NOTIFICATION_CHANNEL_ID,
                        getString(R.string.run_notification_channel),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
                createNotificationChannel(
                    NotificationChannel(
                        RUN_ROUTE_ALERT_NOTIFICATION_CHANNEL_ID,
                        getString(R.string.run_route_alert_notification_channel),
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = getString(R.string.run_route_alert_notification_channel_description)
                    }
                )
            }
        }
        locationQueue = LocationQueue(
            scope = serviceScope,
            onFailure = { error -> Log.e(TAG, "Failed to process a GPS update.", error) }
        ) { update ->
            if (update.runId == runId && locationUpdates.isCurrentGeneration(update.generation)) {
                processLocation(update)
            }
        }
        locationUpdates = RunLocationUpdatesController(
            locationManager = getSystemService(LocationManager::class.java),
            onGpsReading = RunTrackingState::updateGps,
            onLocationUpdate = locationQueue::offer,
            onGpsSignalRestored = notifications::onGpsSignalRestored,
            onPermissionFailure = {
                serviceScope.launch {
                    runId?.let { runRepository.markInterrupted(it) }
                    stopService()
                }
            }
        )
        RunTrackingSettingsObserver(
            scope = serviceScope,
            settings = container.appSettingsRepository.settings,
            onStepRateChanged = ::applyStepRatePreference,
            onAutoPauseChanged = ::applyAutoPausePreference
        ).start()
    }

    private fun applyStepRatePreference(enabled: Boolean) {
        if (segmentStartedAtElapsedMs == 0L) return
        if (enabled) trackingSensors.startStepRate(preferenceEnabled = true)
        else trackingSensors.stopStepRate()
    }

    private suspend fun applyAutoPausePreference(enabled: Boolean) {
        commandMutex.withLock {
            val id = runId ?: return@withLock
            val autoPauseEnabled = enabled && notificationRun?.activityType?.usesAccelerometerAutoPause == true
            if (autoPauseEnabled && segmentStartedAtElapsedMs != 0L) {
                trackingSensors.startMotion()
            } else if (!autoPauseEnabled) {
                trackingSensors.stopMotion()
                if (autoPaused) resumeTracking(id)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isEnding) return START_NOT_STICKY
        if (intent != null) return handleCommandIntent(intent, startId)
        if (!RunCrashRecovery.hasBackgroundLocationPermission(this)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this,
            RUN_NOTIFICATION_ID,
            notifications.build(R.string.run_notification_running, null, null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        serviceScope.launch {
            commandMutex.withLock { resumeTracking(null, isCrashRecovery = true) }
        }
        return START_STICKY
    }

    private fun handleCommandIntent(intent: Intent, startId: Int): Int {
        val command = RunTrackingServiceCommand.from(intent) ?: return START_NOT_STICKY
        if (command.isRecovery && !RunCrashRecovery.hasBackgroundLocationPermission(this)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        when (command.action) {
            ACTION_START, ACTION_RESUME, ACTION_RECOVER -> RunTrackingState.crashRecoverableRunId = command.runId
            ACTION_PAUSE, ACTION_FINISH, ACTION_DISCARD -> {
                if (RunTrackingState.crashRecoverableRunId == command.runId) {
                    RunTrackingState.crashRecoverableRunId = null
                }
            }
        }

        if (command.startsForeground) {
            runId = command.runId
            if (command.action == ACTION_START || command.isRecovery) notificationRun = null
            RunTrackingState.activeRunId = command.runId
            ServiceCompat.startForeground(
                this,
                RUN_NOTIFICATION_ID,
                notifications.build(
                    R.string.run_notification_running,
                    command.runId,
                    notificationRun
                ),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        }

        serviceScope.launch {
            commandMutex.withLock {
                if (isEnding) return@withLock
                executeCommand(
                    command.action,
                    command.runId,
                    command.saveDetails
                )
            }
        }
        return command.startMode
    }

    /** Serializa as transições para impedir retomadas ou pausas durante o encerramento. */
    private suspend fun executeCommand(command: String, id: Long, saveDetails: RunSaveDetails) {
        when (command) {
            ACTION_START -> startTracking(id)
            ACTION_RESUME -> resumeTracking(id)
            ACTION_RECOVER -> resumeTracking(id, isCrashRecovery = true)
            ACTION_PAUSE -> pauseTracking(id)
            ACTION_LAP -> recordLap(id)
            ACTION_ADVANCE_WORKOUT -> advanceWorkout(id, currentDurationMs(), manualAdvance = true)
            ACTION_MUTE_ROUTE_ALERTS_ONCE -> routeCueController.muteAlertsOnce(id)
            ACTION_MUTE_ROUTE_ALERTS_FOR_RUN -> routeCueController.muteAlertsForEntireRun(id)
            ACTION_FINISH -> finishTracking(id, saveDetails)
            ACTION_DISCARD -> discardTracking(id)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        trackingSensors.stopMotion()
        trackingSensors.stopStepRate()
        notifications.clearRouteAlert()
        removeLocationUpdates()
        runId?.let { if (RunTrackingState.activeRunId == it) RunTrackingState.activeRunId = null }
        runId?.let { if (RunTrackingState.crashRecoverableRunId == it) RunTrackingState.crashRecoverableRunId = null }
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Abre uma sessão nova e inicia seu primeiro trecho GPS. */
    private suspend fun startTracking(id: Long) {
        RunTrackingState.crashRecoverableRunId = id
        weatherRequestedRunId = null
        accumulatedDurationMs = 0L
        accumulatedElapsedDurationMs = 0L
        elapsedDurationAnchorElapsedMs = SystemClock.elapsedRealtime()
        val epochAnchorMs = System.currentTimeMillis()
        lastAcceptedGpsPoint = null
        gpsAutoResumeDetector.reset(null)
        notificationRun = runRepository.getRun(id)
        hasWorkout = notificationRun?.workoutTitle != null
        routeCueController.resetForNewRun(id)
        runRepository.beginRun(id, 0L, elapsedDurationAnchorElapsedMs, epochAnchorMs)
        beginLocationSegment()
        notifications.update(R.string.run_notification_running)
    }

    /** Continua uma sessão salva e cria um novo trecho GPS. */
    private suspend fun resumeTracking(
        requestedRunId: Long?,
        motionStartsMoving: Boolean = false,
        isCrashRecovery: Boolean = false
    ) {
        val initialRun = if (requestedRunId == null) {
            runRepository.activeRun.first()
        } else {
            runRepository.getRun(requestedRunId)
        }
        if (initialRun == null) return stopService()
        var run = initialRun
        val id = run.id

        if (isCrashRecovery) {
            this.runId = run.id
            notificationRun = run
            RunTrackingState.activeRunId = run.id
            run = prepareCrashRecovery(run) ?: return
        }
        if (run.status !in listOf(RunStatus.PAUSED, RunStatus.AUTO_PAUSED, RunStatus.INTERRUPTED)) return
        notificationRun = run
        RunTrackingState.crashRecoverableRunId = id
        if (autoPaused) {
            isGpsAutoResumeMonitoring = false
            gpsAutoResumeDetector.reset(null)
            removeLocationUpdates()
            locationQueue.flush()
            locationUpdates.invalidate()
        }
        hasWorkout = run.workoutTitle != null
        autoPaused = false
        trackingSensors.stopMotion()
        accumulatedDurationMs = run.durationMs
        routeCueController.resumeRun(id)
        val elapsedAnchorMs = SystemClock.elapsedRealtime()
        val epochAnchorMs = System.currentTimeMillis()
        accumulatedElapsedDurationMs = run.elapsedDurationAt(elapsedAnchorMs, epochAnchorMs)
        elapsedDurationAnchorElapsedMs = elapsedAnchorMs
        runRepository.resumeRun(id, accumulatedElapsedDurationMs, elapsedAnchorMs, epochAnchorMs)
        beginLocationSegment(motionStartsMoving)
        if (segmentStartedAtElapsedMs != 0L) {
            // A retomada pelo sensor não passa por onStartCommand.
            notifications.update(R.string.run_notification_running)
        }
    }

    /** Valida o estado persistido antes de retomar uma sessão sem interação da pessoa. */
    private suspend fun prepareCrashRecovery(run: RunModel): RunModel? {
        if (!run.status.canRecoverAfterProcessDeath) {
            stopService()
            return null
        }
        if (!RunCrashRecovery.hasBackgroundLocationPermission(this)) {
            runRepository.markInterrupted(run.id)
            stopService()
            return null
        }
        if (run.status == RunStatus.STARTING) {
            startTracking(run.id)
            return null
        }

        if (run.status != RunStatus.INTERRUPTED) runRepository.markInterrupted(run.id)
        return runRepository.getRun(run.id)
    }

    /** Confirma GPS e permissão antes de pedir novas posições. */
    private suspend fun beginLocationSegment(motionStartsMoving: Boolean = false) {
        if (!locationUpdates.isGpsProviderEnabled()) {
            runId?.let { runRepository.markInterrupted(it) }
            return stopService()
        }
        if (!hasPreciseLocationPermission()) {
            runId?.let { runRepository.markInterrupted(it) }
            return stopService()
        }

        runGpsFilter.reset()
        lastAcceptedGpsPoint = null
        gpsAutoResumeDetector.reset(null)
        isGpsAutoResumeMonitoring = false
        segmentStartedAtElapsedMs = SystemClock.elapsedRealtime()
        startLocationUpdates()
        val stepRateEnabled = (application as GymApplication)
            .container.appSettingsRepository.settings.value.runStepRateSensorEnabled
        trackingSensors.startStepRate(preferenceEnabled = stepRateEnabled)
        startDurationUpdates()
        val autoPauseEnabled = (application as GymApplication)
            .container.appSettingsRepository.settings.value.runAutoPause
        if (autoPauseEnabled && notificationRun?.activityType?.usesAccelerometerAutoPause == true) {
            trackingSensors.startMotion(motionStartsMoving)
        }
    }

    private fun startLocationUpdates() {
        val id = runId ?: return
        notifications.resetNoGpsState()
        locationUpdates.start(id, ::activeDurationAt)
    }

    /** Filtra uma leitura e grava pontos válidos fora da thread principal. */
    private suspend fun processLocation(update: RunLocationUpdate) {
        val id = update.runId
        val point = update.location.toGpsPoint(SystemClock.elapsedRealtime()) ?: return
        if (autoPaused) {
            checkGpsAutoResume(id, point)
            return
        }

        processActiveLocation(id, point, update.activeDurationMs)
    }

    /** Atualiza rota, métricas e avisos apenas durante um trecho ativo. */
    private suspend fun processActiveLocation(id: Long, point: GpsPoint, activeDurationMs: Long) {
        val pointWithCadence = point.withCurrentRunCadence(
            trackingSensors.stepRateReading,
            id,
            SystemClock.elapsedRealtime()
        )
        val distanceMeters = runGpsFilter.filter(pointWithCadence).distanceMeters
        runRepository.appendLocation(
            id,
            pointWithCadence.copy(isFiltered = distanceMeters == null),
            distanceMeters ?: 0.0,
            activeDurationMs
        )
        if (distanceMeters == null) return
        lastAcceptedGpsPoint = pointWithCadence
        requestWeatherIfEnabled(id, pointWithCadence.latitude, pointWithCadence.longitude)
        val run = runRepository.getRun(id) ?: return
        notificationRun = run
        notifications.update(R.string.run_notification_running)
        if (hasWorkout) {
            advanceWorkout(
                id,
                currentDurationMs(),
                elevationReading = runElevationReadingOrNull(point.altitudeMeters, point.verticalAccuracyMeters)
            )
        }
        if (!isEnding) routeCueController.update(id, run, point, currentDurationMs())
    }

    /** Busca um único snapshot opcional sem atrasar a gravação GPS. */
    private fun requestWeatherIfEnabled(runId: Long, latitude: Double, longitude: Double) {
        val container = (application as GymApplication).container
        if (!container.appSettingsRepository.settings.value.runWeatherEnabled || weatherRequestedRunId == runId) return
        weatherRequestedRunId = runId
        serviceScope.launch {
            val weather = runWeatherRepository.getCurrentWeather(latitude, longitude) ?: return@launch
            runRepository.updateWeather(runId, weather)
        }
    }

    /** Agenda a retomada somente depois que o GPS confirmar movimento durante a pausa automática. */
    private fun checkGpsAutoResume(id: Long, point: GpsPoint) {
        if (!isGpsAutoResumeMonitoring || !gpsAutoResumeDetector.shouldResume(point)) return
        serviceScope.launch {
            commandMutex.withLock {
                if (runId != id || !autoPaused) return@withLock
                if (!isGpsAutoResumeMonitoring || isEnding) return@withLock
                resumeTracking(id, motionStartsMoving = true)
            }
        }
    }

    /** Salva o tempo ativo a cada segundo para limitar perda em caso de parada. */
    private fun startDurationUpdates() {
        durationJob?.cancel()
        durationJob = serviceScope.launch {
            while (isActive) {
                delay(DURATION_UPDATE_INTERVAL_MS)
                val id = runId ?: return@launch
                val durationMs = currentDurationMs()
                val elapsedAnchorMs = SystemClock.elapsedRealtime()
                runRepository.updateDuration(
                    id,
                    durationMs,
                    elapsedDurationSinceAnchor(
                        accumulatedElapsedDurationMs,
                        elapsedDurationAnchorElapsedMs,
                        elapsedAnchorMs
                    ),
                    elapsedAnchorMs,
                    System.currentTimeMillis()
                )
                if (hasWorkout) {
                    advanceWorkout(id, durationMs)
                }
                val gpsStatus = runGpsStatus(
                    RunTrackingState.gpsReading.value,
                    id,
                    elapsedAnchorMs
                )
                notifications.updateGpsStatus(gpsStatus)
            }
        }
    }

    /** Para GPS, grava o tempo final do trecho e mantém o serviço para retomada. */
    private suspend fun pauseTracking(id: Long) {
        if (runId != id) return
        RunTrackingState.crashRecoverableRunId = null
        isGpsAutoResumeMonitoring = false
        gpsAutoResumeDetector.reset(null)
        trackingSensors.stopMotion()
        trackingSensors.stopStepRate()
        val elapsedAnchorMs = SystemClock.elapsedRealtime()
        val epochAnchorMs = System.currentTimeMillis()
        accumulatedDurationMs = currentDurationMs()
        val elapsedDurationMs = elapsedDurationSinceAnchor(
            accumulatedElapsedDurationMs,
            elapsedDurationAnchorElapsedMs,
            elapsedAnchorMs
        )
        segmentStartedAtElapsedMs = 0L
        durationJob?.cancel()
        removeLocationUpdates()
        locationQueue.flush()
        locationUpdates.invalidate()
        autoPaused = false
        lastAcceptedGpsPoint = null
        advanceWorkout(id, accumulatedDurationMs)
        runRepository.pauseRun(id, accumulatedDurationMs, elapsedDurationMs, elapsedAnchorMs, epochAnchorMs)
        notifications.update(R.string.run_notification_paused)
    }

    private suspend fun autoPauseTracking(id: Long) {
        if (runId != id || segmentStartedAtElapsedMs == 0L) return
        trackingSensors.stopStepRate()
        val elapsedAnchorMs = SystemClock.elapsedRealtime()
        val epochAnchorMs = System.currentTimeMillis()
        accumulatedDurationMs = currentDurationMs()
        val elapsedDurationMs = elapsedDurationSinceAnchor(
            accumulatedElapsedDurationMs,
            elapsedDurationAnchorElapsedMs,
            elapsedAnchorMs
        )
        segmentStartedAtElapsedMs = 0L
        durationJob?.cancel()
        removeLocationUpdates()
        locationQueue.flush()
        locationUpdates.invalidate()
        autoPaused = true
        isGpsAutoResumeMonitoring = false
        advanceWorkout(id, accumulatedDurationMs)
        runRepository.autoPauseRun(id, accumulatedDurationMs, elapsedDurationMs, elapsedAnchorMs, epochAnchorMs)
        notifications.update(R.string.run_notification_auto_paused)
        gpsAutoResumeDetector.reset(lastAcceptedGpsPoint)
        isGpsAutoResumeMonitoring = true
        startLocationUpdates()
    }

    private suspend fun recordLap(id: Long) {
        if (runId != id || segmentStartedAtElapsedMs == 0L) return
        removeLocationUpdates()
        try {
            locationQueue.flush()
            val elapsedAnchorMs = SystemClock.elapsedRealtime()
            runRepository.recordLap(
                runId = id,
                durationMs = currentDurationMs(),
                elapsedDurationMs = elapsedDurationSinceAnchor(
                    accumulatedElapsedDurationMs,
                    elapsedDurationAnchorElapsedMs,
                    elapsedAnchorMs
                ),
                recordedAtEpochMs = System.currentTimeMillis()
            )
        } finally {
            startLocationUpdates()
        }
    }

    private suspend fun advanceWorkout(
        id: Long,
        durationMs: Long,
        manualAdvance: Boolean = false,
        elevationReading: RunElevationReading? = null
    ) {
        if (!hasWorkout) return
        if (manualAdvance && (runId != id || segmentStartedAtElapsedMs == 0L)) return
        val run = runRepository.getRun(id) ?: return
        runRepository.advanceWorkoutProgress(id, durationMs, run.distanceMeters, manualAdvance, elevationReading)
    }

    /** Salva a duração final e encerra o serviço. */
    private suspend fun finishTracking(id: Long, saveDetails: RunSaveDetails) {
        if (runId != id) return
        isEnding = true
        stopPendingUpdates(saveLocations = true)
        val elapsedAnchorMs = SystemClock.elapsedRealtime()
        val epochAnchorMs = System.currentTimeMillis()
        val durationMs = currentDurationMs()
        val elapsedDurationMs = elapsedDurationSinceAnchor(
            accumulatedElapsedDurationMs,
            elapsedDurationAnchorElapsedMs,
            elapsedAnchorMs
        )
        advanceWorkout(id, durationMs)
        runRepository.finishRun(
            id,
            durationMs,
            epochAnchorMs,
            saveDetails,
            elapsedDurationMs,
            elapsedAnchorMs,
            epochAnchorMs
        )
        stopService()
    }

    /** Descarta somente depois de impedir novas gravações e aguardar as operações em andamento. */
    private suspend fun discardTracking(id: Long) {
        if (runId != id) return
        isEnding = true
        stopPendingUpdates(saveLocations = false)
        runRepository.discardUnfinishedRun(id)
        stopService()
    }

    /** Aguarda o término dos produtores para que nenhum ponto seja inserido após a exclusão. */
    private suspend fun stopPendingUpdates(saveLocations: Boolean) {
        isGpsAutoResumeMonitoring = false
        gpsAutoResumeDetector.reset(null)
        trackingSensors.stopMotion()
        trackingSensors.stopStepRate()
        removeLocationUpdates()
        durationJob?.cancelAndJoin()
        if (saveLocations) locationQueue.close() else locationQueue.cancel()
        locationUpdates.invalidate()
    }

    /** Soma o tempo desde o início do trecho ao tempo já acumulado. */
    private fun currentDurationMs(): Long {
        if (segmentStartedAtElapsedMs == 0L) return accumulatedDurationMs
        val currentSegmentMs = (SystemClock.elapsedRealtime() - segmentStartedAtElapsedMs).coerceAtLeast(0L)
        return accumulatedDurationMs + currentSegmentMs
    }

    /** Usa o instante da amostra para manter os cortes alinhados ao cronômetro ativo. */
    private fun activeDurationAt(pointElapsedRealtimeMs: Long): Long {
        if (segmentStartedAtElapsedMs == 0L) return accumulatedDurationMs
        return accumulatedDurationMs + (pointElapsedRealtimeMs - segmentStartedAtElapsedMs).coerceAtLeast(0L)
    }

    /** Remove o listener para evitar atualizações depois da pausa ou fim. */
    private fun removeLocationUpdates() {
        locationUpdates.stop()
    }

    /** Retira a notificação e encerra o serviço. */
    private fun stopService() {
        trackingSensors.stopMotion()
        trackingSensors.stopStepRate()
        notifications.clearRouteAlert()
        durationJob?.cancel()
        removeLocationUpdates()
        RunTrackingState.activeRunId = null
        RunTrackingState.crashRecoverableRunId = null
        runId = null
        notificationRun = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "RunTrackingService"
        const val ACTION_START = "com.example.gymtimer2.run.START"
        const val ACTION_RESUME = "com.example.gymtimer2.run.RESUME"
        const val ACTION_RECOVER = "com.example.gymtimer2.run.RECOVER"
        const val ACTION_PAUSE = "com.example.gymtimer2.run.PAUSE"
        const val ACTION_LAP = "com.example.gymtimer2.run.LAP"
        const val ACTION_ADVANCE_WORKOUT = "com.example.gymtimer2.run.ADVANCE_WORKOUT"
        const val ACTION_MUTE_ROUTE_ALERTS_ONCE = "com.example.gymtimer2.run.MUTE_ROUTE_ALERTS_ONCE"
        const val ACTION_MUTE_ROUTE_ALERTS_FOR_RUN = "com.example.gymtimer2.run.MUTE_ROUTE_ALERTS_FOR_RUN"
        const val ACTION_FINISH = "com.example.gymtimer2.run.FINISH"
        const val ACTION_DISCARD = "com.example.gymtimer2.run.DISCARD"
        const val EXTRA_TITLE = "run_title"
        const val EXTRA_DESCRIPTION = "run_description"
        const val EXTRA_GEAR_NAME = "run_gear_name"
        const val EXTRA_GEAR_ID = "run_gear_id"
        const val EXTRA_PRIVATE_NOTES = "run_private_notes"
        const val EXTRA_IS_COMMUTE = "run_is_commute"
        const val EXTRA_PERCEIVED_EXERTION = "run_perceived_exertion"
        const val EXTRA_ACTIVITY_TYPE = "run_activity_type"
        const val EXTRA_ACTIVITY_TAGS = "run_activity_tags"
        const val EXTRA_RUN_ID = "run_id"
        private const val INVALID_RUN_ID = -1L
        private const val INVALID_GEAR_ID = -1L
        private const val DURATION_UPDATE_INTERVAL_MS = 1_000L

        fun isTracking(runId: Long): Boolean = RunTrackingState.activeRunId == runId
    }
}

private fun RunTrackingService.hasPreciseLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun elapsedDurationSinceAnchor(
    accumulatedDurationMs: Long,
    anchorElapsedRealtimeMs: Long,
    nowElapsedRealtimeMs: Long
): Long = accumulatedDurationMs + (nowElapsedRealtimeMs - anchorElapsedRealtimeMs).coerceAtLeast(0L)
