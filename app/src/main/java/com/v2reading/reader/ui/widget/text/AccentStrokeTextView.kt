package com.v2reading.reader.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.v2reading.reader.R
import com.v2reading.reader.lib.theme.Selector
import com.v2reading.reader.lib.theme.ThemeStore
import com.v2reading.reader.lib.theme.bottomBackground
import com.v2reading.reader.utils.ColorUtils
import com.v2reading.reader.utils.dpToPx
import com.v2reading.reader.utils.getCompatColor

class AccentStrokeTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    private var radius = 3.dpToPx()
    private val isBottomBackground: Boolean

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccentStrokeTextView)
        radius = typedArray.getDimensionPixelOffset(R.styleable.StrokeTextView_radius, radius)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.StrokeTextView_isBottomBackground, false)
        typedArray.recycle()
        upStyle()
    }

    private fun upStyle() {
        val isLight = ColorUtils.isColorLight(context.bottomBackground)
        val disableColor = if (isBottomBackground) {
            if (isLight) {
                context.getCompatColor(R.color.md_light_disabled)
            } else {
                context.getCompatColor(R.color.md_dark_disabled)
            }
        } else {
            context.getCompatColor(R.color.disabled)
        }
        val accentColor = if (isInEditMode) {
            context.getCompatColor(R.color.accent)
        } else {
            ThemeStore.accentColor(context)
        }
        background = Selector.shapeBuild()
            .setCornerRadius(radius)
            .setStrokeWidth(1.dpToPx())
            .setDisabledStrokeColor(disableColor)
            .setDefaultStrokeColor(accentColor)
            .setPressedBgColor(context.getCompatColor(R.color.transparent30))
            .create()
        setTextColor(
            Selector.colorBuild()
                .setDefaultColor(accentColor)
                .setDisabledColor(disableColor)
                .create()
        )
    }

}
