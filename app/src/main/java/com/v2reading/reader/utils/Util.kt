package com.v2reading.reader.utils

import android.content.Context
import java.math.BigDecimal

object Util {
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    fun change(a: Double): Double {
        return a * Math.PI / 180
    }

    fun changeAngle(a: Double): Double {
        return a * 180 / Math.PI
    }

    // 除法运算
    @JvmStatic
    fun div(d1:Double,d2: Double):Double = BigDecimal(d1).divide(BigDecimal(d2),2,BigDecimal.ROUND_HALF_DOWN).toDouble()

}