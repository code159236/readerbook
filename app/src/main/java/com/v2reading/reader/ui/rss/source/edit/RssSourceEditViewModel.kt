package com.v2reading.reader.ui.rss.source.edit

import android.app.Application
import android.content.Intent
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.RssSource
import com.v2reading.reader.help.RuleComplete
import com.v2reading.reader.help.http.CookieStore
import com.v2reading.reader.utils.getClipText
import com.v2reading.reader.utils.msg
import com.v2reading.reader.utils.printOnDebug

import com.v2reading.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers


class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {
    var autoComplete = false
    var rssSource: RssSource = RssSource()
    private var oldSourceUrl: String = ""

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("sourceUrl")
            if (key != null) {
                appDb.rssSourceDao.getByKey(key)?.let {
                    rssSource = it
                }
            }
            oldSourceUrl = rssSource.sourceUrl
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: RssSource, success: (() -> Unit)) {
        execute {
            if (oldSourceUrl != source.sourceUrl) {
                appDb.rssSourceDao.delete(oldSourceUrl)
                oldSourceUrl = source.sourceUrl
            }
            appDb.rssSourceDao.insert(source)
        }.onSuccess {
            success()
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printOnDebug()
        }
    }

    fun pasteSource(onSuccess: (source: RssSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            var source: RssSource? = null
            context.getClipText()?.let { json ->
                source = RssSource.fromJson(json).getOrThrow()
            }
            source
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }.onSuccess {
            if (it != null) {
                onSuccess(it)
            } else {
                context.toastOnUi("格式不对")
            }
        }
    }

    fun importSource(text: String, finally: (source: RssSource) -> Unit) {
        execute {
            val text1 = text.trim()
            RssSource.fromJson(text1).getOrThrow().let {
                finally.invoke(it)
            }
        }.onError {
            context.toastOnUi(it.msg)
        }
    }

    fun clearCookie(url: String) {
        execute {
            CookieStore.removeCookie(url)
        }
    }

    fun ruleComplete(rule: String?, preRule: String? = null, type: Int = 1): String? {
        if (autoComplete) {
            return RuleComplete.autoComplete(rule, preRule, type)
        }
        return rule
    }

}