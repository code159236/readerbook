package com.v2reading.reader.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.constant.AppPattern
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BookSource
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.help.ContentProcessor
import com.v2reading.reader.help.SourceHelp
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.http.newCallResponseBody
import com.v2reading.reader.help.http.okHttpClient
import com.v2reading.reader.utils.*


class ImportBookSourceViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<BookSource>()
    val checkSources = arrayListOf<BookSource?>()
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
            val group = groupName?.trim()
            val keepName = AppConfig.importKeepName
            val selectSource = arrayListOf<BookSource>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val source = allSources[index]
                    if (keepName) {
                        checkSources[index]?.let {
                            source.bookSourceName = it.bookSourceName
                            source.bookSourceGroup = it.bookSourceGroup
                            source.customOrder = it.customOrder
                        }
                    }
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            source.bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            source.bookSourceGroup = groups.joinToString(",")
                        } else {
                            source.bookSourceGroup = group
                        }
                    }
                    selectSource.add(source)
                }
            }

            SourceHelp.insertBookSource(*selectSource.toTypedArray())
            ContentProcessor.upReplaceRules()
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            val mText = text.trim()
            when {
                mText.isJsonObject() -> {
                    kotlin.runCatching {
                        val json = JsonPath.parse(mText)
                        json.read<List<String>>("$.sourceUrls")
                    }.onSuccess {
                        it.forEach {
                            importSourceUrl(it)
                        }
                    }.onFailure {
                        BookSource.fromJson(mText).getOrThrow().let {
                            allSources.add(it)
                        }
                    }
                }
                mText.isJsonArray() -> BookSource.fromJsonArray(mText).getOrThrow().let { items ->
                    allSources.addAll(items)
                }
                mText.isAbsUrl() -> {
                    importSourceUrl(mText)
                }
                else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
            }
        }.onError {
            it.printOnDebug()
            errorLiveData.postValue(it.localizedMessage ?: "")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            url(url)
        }.byteStream().let {
            allSources.addAll(BookSource.fromJsonArray(it).getOrThrow())
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.bookSourceDao.getBookSource(it.bookSourceUrl)
                checkSources.add(source)
                selectStatus.add(true)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}