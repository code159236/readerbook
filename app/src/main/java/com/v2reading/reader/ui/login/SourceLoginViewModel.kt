package com.v2reading.reader.ui.login

import android.app.Application
import android.content.Intent
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BaseSource
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.utils.toastOnUi

class SourceLoginViewModel(application: Application) : BaseViewModel(application) {

    var source: BaseSource? = null

    fun initData(intent: Intent, success: (bookSource: BaseSource) -> Unit) {
        execute {
            val sourceKey = intent.getStringExtra("key")
                ?: throw NoStackTraceException("没有参数")
            when (intent.getStringExtra("type")) {
                "bookSource" -> source = appDb.bookSourceDao.getBookSource(sourceKey)
                "rssSource" -> source = appDb.rssSourceDao.getByKey(sourceKey)
                "httpTts" -> source = appDb.httpTTSDao.get(sourceKey.toLong())
            }
            source
        }.onSuccess {
            if (it != null) {
                success.invoke(it)
            } else {
                context.toastOnUi("未找到站点")
            }
        }
    }

}