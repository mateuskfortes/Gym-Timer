package com.example.gymtimer2.timer_overlay_service.components.float_timer

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.example.gymtimer2.R
import com.example.gymtimer2.databinding.FinishSetDialogBinding
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.timer_overlay_service.overlayWindowType

enum class FinishSetAction { KEEP, NEXT_OR_FINISH }

class FinishSetDialogController(
    private val context: Context,
    inflater: LayoutInflater,
    private val windowManager: WindowManager,
    private val onSelection: (FinishSetAction, Int?) -> Unit
) {
    private val binding = FinishSetDialogBinding.inflate(inflater)
    private var isVisible = false

    private val layoutParams = WindowManager.LayoutParams(
        (context.resources.displayMetrics.density * 320).toInt(),
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayWindowType(),
        0,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }

    fun show(exercise: ExerciseModel, hasNext: Boolean) {
        if (isVisible) return
        binding.exerciseName.text = exercise.name
        binding.currentWeight.text = context.getString(R.string.current_weight_format, exercise.weight.plainText)
        binding.newWeight.text.clear()
        binding.errorText.visibility = View.GONE
        binding.nextButton.setText(if (hasNext) R.string.next_exercise else R.string.finish_workout)
        setSubmitting(false)
        binding.keepButton.setOnClickListener { submit(FinishSetAction.KEEP) }
        binding.nextButton.setOnClickListener { submit(FinishSetAction.NEXT_OR_FINISH) }
        binding.dialogRoot.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                submit(FinishSetAction.KEEP)
                true
            } else false
        }
        windowManager.addView(binding.root, layoutParams)
        binding.dialogRoot.requestFocus()
        isVisible = true
    }

    fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
        setSubmitting(false)
    }

    fun hide() {
        if (!isVisible) return
        windowManager.removeView(binding.root)
        isVisible = false
    }

    private fun submit(action: FinishSetAction) {
        if (!isVisible || !binding.keepButton.isEnabled) return
        val text = binding.newWeight.text.toString().trim()
        val newWeight = if (text.isEmpty()) null else text.toIntOrNull()
        if (text.isNotEmpty() && newWeight == null) {
            showError(context.getString(R.string.invalid_weight))
            return
        }
        setSubmitting(true)
        onSelection(action, newWeight)
    }

    private fun setSubmitting(submitting: Boolean) {
        binding.keepButton.isEnabled = !submitting
        binding.nextButton.isEnabled = !submitting
    }
}
