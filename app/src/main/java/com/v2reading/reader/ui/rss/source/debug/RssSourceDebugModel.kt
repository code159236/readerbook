package com.v2reading.reader.ui.rss.source.debug

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.RssSource
import com.v2reading.reader.model.Debug

class RssSourceDebugModel(application: Application) : BaseViewModel(application),
    Debug.Callback {
    private var rssSource: RssSource? = null
    private var callback: ((Int, String) -> Unit)? = null
    var listSrc: String? = null
    var contentSrc: String? = null

    fun initData(sourceUrl: String?, finally: () -> Unit) {
        sourceUrl?.let {
            execute {
                rssSource = appDb.rssSourceDao.getByKey(sourceUrl)
            }.onFinally {
                finally()
            }
        }
    }

    fun observe(callback: (Int, String) -> Unit) {
        this.callback = callback
    }

    fun startDebug(start: (() -> Unit)? = null, error: (() -> Unit)? = null) {
        rssSource?.let {
            start?.invoke()
            Debug.callback = this
            Debug.startDebug(viewModelScope, it)
        } ?: error?.invoke()
    }

    override fun printLog(state: Int, msg: String) {
        when (state) {
            10 -> listSrc = msg
            20 -> contentSrc = msg
            else -> callback?.invoke(state, msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Debug.cancelDebug(true)
    }

}
