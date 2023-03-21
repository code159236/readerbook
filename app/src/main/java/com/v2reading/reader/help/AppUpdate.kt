package com.v2reading.reader.help

import com.v2reading.reader.R
import com.v2reading.reader.constant.AppConst
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.help.coroutine.Coroutine
import com.v2reading.reader.help.http.newCallStrResponse
import com.v2reading.reader.help.http.okHttpClient
import com.v2reading.reader.utils.jsonPath
import com.v2reading.reader.utils.readString
import com.v2reading.reader.utils.toastOnUi
import kotlinx.coroutines.CoroutineScope
import splitties.init.appCtx

object AppUpdate {

    fun checkFromGitHub(
        scope: CoroutineScope,
        showErrorMsg: Boolean = true,
        callback: (newVersion: String, updateBody: String, url: String, fileName: String) -> Unit
    ) {
        Coroutine.async(scope) {
            val lastReleaseUrl = appCtx.getString(R.string.latest_release_api)
            val body = okHttpClient.newCallStrResponse {
                url(lastReleaseUrl)
            }.body
            if (body.isNullOrBlank()) {
                throw NoStackTraceException("获取新版本出错")
            }
            val rootDoc = jsonPath.parse(body)
            val tagName = rootDoc.readString("$.tag_name")
                ?: throw NoStackTraceException("获取新版本出错")
            if (tagName > AppConst.appInfo.versionName) {
                val updateBody = rootDoc.readString("$.body")
                    ?: throw NoStackTraceException("获取新版本出错")
                val downloadUrl = rootDoc.readString("$.assets[0].browser_download_url")
                    ?: throw NoStackTraceException("获取新版本出错")
                val fileName = rootDoc.readString("$.assets[0].name")
                    ?: throw NoStackTraceException("获取新版本出错")
                return@async arrayOf(tagName, updateBody, downloadUrl, fileName)
            } else {
                throw NoStackTraceException("已是最新版本")
            }
        }.timeout(10000)
            .onSuccess {
                callback.invoke(it[0], it[1], it[2], it[3])
            }.onError {
                if (showErrorMsg) {
                    appCtx.toastOnUi("检测更新\n${it.localizedMessage}")
                }
            }
    }

}