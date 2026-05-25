package com.example.gymtimer2.timer_overlay_service.components.float_timer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.databinding.FloatTimerBinding
import com.example.gymtimer2.domain.model.ExerciseWithChorusesModel
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.timer_overlay_service.components.remove_area.RemoveAreaController
import com.example.gymtimer2.ui.components.music.hasAudioPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

const val FLOAT_TIMER_WIDTH = 450
const val FLOAT_TIMER_HEIGHT = 220

class FloatTimerController {
    // Core Android dependencies
    private val context: Context
    private val inflater: LayoutInflater
    private val windowManager: WindowManager

    // Callbacks, dependencies, and controllers
    private val onClose: () -> Unit
    private val removeAreaController: RemoveAreaController
    private val playerManager: MusicPlayerManager
    private val repository: WorkoutRepository

    // UI and timer state
    private lateinit var workoutExercise: ExerciseWithChorusesModel
    private lateinit var binding: FloatTimerBinding
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isFloatTimerVisible = false
    private var countDownTimer: CountDownTimer? = null
    private var state: WorkoutState = WorkoutState.READY_TO_START

    // Coroutine scope for async data loading
    private val scope = CoroutineScope(Dispatchers.Main)

    constructor(
        context: Context,
        inflater: LayoutInflater,
        windowManager: WindowManager,
        exerciseId: Int,
        onClose: () -> Unit
    ){
        this.windowManager = windowManager
        this.inflater = inflater
        this.context = context
        this.onClose = onClose

        val app = context.applicationContext as GymApplication
        this.playerManager = app.musicPlayerManager
        this.repository = app.container.workoutRepository

        scope.launch {
            repository
                .getExerciseWithChorusesByExerciseId(exerciseId)
                .collect { value ->
                    workoutExercise = value
                }
        }

        this.removeAreaController = RemoveAreaController(
            context,
            inflater,
            windowManager
        )

        setBinding()
        setLayoutParams()
    }

    private fun setBinding() {
        binding = FloatTimerBinding.inflate(inflater)

        setOnClickListener()

        setOnTouchListener()
    }
    private fun setLayoutParams() {
        @Suppress("DEPRECATION")
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            FLOAT_TIMER_WIDTH,
            FLOAT_TIMER_HEIGHT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }
    }

    private fun updateState(newState: WorkoutState) {
        state = newState
        when (state) {
            WorkoutState.ON_SET -> {
                stopTimer()
                binding.timer.text = context.getString(R.string.float_timer_end_set)
                playChorus()
            }
            WorkoutState.READY_TO_START -> {
                stopTimer()
                binding.timer.text = context.getString(R.string.float_timer_start_set)
            }
            WorkoutState.RESTING -> {
                startTimer()
                playerManager.stop()
            }
        }
    }

    // Timer management
    private fun startTimer() {
        binding.timer.scaleX = 2.1f
        binding.timer.scaleY = 2.1f

        countDownTimer = object : CountDownTimer(workoutExercise.exercise.restPeriod, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = millisUntilFinished / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60

                binding.timer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                updateState(WorkoutState.READY_TO_START)
            }
        }.start()
    }
    private fun stopTimer() {
        countDownTimer?.cancel()
        binding.timer.scaleX = 1f
        binding.timer.scaleY = 1f
    }

    // Touch listener
    private fun setOnClickListener() {
        binding.root.setOnClickListener {
            // Stop set
            if (state == WorkoutState.ON_SET)
                updateState(WorkoutState.RESTING)
            // Start set
            else updateState(WorkoutState.ON_SET)
        }
    }
    private fun setOnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        binding.root.setOnTouchListener(object : View.OnTouchListener {
            var dX = 0f // X variation
            var dY = 0f // Y variation
            var accX = 0f // total X variation
            var accY = 0f // total Y variation
            var prevXTouch: Float = 0f
            var prevYTouch: Float = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                event ?: return false

                val root = binding.root
                val screenWidth = context.resources.displayMetrics.widthPixels
                val screenHeight = context.resources.displayMetrics.heightPixels

                val viewWidth = root.width
                val viewHeight = root.height

                val minX = -(screenWidth - viewWidth) / 2f
                val maxX = (screenWidth - viewWidth) / 2f
                val minY = -(screenHeight - viewHeight) / 2f
                val maxY = (screenHeight - viewHeight) / 2f

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        prevXTouch = event.rawX
                        prevYTouch = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        dX = event.rawX - prevXTouch
                        dY = event.rawY - prevYTouch

                        accX += abs(dX)
                        accY += abs(dY)

                        layoutParams.x = (layoutParams.x + dX).coerceIn(minX, maxX).toInt()
                        layoutParams.y = (layoutParams.y + dY).coerceIn(minY, maxY).toInt()

                        if (isFloatTimerVisible) {
                            windowManager.updateViewLayout(root, layoutParams)
                        }

                        if (removeAreaController.isVisible) {
                            val overRemove =
                                removeAreaController.isOverRemoveArea(
                                    event.rawX,
                                    event.rawY
                                )
                            removeAreaController.setHighlight(overRemove)
                        } else if (accX > 10 && accY > 10) {
                            removeAreaController.show()
                        }

                        prevXTouch = event.rawX
                        prevYTouch = event.rawY

                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val isClick = accX < 10f && accY < 10f
                        accX = 0f
                        accY = 0f

                        if (isClick) {
                            v?.performClick()
                        }

                        val overRemove =
                            removeAreaController.isVisible &&
                                removeAreaController.isOverRemoveArea(
                                    event.rawX,
                                    event.rawY
                                )

                        if (overRemove) {
                            onClose()
                        }

                        removeAreaController.hide()

                        return true
                    }
                }
                return false
            }
        })
    }

    // Song chorus
    private fun playChorus() {
        val randomChorus = workoutExercise.choruses.randomOrNull()

        randomChorus?.let {
            if (hasAudioPermission(context)) {
                runCatching {
                    playerManager.delayedPlay(
                        randomChorus.song.uriString,
                        randomChorus.chorus.startMs.toInt(),
                        workoutExercise.exercise.chorusDelay.toInt()
                    )
                }
            }
        }
    }

    // Visibility
    fun show() {
        if (!isFloatTimerVisible) {
            windowManager.addView(binding.root, layoutParams)
            binding.timer.text = context.getString(R.string.float_timer_start_set)
            isFloatTimerVisible = true
        }
    }
    fun hide() {
        if (isFloatTimerVisible) {
            runCatching {
                windowManager.removeView(binding.root)
            }
            stopTimer()
            isFloatTimerVisible = false
            playerManager.stop()
        }
    }
}