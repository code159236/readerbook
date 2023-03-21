@file:Suppress("NOTHING_TO_INLINE")

package com.v2reading.reader.utils

import androidx.annotation.DrawableRes
import android.app.Fragment
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.TextView

fun Context.drawable(@DrawableRes id: Int): Drawable {
    return if (Build.VERSION.SDK_INT >= 21) {
        resources.getDrawable(id, null)
    } else {
        @Suppress("DEPRECATION")
        resources.getDrawable(id)
    }
}

inline fun Fragment.drawable(@DrawableRes id: Int) = activity.drawable(id)
inline fun View.drawable(@DrawableRes id: Int) = context.drawable(id)

fun Drawable.tint(color: Int, mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN) {
    mutate().setColorFilter(color, mode)
}

var TextView.drawableStart: Drawable?
    get() = drawables[0]
    set(value) = setDrawables(value, drawableTop, drawableEnd, drawableBottom)

var TextView.drawableTop: Drawable?
    get() = drawables[1]
    set(value) = setDrawables(drawableStart, value, drawableEnd, drawableBottom)

var TextView.drawableEnd: Drawable?
    get() = drawables[2]
    set(value) = setDrawables(drawableStart, drawableTop, value, drawableBottom)

var TextView.drawableBottom: Drawable?
    get() = drawables[3]
    set(value) = setDrawables(drawableStart, drawableTop, drawableEnd, value)

@Deprecated(
    "Consider replace with drawableStart to better support right-to-left Layout",
    ReplaceWith("drawableStart")
)
var TextView.drawableLeft: Drawable?
    get() = compoundDrawables[0]
    set(value) = setCompoundDrawablesWithIntrinsicBounds(
        value,
        drawableTop,
        drawableRight,
        drawableBottom
    )

@Deprecated(
    "Consider replace with drawableEnd to better support right-to-left Layout",
    ReplaceWith("drawableEnd")
)
var TextView.drawableRight: Drawable?
    get() = compoundDrawables[2]
    set(value) = setCompoundDrawablesWithIntrinsicBounds(
        drawableLeft,
        drawableTop,
        value,
        drawableBottom
    )

private val TextView.drawables: Array<Drawable?>
    get() = if (Build.VERSION.SDK_INT >= 17) compoundDrawablesRelative else compoundDrawables

private fun TextView.setDrawables(
    start: Drawable?,
    top: Drawable?,
    end: Drawable?,
    buttom: Drawable?
) {
    if (Build.VERSION.SDK_INT >= 17)
        setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, buttom)
    else
        setCompoundDrawablesWithIntrinsicBounds(start, top, end, buttom)
}