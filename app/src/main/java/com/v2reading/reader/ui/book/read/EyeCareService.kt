package com.v2reading.reader.ui.book.read

import android.app.Service
import android.view.WindowManager
import android.widget.FrameLayout
import android.os.Build
import android.view.Gravity
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.IBinder


class EyeCareService : Service() {
    private var windowManager: WindowManager? = null
    private var coverLayout: FrameLayout? = null
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type =
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY or WindowManager.LayoutParams.TYPE_STATUS_BAR
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.format = PixelFormat.TRANSLUCENT
        params.gravity = Gravity.START or Gravity.TOP
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager!!.defaultDisplay.getRealSize(point)
        }
        params.width = point.x
        params.height = point.y
        coverLayout = FrameLayout(this)
        coverLayout!!.setBackgroundColor(getFilterColor(30))
        windowManager!!.addView(coverLayout, params)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        windowManager!!.removeViewImmediate(coverLayout)
        super.onDestroy()
    }

    /**
     * 过滤蓝光
     *
     * @param blueFilterPercent 蓝光过滤比例[10-30-80]
     */
    fun getFilterColor(blueFilterPercent: Int): Int {
        var realFilter = blueFilterPercent
        if (realFilter < 10) {
            realFilter = 10
        } else if (realFilter > 80) {
            realFilter = 80
        }
        val a = (realFilter / 80f * 180).toInt()
        val r = (200 - realFilter / 80f * 190).toInt()
        val g = (180 - realFilter / 80f * 170).toInt()
        val b = (60 - realFilter / 80f * 60).toInt()
        return Color.argb(a, r, g, b)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}