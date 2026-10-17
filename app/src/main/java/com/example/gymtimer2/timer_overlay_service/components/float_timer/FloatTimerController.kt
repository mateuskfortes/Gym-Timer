package com.example.gymtimer2.timer_overlay_service.components.float_timer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.databinding.FloatTimerBinding
import com.example.gymtimer2.domain.model.ExerciseWithChorusesModel
import com.example.gymtimer2.domain.model.WorkoutScheduleError
import com.example.gymtimer2.domain.model.WorkoutScheduleException
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.timer_overlay_service.components.remove_area.RemoveAreaController
import com.example.gymtimer2.timer_overlay_service.overlayWindowType
import com.example.gymtimer2.util.hasAudioPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private const val FLOAT_TIMER_WIDTH_DP = 280
private const val FLOAT_TIMER_SCREEN_MARGIN_DP = 32
private const val ACTION_TEXT_SIZE_SP = 13f
private const val COUNTDOWN_TEXT_SIZE_SP = 26f

private fun overlayWidthPx(context: Context): Int {
    val density = context.resources.displayMetrics.density
    val preferredWidth = (FLOAT_TIMER_WIDTH_DP * density).toInt()
    val availableWidth = (
        context.resources.displayMetrics.widthPixels - FLOAT_TIMER_SCREEN_MARGIN_DP * density
    ).toInt().coerceAtLeast(1)
    return minOf(preferredWidth, availableWidth)
}

