package com.example.autoclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import kotlin.math.abs

class ClickerService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private lateinit var btn: TextView
    private lateinit var btnLp: WindowManager.LayoutParams

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var holdMode = false
    private var holdActive = false
    private val intervalMs = 150L

    private val targetXFraction = 0.5f
    private val targetYFraction = 0.5f

    private val dp get() = resources.displayMetrics.density

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addButton()
    }

    private fun addButton() {
        val size = (56 * dp).toInt()
        btn = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 12f
        }
        refreshButton()
        btnLp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * dp).toInt()
            y = (120 * dp).toInt()
        }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        var moved = false; var downAt = 0L
        btn.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX; sy = e.rawY; ox = btnLp.x; oy = btnLp.y
                    moved = false; downAt = System.currentTimeMillis()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (abs(dx) > 12 * dp || abs(dy) > 12 * dp) moved = true
                    if (moved) {
                        btnLp.x = ox + dx.toInt(); btnLp.y = oy + dy.toInt()
                        wm.updateViewLayout(btn, btnLp)
                    }
                }
                MotionEvent.ACTION_UP -> if (!moved) {
                    if (System.currentTimeMillis() - downAt > 600) {
                        if (!running) { holdMode = !holdMode; refreshButton() }
                    } else toggle()
                }
            }
            true
        }
        wm.addView(btn, btnLp)
    }

    private fun refreshButton() {
        val color = if (running) "#2E7D32" else "#C62828"
        btn.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(color))
        }
        btn.text = (if (running) "ON" else "OFF") + "\n" + (if (holdMode) "HOLD" else "TAP")
    }

    private fun toggle() {
        running = !running
        refreshButton()
        if (running) start()
    }

    private fun targetPath(): Path {
        val m = resources.displayMetrics
        return Path().apply {
            moveTo(m.widthPixels * targetXFraction, m.heightPixels * targetYFraction)
        }
    }

    private fun start() {
        if (holdMode) {
            holdActive = true
            holdStep(StrokeDescription(targetPath(), 0, 600, true), targetPath())
        } else handler.post(tapRunnable)
    }

    private val tapRunnable = object : Runnable {
        override fun run() {
            if (!running || holdMode) return
            val g = GestureDescription.Builder()
                .addStroke(StrokeDescription(targetPath(), 0, 30)).build()
            dispatchGesture(g, null, null)
            handler.postDelayed(this, intervalMs)
        }
    }

    private fun holdStep(stroke: StrokeDescription, path: Path) {
        dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(),
            object : GestureResultCallback() {
                override fun onCompleted(g: GestureDescription?) {
                    if (!holdActive) return
                    val more = running
                    val next = stroke.continueStroke(path, 0, 600, more)
                    if (!more) holdActive = false
                    holdStep(next, path)
                }
                override fun onCancelled(g: GestureDescription?) { holdActive = false }
            }, null
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        if (::btn.isInitialized) wm.removeView(btn)
        super.onDestroy()
    }
}
