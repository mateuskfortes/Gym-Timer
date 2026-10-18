package com.example.gymtimer2.timer_overlay_service

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.ViewGroup
import android.widget.TextView
import com.example.gymtimer2.databinding.FinishSetDialogBinding
import com.example.gymtimer2.databinding.FloatTimerBinding
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.ThemeMode

/** Applies the same saved appearance to the XML windows owned by the overlay service. */
class OverlayAppearance(context: Context, settings: AppSettings) {
    private val radiusPx = 18f * context.resources.displayMetrics.density
    private val dark = settings.themeMode == ThemeMode.DARK
    private val accent = settings.accentColor.argb(dark)
    private val surface = if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
    private val surfaceVariant = if (dark) 0xFF2C2C2E.toInt() else 0xFFF2F2F7.toInt()
    private val text = if (dark) 0xFFF5F5F7.toInt() else 0xFF1C1C1E.toInt()
    private val mutedText = if (dark) 0xFFAEAEB2.toInt() else 0xFF6D6D72.toInt()
    private val onAccent = if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()

    fun applyToTimer(binding: FloatTimerBinding, resting: Boolean) {
        val backgroundColor = if (resting) {
            (surface and 0x00FFFFFF) or (0x4D shl 24)
        } else surface
        binding.root.background = rounded(backgroundColor)
        binding.exerciseName.setTextColor(text)
        binding.timer.setTextColor(accent)
    }

    fun applyToDialog(binding: FinishSetDialogBinding) {
        binding.dialogRoot.background = rounded(surface)
        setTextColors(binding.dialogRoot)
        binding.currentWeight.setTextColor(mutedText)
        binding.newWeight.setTextColor(text)
        binding.newWeight.setHintTextColor(mutedText)
        binding.errorText.setTextColor(if (dark) 0xFFFF453A.toInt() else 0xFFFF3B30.toInt())
        binding.newWeight.background = rounded(surfaceVariant, radiusPx * 0.67f)
        binding.keepButton.background = buttonBackground(surfaceVariant)
        binding.keepButton.setTextColor(text)
        binding.nextButton.background = buttonBackground(accent)
        binding.nextButton.setTextColor(onAccent)
    }

    private fun setTextColors(group: ViewGroup) {
        for (index in 0 until group.childCount) {
            when (val child = group.getChildAt(index)) {
                is ViewGroup -> setTextColors(child)
                is TextView -> child.setTextColor(text)
            }
        }
    }

    private fun buttonBackground(color: Int): RippleDrawable = RippleDrawable(
        ColorStateList.valueOf(if (dark) 0x33FFFFFF else 0x22000000),
        rounded(color, radiusPx * 0.67f),
        rounded(0xFFFFFFFF.toInt(), radiusPx * 0.67f)
    )

    private fun rounded(color: Int, radius: Float = radiusPx): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
}
