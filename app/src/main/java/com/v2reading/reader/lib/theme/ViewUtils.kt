package com.v2reading.reader.lib.theme

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.ColorInt
import com.v2reading.reader.utils.DrawableUtils

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused")
object ViewUtils {

    fun removeOnGlobalLayoutListener(v: View, listener: ViewTreeObserver.OnGlobalLayoutListener) {
        v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }

    fun setBackgroundCompat(view: View, drawable: Drawable?) {
        view.background = drawable
    }

    fun setBackgroundTransition(view: View, newDrawable: Drawable): TransitionDrawable {
        val transition = DrawableUtils.createTransitionDrawable(view.background, newDrawable)
        setBackgroundCompat(view, transition)
        return transition
    }

    fun setBackgroundColorTransition(view: View, @ColorInt newColor: Int): TransitionDrawable {
        val oldColor = view.background

        val start = oldColor ?: ColorDrawable(view.solidColor)
        val end = ColorDrawable(newColor)

        val transition = DrawableUtils.createTransitionDrawable(start, end)

        setBackgroundCompat(view, transition)

        return transition
    }

    fun removeFromParent(view: View) {
        val parent = view.parent
        if (parent is ViewGroup) {
            parent.removeView(view)
        }
    }


}
