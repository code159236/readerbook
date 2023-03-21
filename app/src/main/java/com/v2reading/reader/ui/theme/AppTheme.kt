package com.v2reading.reader.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.v2reading.reader.help.config.ThemeConfig
import com.v2reading.reader.lib.theme.accentColor
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.utils.ColorUtils
import splitties.init.appCtx

object AppTheme {

    val colors
        get() = if (ThemeConfig.isDarkTheme()) {
            darkColors(
                primary = Color(appCtx.accentColor),
                primaryVariant = Color(ColorUtils.darkenColor(appCtx.accentColor)),
                secondary = Color(appCtx.primaryColor),
                secondaryVariant = Color(appCtx.primaryColor)
            )
        } else {
            lightColors(
                primary = Color(appCtx.accentColor),
                primaryVariant = Color(ColorUtils.darkenColor(appCtx.accentColor)),
                secondary = Color(appCtx.primaryColor),
                secondaryVariant = Color(appCtx.primaryColor)
            )
        }

}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = AppTheme.colors,
        content = content
    )
}