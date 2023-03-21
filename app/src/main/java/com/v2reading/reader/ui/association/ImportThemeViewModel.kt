package com.v2reading.reader.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.help.config.ThemeConfig
import com.v2reading.reader.help.http.newCallResponseBody
import com.v2reading.reader.help.http.okHttpClient
import com.v2reading.reader.help.http.text
import com.v2reading.reader.utils.*

class ImportThemeViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<ThemeConfig.Config>()
    val checkSources = arrayListOf<ThemeConfig.Config?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    ThemeConfig.addConfig(allSources[index])
                }
            }
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            it.printOnDebug()
            errorLiveData.postValue(it.localizedMessage ?: "")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                GSON.fromJsonObject<ThemeConfig.Config>(text).getOrThrow()?.let {
                    allSources.add(it)
                }
            }
            text.isJsonArray() -> GSON.fromJsonArray<ThemeConfig.Config>(text).getOrThrow()
                ?.let { items ->
                    allSources.addAll(items)
                }
            text.isAbsUrl() -> {
                importSourceUrl(text)
            }
            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            url(url)
        }.text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach { config ->
                val source = ThemeConfig.configList.find {
                    it.themeName == config.themeName
                }
                checkSources.add(source)
                selectStatus.add(source == null || source != config)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}