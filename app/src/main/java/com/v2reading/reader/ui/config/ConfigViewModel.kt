package com.v2reading.reader.ui.config

import android.app.Application
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.help.AppWebDav
import com.v2reading.reader.help.BookHelp
import com.v2reading.reader.utils.FileUtils
import com.v2reading.reader.utils.toastOnUi

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }


}