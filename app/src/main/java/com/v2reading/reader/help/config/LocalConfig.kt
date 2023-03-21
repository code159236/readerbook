package com.v2reading.reader.help.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.v2reading.reader.utils.getBoolean
import com.v2reading.reader.utils.putBoolean
import com.v2reading.reader.utils.putLong
import splitties.init.appCtx

object LocalConfig :
    SharedPreferences by appCtx.getSharedPreferences("local", Context.MODE_PRIVATE) {
    private const val versionCodeKey = "appVersionCode"

    var httpTTSVersion = 0
    var jhVersion = 0

    var lastBackup: Long
        get() = getLong("lastBackup", 0)
        set(value) {
            putLong("lastBackup", value)
        }

    var privacyPolicyOk: Boolean
        get() = getBoolean("privacyPolicyOk",false)
        set(value) {
            putBoolean("privacyPolicyOk", value)
        }
    var setTTS: Boolean
        get() = getBoolean("setTTS")
        set(value) {
            putBoolean("setTTS", value)
        }

    val readHelpVersionIsLast: Boolean
        get() = isLastVersion(1, "readHelpVersion", "firstRead")

    val backupHelpVersionIsLast: Boolean
        get() = isLastVersion(1, "backupHelpVersion", "firstBackup")

    val readMenuHelpVersionIsLast: Boolean
        get() = isLastVersion(1, "readMenuHelpVersion", "firstReadMenu")

    val bookSourcesHelpVersionIsLast: Boolean
        get() = isLastVersion(1, "bookSourceHelpVersion", "firstOpenBookSources")

    val ruleHelpVersionIsLast: Boolean
        get() = isLastVersion(1, "ruleHelpVersion")

    val needUpHttpTTS: Boolean
        get() = !isLastVersion(httpTTSVersion, "httpTtsVersion")

    val needUpJH: Boolean
        get() = !isLastVersion(jhVersion, "jhVersion")

    val needUpTxtTocRule: Boolean
        get() = !isLastVersion(1, "txtTocRuleVersion")

    val needUpRssSources: Boolean
        get() = !isLastVersion(4, "rssSourceVersion")

    var versionCode
        get() = getLong(versionCodeKey, 0)
        set(value) {
            edit { putLong(versionCodeKey, value) }
        }

    val isFirstOpenApp: Boolean
        get() {
            val value = getBoolean("firstOpen", true)
            if (value) {
                edit { putBoolean("firstOpen", false) }
            }
            return value
        }

    @Suppress("SameParameterValue")
    private fun isLastVersion(
        lastVersion: Int,
        versionKey: String,
        firstOpenKey: String? = null
    ): Boolean {
        var version = getInt(versionKey, 0)
        if (version == 0 && firstOpenKey != null) {
            if (!getBoolean(firstOpenKey, true)) {
                version = 1
            }
        }
        if (version < lastVersion) {
            edit { putInt(versionKey, lastVersion) }
            return false
        }
        return true
    }

}