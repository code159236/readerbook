package com.v2reading.reader.help

import android.os.Handler
import android.os.Looper
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BookSource
import com.v2reading.reader.data.entities.RssSource
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.utils.EncoderUtils
import com.v2reading.reader.utils.NetworkUtils
import com.v2reading.reader.utils.splitNotBlank
import com.v2reading.reader.utils.toastOnUi
import splitties.init.appCtx

object SourceHelp {

    private val handler = Handler(Looper.getMainLooper())
    private val list18Plus by lazy {
        try {
            return@lazy String(appCtx.assets.open("18PlusList.txt").readBytes())
                .splitNotBlank("\n")
        } catch (e: Exception) {
            return@lazy arrayOf<String>()
        }
    }

    fun insertRssSource(vararg rssSources: RssSource) {
        rssSources.forEach { rssSource ->
            if (is18Plus(rssSource.sourceUrl)) {
                handler.post {
                    appCtx.toastOnUi("${rssSource.sourceName}是18+网址,禁止导入.")
                }
            } else {
                appDb.rssSourceDao.insert(rssSource)
            }
        }
    }

    fun insertBookSource(vararg bookSources: BookSource) {
        if (bookSources.isNotEmpty()) {
            appDb.bookSourceDao.deleteAll()
//        bookSources.forEach { bookSource ->
//            if (is18Plus(bookSource.bookSourceUrl)) {
//                handler.post {
//                    appCtx.toastOnUi("${bookSource.bookSourceName}是18+网址,禁止导入.")
//                }
//            } else {
//                appDb.bookSourceDao.insert(bookSource)
//            }
//        }
            appDb.bookSourceDao.insert(*bookSources)
        }
    }

    private fun is18Plus(url: String?): Boolean {
        url ?: return false
        val baseUrl = NetworkUtils.getBaseUrl(url)
        baseUrl ?: return false
        if (AppConfig.isGooglePlay) return false
        try {
            val host = baseUrl.split("//", ".")
            val base64Url = EncoderUtils.base64Encode("${host[host.lastIndex - 1]}.${host.last()}")
            list18Plus.forEach {
                if (base64Url == it) {
                    return true
                }
            }
        } catch (e: Exception) {
        }
        return false
    }

}