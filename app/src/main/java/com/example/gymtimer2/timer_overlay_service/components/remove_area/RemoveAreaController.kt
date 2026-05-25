package com.example.gymtimer2.timer_overlay_service.components.remove_area

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.example.gymtimer2.databinding.FloatTimerRemoveAreaBinding

const val REMOVE_AREA_HEIGHT = 200
const val REMOVE_AREA_WIDTH = 200

class RemoveAreaController {
    private val context: Context
    private val inflater: LayoutInflater
    private val windowManager: WindowManager
    private lateinit var binding: FloatTimerRemoveAreaBinding
    private lateinit var layoutParams: WindowManager.LayoutParams
    var isVisible = false
    private var isHighlighted = false

    constructor(
        context: Context,
        inflater: LayoutInflater,
        windowManager: WindowManager,
    ){
        this.windowManager = windowManager
        this.inflater = inflater
        this.context = context
        setBinding()
        setLayoutParams()
    }

    fun setBinding() {
        binding = FloatTimerRemoveAreaBinding.inflate(inflater)
    }

    fun setLayoutParams() {
        val metrics = context.resources.displayMetrics
        val height = metrics.heightPixels

        @Suppress("DEPRECATION")
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            REMOVE_AREA_WIDTH,
            REMOVE_AREA_HEIGHT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = height / 2  - REMOVE_AREA_HEIGHT / 2 - 200
        }
    }

    fun isOverRemoveArea(touchX: Float, touchY: Float): Boolean {

        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        val removeAreaCenterX = screenWidth / 2f
        val removeAreaCenterY = screenHeight / 2f + layoutParams.y
        val halfW = REMOVE_AREA_WIDTH / 2f
        val halfH = REMOVE_AREA_HEIGHT / 2f

        val left = removeAreaCenterX - halfW
        val right = removeAreaCenterX + halfW
        val top = removeAreaCenterY - halfH
        val bottom = removeAreaCenterY + halfH

        return touchX in left..right && touchY in top..bottom
    }

    fun setHighlight(highlight: Boolean) {
        if (highlight == isHighlighted) return

        isHighlighted = highlight
        if (highlight) {
            binding.removeIcon.scaleX = 1.3f
            binding.removeIcon.scaleY = 1.3f
            binding.removeAreaBackground.setBackgroundColor(0xFFFF8888.toInt())
        } else {
            binding.removeIcon.scaleX = 1f
            binding.removeIcon.scaleY = 1f
            binding.removeAreaBackground.setBackgroundColor(0xFFCC0000.toInt())
        }
    }

    fun show() {
        windowManager.addView(binding.root, layoutParams)
        isVisible = true
    }

    fun hide() {
        runCatching {
            windowManager.removeView(binding.root)
            isVisible = false
        }
    }
}