package com.v2reading.reader.ui.book.read.config

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.HttpTTS
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.help.DefaultData
import com.v2reading.reader.help.http.newCallResponseBody
import com.v2reading.reader.help.http.okHttpClient
import com.v2reading.reader.help.http.text
import com.v2reading.reader.utils.isJsonArray
import com.v2reading.reader.utils.isJsonObject
import com.v2reading.reader.utils.readText
import com.v2reading.reader.utils.toastOnUi

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> = TextToSpeech(context, null).engines

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

    fun importOnLine(url: String) {
        execute {
            okHttpClient.newCallResponseBody {
                url(url)
            }.text("utf-8").let { json ->
                import(json)
            }
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败")
        }
    }

    fun importLocal(uri: Uri) {
        execute {
            import(uri.readText(context))
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败\n${it.localizedMessage}")
        }
    }

    fun import(text: String) {
        when {
            text.isJsonArray() -> {
                HttpTTS.fromJsonArray(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(it)
                }
            }
            else -> {
                throw NoStackTraceException("格式不对")
            }
        }
    }

}