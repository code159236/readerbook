package com.v2reading.reader.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import com.v2reading.reader.lib.theme.accentColor
import com.v2reading.reader.utils.applyTint

/**
 * @author Aidan Follestad (afollestad)
 */
class ThemeSwitch(context: Context, attrs: AttributeSet) : SwitchCompat(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }

    }

}
