package com.v2reading.reader.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.v2reading.reader.lib.theme.secondaryTextColor

/**
 * @author Aidan Follestad (afollestad)
 */
@Suppress("unused")
class SecondaryTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        setTextColor(context.secondaryTextColor)
    }
}
