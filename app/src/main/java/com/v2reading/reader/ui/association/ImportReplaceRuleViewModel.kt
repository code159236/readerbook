package com.v2reading.reader.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.constant.AppPattern
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.ReplaceRule
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.help.ReplaceAnalyzer
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.http.newCallResponseBody
import com.v2reading.reader.help.http.okHttpClient
import com.v2reading.reader.help.http.text
import com.v2reading.reader.utils.isAbsUrl
import com.v2reading.reader.utils.isJsonArray
import com.v2reading.reader.utils.isJsonObject
import com.v2reading.reader.utils.splitNotBlank

class ImportReplaceRuleViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allRules = arrayListOf<ReplaceRule>()
    val checkRules = arrayListOf<ReplaceRule?>()
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
            val selectRules = arrayListOf<ReplaceRule>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val rule = allRules[index]
                    if (keepName) {
                        checkRules[index]?.let {
                            rule.name = it.name
                            rule.group = it.group
                            rule.order = it.order
                        }
                    }
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            rule.group?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            rule.group = groups.joinToString(",")
                        } else {
                            rule.group = group
                        }
                    }
                    selectRules.add(rule)
                }
            }
            appDb.replaceRuleDao.insert(*selectRules.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun import(text: String) {
        execute {
            importAwait(text.trim())
        }.onError {
            errorLiveData.postValue(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importAwait(text: String) {
        when {
            text.isAbsUrl() -> importUrl(text)
            text.isJsonArray() -> {
                val rules = ReplaceAnalyzer.jsonToReplaceRules(text).getOrThrow()
                allRules.addAll(rules)
            }
            text.isJsonObject() -> {
                val rule = ReplaceAnalyzer.jsonToReplaceRule(text).getOrThrow()
                allRules.add(rule)
            }
            else -> throw NoStackTraceException("格式不对")
        }
    }

    private suspend fun importUrl(url: String) {
        okHttpClient.newCallResponseBody {
            url(url)
        }.text("utf-8").let {
            importAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allRules.forEach {
                val rule = appDb.replaceRuleDao.findById(it.id)
                checkRules.add(rule)
                selectStatus.add(rule == null)
            }
        }.onSuccess {
            successLiveData.postValue(allRules.size)
        }
    }
}