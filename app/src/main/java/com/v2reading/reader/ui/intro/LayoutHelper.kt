package com.v2reading.reader.ui.intro

import android.annotation.SuppressLint
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.v2reading.reader.App.Companion.instance

object LayoutHelper {
    const val MATCH_PARENT = -1
    const val WRAP_CONTENT = -2
    private fun getSize(size: Int): Int {
        return (if (size < 0) size else dp(size))
    }

    //region Gravity
    private fun getAbsoluteGravity(gravity: Int): Int {
        return Gravity.getAbsoluteGravity(
            gravity,
            if (LocaleController.isRTL) ViewCompat.LAYOUT_DIRECTION_RTL else ViewCompat.LAYOUT_DIRECTION_LTR
        )
    }

    @get:SuppressLint("RtlHardcoded")
    val absoluteGravityStart: Int
        get() = if (LocaleController.isRTL) Gravity.RIGHT else Gravity.LEFT

    @get:SuppressLint("RtlHardcoded")
    val absoluteGravityEnd: Int
        get() = if (LocaleController.isRTL) Gravity.LEFT else Gravity.RIGHT

    //endregion
    //region ScrollView
    fun createScroll(width: Int, height: Int, gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            getSize(width),
            getSize(height),
            gravity
        )
    }


    //endregion
    //region FrameLayout
    fun createFrame(
        width: Int,
        height: Int,
        gravity: Int,
        leftMargin: Int,
        topMargin: Int,
        rightMargin: Int,
        bottomMargin: Int
    ): FrameLayout.LayoutParams {
        val layoutParams =
            FrameLayout.LayoutParams(getSize(width), getSize(height), gravity)
        layoutParams.setMargins(dp(leftMargin), dp(topMargin), dp(rightMargin), dp(bottomMargin))
        return layoutParams
    }

    fun createFrame(width: Int, height: Int, gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            getSize(width),
            getSize(height),
            gravity
        )
    }

    fun createFrame(width: Int, height: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(getSize(width), getSize(height))
    }


    fun dp(value: Int): Int {
        return if (value == 0) {
            0
        } else Math.ceil(
            (instance!!.resources.displayMetrics.density * value).toDouble()
        ).toInt()
    } //endregion

    object LocaleController {
        var isRTL = false
    }
}