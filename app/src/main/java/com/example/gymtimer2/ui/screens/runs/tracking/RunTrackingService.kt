@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.tracking

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.gymtimer2.MainActivity
import com.example.gymtimer2.ui.screens.runs.notifications.RUN_NOTIFICATION_CHANNEL_ID
import com.example.gymtimer2.R
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.GymApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Grava posições GPS enquanto a corrida está ativa, mesmo com a tela bloqueada. */
class RunTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commandMutex = Mutex()
    private var isEnding = false
    private val locationChannel = Channel<Location>(Channel.UNLIMITED)
    private lateinit var locationManager: LocationManager
    private lateinit var runRepository: RunRepository
    private var locationListener: LocationListener? = null
    private var locationJob: Job? = null
    private var durationJob: Job? = null
    @Volatile
    private var runId: Long? = null
    private var accumulatedDurationMs = 0L
    private var segmentStartedAtElapsedMs = 0L
    private var previousAcceptedPoint: GpsPoint? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        runRepository = (application as GymApplication).container.runRepository
        createNotificationChannel()
        locationJob = serviceScope.launch(Dispatchers.IO) {
            for (location in locationChannel) processLocation(location)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isEnding) return START_NOT_STICKY
        val command = intent?.action ?: return START_NOT_STICKY
        val requestedRunId = intent.getLongExtra(EXTRA_RUN_ID, INVALID_RUN_ID)
        if (requestedRunId == INVALID_RUN_ID) return START_NOT_STICKY

        if (command == ACTION_START || command == ACTION_RESUME) {
            this.runId = requestedRunId
            RunTrackingState.activeRunId = requestedRunId
            startForegroundNotification()
        }

        serviceScope.launch {
            commandMutex.withLock {
                if (isEnding) return@withLock
                executeCommand(command, requestedRunId, intent.getStringExtra(EXTRA_TITLE))
            }
        }
        return START_NOT_STICKY
    }

    /** Serializa as transições para impedir retomadas ou pausas durante o encerramento. */
    private suspend fun executeCommand(command: String, id: Long, title: String?) {
        when (command) {
            ACTION_START -> startTracking(id)
            ACTION_RESUME -> resumeTracking(id)
            ACTION_PAUSE -> pauseTracking(id)
            ACTION_FINISH -> finishTracking(id, title)
            ACTION_DISCARD -> discardTracking(id)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeLocationUpdates()
        locationChannel.close()
        runId?.let { if (RunTrackingState.activeRunId == it) RunTrackingState.activeRunId = null }
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Abre uma sessão nova e inicia seu primeiro trecho GPS. */
    private suspend fun startTracking(id: Long) {
        accumulatedDurationMs = 0L
        runRepository.beginRun(id)
        beginLocationSegment()
    }

    /** Continua uma sessão salva e cria um novo trecho GPS. */
    private suspend fun resumeTracking(id: Long) {
        val run = runRepository.getRun(id) ?: return stopService()
        accumulatedDurationMs = run.durationMs
        runRepository.resumeRun(id)
        beginLocationSegment()
    }

    /** Confirma GPS e permissão antes de pedir novas posições. */
    private suspend fun beginLocationSegment() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            runId?.let { runRepository.markInterrupted(it) }
            return stopService()
        }
        if (!hasPreciseLocationPermission()) {
            runId?.let { runRepository.markInterrupted(it) }
            return stopService()
        }

        previousAcceptedPoint = null
        segmentStartedAtElapsedMs = SystemClock.elapsedRealtime()
        requestLocationUpdates()
        startDurationUpdates()
    }

    /** Recebe posições na thread principal e as envia para processamento sequencial. */
    private fun requestLocationUpdates() {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationChannel.trySend(location)
            }

            @Deprecated("Required by LocationListener on supported Android versions")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        locationListener = listener
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL_MS,
                MINIMUM_LOCATION_DISTANCE_METERS,
                listener,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            serviceScope.launch {
                runId?.let { runRepository.markInterrupted(it) }
                stopService()
            }
        }
    }

    /** Filtra uma leitura e grava pontos válidos fora da thread principal. */
    private suspend fun processLocation(location: Location) {
        val id = runId ?: return
        if (!location.hasAccuracy()) return
        val point = GpsPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            recordedAtEpochMs = location.time,
            accuracyMeters = location.accuracy
        )
        val distanceMeters = RunMetrics.acceptedDistanceMeters(previousAcceptedPoint, point) ?: return
        previousAcceptedPoint = point
        runRepository.appendLocation(id, point, distanceMeters)
    }

    /** Salva o tempo ativo a cada segundo para limitar perda em caso de parada. */
    private fun startDurationUpdates() {
        durationJob?.cancel()
        durationJob = serviceScope.launch {
            while (isActive) {
                delay(DURATION_UPDATE_INTERVAL_MS)
                val id = runId ?: return@launch
                runRepository.updateDuration(id, currentDurationMs())
            }
        }
    }

    /** Para GPS, grava o tempo final do trecho e mantém o serviço para retomada. */
    private suspend fun pauseTracking(id: Long) {
        if (runId != id) return
        accumulatedDurationMs = currentDurationMs()
        segmentStartedAtElapsedMs = 0L
        durationJob?.cancel()
        removeLocationUpdates()
        runRepository.pauseRun(id, accumulatedDurationMs)
        updateNotification(R.string.run_notification_paused)
    }

    /** Salva a duração final e encerra o serviço. */
    private suspend fun finishTracking(id: Long, title: String?) {
        if (runId != id) return
        isEnding = true
        val durationMs = currentDurationMs()
        stopPendingUpdates()
        runRepository.finishRun(id, durationMs, System.currentTimeMillis(), title)
        stopService()
    }

    /** Descarta somente depois de impedir novas gravações e aguardar as operações em andamento. */
    private suspend fun discardTracking(id: Long) {
        if (runId != id) return
        isEnding = true
        stopPendingUpdates()
        runRepository.discardUnfinishedRun(id)
        stopService()
    }

    /** Aguarda o término dos produtores para que nenhum ponto seja inserido após a exclusão. */
    private suspend fun stopPendingUpdates() {
        removeLocationUpdates()
        durationJob?.cancelAndJoin()
        locationChannel.close()
        locationJob?.cancelAndJoin()
    }

    /** Soma o tempo desde o início do trecho ao tempo já acumulado. */
    private fun currentDurationMs(): Long {
        if (segmentStartedAtElapsedMs == 0L) return accumulatedDurationMs
        val currentSegmentMs = (SystemClock.elapsedRealtime() - segmentStartedAtElapsedMs).coerceAtLeast(0L)
        return accumulatedDurationMs + currentSegmentMs
    }

    /** Remove o listener para evitar atualizações depois da pausa ou fim. */
    private fun removeLocationUpdates() {
        locationListener?.let(locationManager::removeUpdates)
        locationListener = null
    }

    /** Retira a notificação e encerra o serviço. */
    private fun stopService() {
        durationJob?.cancel()
        removeLocationUpdates()
        RunTrackingState.activeRunId = null
        runId = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Publica o serviço com o tipo location exigido pelo Android. */
    private fun startForegroundNotification() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(R.string.run_notification_running),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    /** Atualiza o texto da notificação sem recriar o serviço. */
    private fun updateNotification(messageId: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(messageId))
    }

    /** Exibe o aviso imediatamente e permite voltar ao app sem interromper a gravação. */
    private fun buildNotification(messageId: Int): Notification =
        NotificationCompat.Builder(this, RUN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.run_notification_title))
            .setContentText(getString(messageId))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    /** Cria o canal de notificação para a sessão de corrida. */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            RUN_NOTIFICATION_CHANNEL_ID,
            getString(R.string.run_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Confirma que o usuário concedeu localização precisa. */
    private fun hasPreciseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val ACTION_START = "com.example.gymtimer2.run.START"
        const val ACTION_RESUME = "com.example.gymtimer2.run.RESUME"
        const val ACTION_PAUSE = "com.example.gymtimer2.run.PAUSE"
        const val ACTION_FINISH = "com.example.gymtimer2.run.FINISH"
        const val ACTION_DISCARD = "com.example.gymtimer2.run.DISCARD"
        const val EXTRA_TITLE = "run_title"
        const val EXTRA_RUN_ID = "run_id"
        private const val INVALID_RUN_ID = -1L
        private const val NOTIFICATION_ID = 2301
        private const val LOCATION_INTERVAL_MS = 2_000L
        private const val DURATION_UPDATE_INTERVAL_MS = 1_000L
        private const val MINIMUM_LOCATION_DISTANCE_METERS = 0f

        fun isTracking(runId: Long): Boolean = RunTrackingState.activeRunId == runId
    }
}
