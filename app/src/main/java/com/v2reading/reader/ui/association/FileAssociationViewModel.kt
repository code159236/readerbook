package com.v2reading.reader.ui.association

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import com.v2reading.reader.constant.AppPattern.bookFileRegex
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.model.localBook.LocalBook
import com.v2reading.reader.utils.isJson
import com.v2reading.reader.utils.printOnDebug
import com.v2reading.reader.utils.readText
import java.io.File

class FileAssociationViewModel(application: Application) : BaseAssociationViewModel(application) {
    val importBookLiveData = MutableLiveData<Uri>()
    val onLineImportLive = MutableLiveData<Uri>()
    val openBookLiveData = MutableLiveData<String>()
    val notSupportedLiveData = MutableLiveData<Pair<Uri, String>>()

    @Suppress("BlockingMethodInNonBlockingContext")
    fun dispatchIndent(uri: Uri) {
        execute {
            lateinit var fileName: String
            lateinit var content: String
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.scheme == "file" || uri.scheme == "content") {
                if (uri.scheme == "file") {
                    val file = File(uri.path.toString())
                    content = file.readText()
                    fileName = file.name
                } else {
                    val file = DocumentFile.fromSingleUri(context, uri)
                    content = file?.readText(context) ?: throw NoStackTraceException("文件不存在")
                    fileName = file.name ?: ""
                }
                when {
                    content.isJson() -> {
                        importJson(content)
                    }
                    fileName.matches(bookFileRegex) -> {
                        importBookLiveData.postValue(uri)
                    }
                    else -> {
                        notSupportedLiveData.postValue(Pair(uri, fileName))
                    }
                }
            } else {
                onLineImportLive.postValue(uri)
            }
        }.onError {
            it.printOnDebug()
            errorLive.postValue(it.localizedMessage)
        }
    }

    fun importBook(uri: Uri) {
        val book = LocalBook.importFile(uri)
        openBookLiveData.postValue(book.bookUrl)
    }
}