class FloatTimerController(
    private val context: Context,
    inflater: LayoutInflater,
    private val windowManager: WindowManager,
    initialExerciseId: Int,
    private val onClose: () -> Unit
) {
    private val app = context.applicationContext as GymApplication
    private val repository: WorkoutRepository = app.container.workoutRepository
    private val playerManager: MusicPlayerManager = app.musicPlayerManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val binding = FloatTimerBinding.inflate(inflater)
    private val removeAreaController = RemoveAreaController(context, inflater, windowManager)
    private val finishDialog = FinishSetDialogController(context, inflater, windowManager, ::onFinishSelection)

    private val layoutParams = WindowManager.LayoutParams(
        overlayWidthPx(context),
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
        x = 0
        y = 0
    }

    private var isVisible = false
    private var timer: CountDownTimer? = null
    private var state = WorkoutState.READY_TO_START
    private var workoutExercise: ExerciseWithChorusesModel? = null
    private var sequence = WorkoutSequence(emptyList())

    init {
        setOnClickListener()
        setOnTouchListener()
        scope.launch {
            try {
                sequence = WorkoutSequence(repository.orderedExerciseIdsFrom(initialExerciseId))
                if (sequence.currentId == null || !loadExercise(initialExerciseId)) {
                    onClose()
                } else {
                    readyForSet()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onClose()
            }
        }
    }

    private suspend fun loadExercise(exerciseId: Int): Boolean {
        val loaded = repository.getExerciseWithChorusesByExerciseId(exerciseId).first() ?: return false
        workoutExercise = loaded
        binding.exerciseName.text = loaded.exercise.name
        return true
    }

    private fun startSet() {
        if (workoutExercise == null) return
        stopTimer()
        state = WorkoutState.ON_SET
        setCompactRestLayout(false)
        binding.root.setBackgroundResource(R.drawable.rounded_timer_background)
        binding.timer.text = context.getString(R.string.float_timer_end_set)
        playChorus()
    }

    private fun finishSet() {
        val exercise = workoutExercise?.exercise ?: return
        if (state != WorkoutState.ON_SET) return
        playerManager.stop()
        state = WorkoutState.FINISH_PROMPT
        runCatching {
            finishDialog.show(exercise, sequence.hasNext)
        }.onFailure { onClose() }
    }

    private fun onFinishSelection(action: FinishSetAction, newWeight: Int?) {
        val current = workoutExercise ?: return
        scope.launch {
            try {
                if (newWeight != null) {
                    repository.updateExerciseWeight(current.exercise.id, newWeight)
                    workoutExercise = current.copy(
                        exercise = current.exercise.copy(
                            weight = current.exercise.weight.copy(value = newWeight)
                        )
                    )
                }
                when (action) {
                    FinishSetAction.KEEP -> {
                        finishDialog.hide()
                        startRest()
                    }
                    FinishSetAction.NEXT_OR_FINISH -> advanceOrClose()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: WorkoutScheduleException) {
                val message = if (exception.reason == WorkoutScheduleError.INVALID_WEIGHT) {
                    R.string.invalid_weight
                } else {
                    R.string.exercise_not_found
                }
                finishDialog.showError(context.getString(message))
            } catch (_: Exception) {
                finishDialog.showError(context.getString(R.string.exercise_update_failed))
            }
        }
    }

    private suspend fun advanceOrClose() {
        while (true) {
            val nextId = sequence.nextId ?: break
            if (loadExercise(nextId)) {
                sequence.advance()
                finishDialog.hide()
                startRest()
                return
            }
            sequence.advance()
        }
        finishDialog.hide()
        onClose()
    }

    private fun startRest() {
        val restMs = workoutExercise?.exercise?.restPeriod ?: return
        stopTimer()
        playerManager.stop()
        if (restMs <= 0L) {
            readyForSet()
            return
        }
        state = WorkoutState.RESTING
        setCompactRestLayout(true)
        binding.root.setBackgroundResource(R.drawable.rounded_timer_rest_background)
        binding.timer.setTextSize(TypedValue.COMPLEX_UNIT_SP, COUNTDOWN_TEXT_SIZE_SP)
        binding.timer.text = formatRemaining(restMs)
        timer = object : CountDownTimer(restMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                binding.timer.text = formatRemaining(millisUntilFinished)
            }

            override fun onFinish() {
                readyForSet()
            }
        }.start()
    }

    private fun readyForSet() {
        stopTimer()
        state = WorkoutState.READY_TO_START
        setCompactRestLayout(false)
        binding.root.setBackgroundResource(R.drawable.rounded_timer_background)
        binding.timer.text = context.getString(R.string.float_timer_start_set)
    }

    private fun setCompactRestLayout(compact: Boolean) {
        binding.exerciseName.visibility = if (compact) View.GONE else View.VISIBLE
        val timerParams = binding.timer.layoutParams as LinearLayout.LayoutParams
        timerParams.topMargin = if (compact) 0 else (4 * context.resources.displayMetrics.density).toInt()
        binding.timer.layoutParams = timerParams

        layoutParams.width = if (compact) {
            WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            overlayWidthPx(context)
        }
        if (!compact) {
            val maxX = ((context.resources.displayMetrics.widthPixels - layoutParams.width) / 2)
                .coerceAtLeast(0)
            layoutParams.x = layoutParams.x.coerceIn(-maxX, maxX)
        }
        if (isVisible) windowManager.updateViewLayout(binding.root, layoutParams)
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
        binding.timer.setTextSize(TypedValue.COMPLEX_UNIT_SP, ACTION_TEXT_SIZE_SP)
    }

    private fun formatRemaining(millis: Long): String {
        val seconds = (millis + 999L) / 1000L
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60L, seconds % 60L)
    }

    private fun playChorus() {
        val exercise = workoutExercise ?: return
        val selected = exercise.choruses.randomOrNull() ?: return
        if (!hasAudioPermission(context)) return
        runCatching {
            playerManager.delayedPlay(
                selected.song.uriString,
                selected.chorus.startMs.toInt(),
                exercise.exercise.chorusDelay.toInt(),
                volume = selected.chorus.volume
            )
        }
    }

    private fun setOnClickListener() {
        binding.root.setOnClickListener {
            when (state) {
                WorkoutState.ON_SET -> finishSet()
                WorkoutState.READY_TO_START, WorkoutState.RESTING -> startSet()
                WorkoutState.FINISH_PROMPT -> Unit
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListener() {
        binding.root.setOnTouchListener(object : View.OnTouchListener {
            var accumulatedX = 0f
            var accumulatedY = 0f
            var previousX = 0f
            var previousY = 0f

            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                event ?: return false
                val root = binding.root
                val screenWidth = context.resources.displayMetrics.widthPixels
                val screenHeight = context.resources.displayMetrics.heightPixels
                val minX = -(screenWidth - root.width) / 2f
                val maxX = (screenWidth - root.width) / 2f
                val minY = -(screenHeight - root.height) / 2f
                val maxY = (screenHeight - root.height) / 2f

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        accumulatedX = 0f
                        accumulatedY = 0f
                        previousX = event.rawX
                        previousY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - previousX
                        val deltaY = event.rawY - previousY
                        accumulatedX += abs(deltaX)
                        accumulatedY += abs(deltaY)
                        layoutParams.x = (layoutParams.x + deltaX).coerceIn(minX, maxX).toInt()
                        layoutParams.y = (layoutParams.y + deltaY).coerceIn(minY, maxY).toInt()
                        if (isVisible) windowManager.updateViewLayout(root, layoutParams)
                        if (removeAreaController.isVisible) {
                            removeAreaController.setHighlight(
                                removeAreaController.isOverRemoveArea(event.rawX, event.rawY)
                            )
                        } else if (accumulatedX > 10 || accumulatedY > 10) {
                            removeAreaController.show()
                        }
                        previousX = event.rawX
                        previousY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val isClick = accumulatedX < 10f && accumulatedY < 10f
                        accumulatedX = 0f
                        accumulatedY = 0f
                        if (isClick) view?.performClick()
                        val overRemove = removeAreaController.isVisible &&
                            removeAreaController.isOverRemoveArea(event.rawX, event.rawY)
                        removeAreaController.hide()
                        if (overRemove) onClose()
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        accumulatedX = 0f
                        accumulatedY = 0f
                        removeAreaController.hide()
                        return true
                    }
                }
                return false
            }
        })
    }

    fun show() {
        if (isVisible) return
        binding.timer.text = context.getString(R.string.loading_exercise)
        windowManager.addView(binding.root, layoutParams)
        isVisible = true
        if (workoutExercise != null) readyForSet()
    }

    fun hide() {
        scope.cancel()
        finishDialog.hide()
        removeAreaController.hide()
        stopTimer()
        playerManager.stop()
        if (isVisible) {
            windowManager.removeView(binding.root)
            isVisible = false
        }
    }
}
