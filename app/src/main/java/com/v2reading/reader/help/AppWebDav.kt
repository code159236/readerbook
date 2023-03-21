package com.v2reading.reader.help

import android.content.Context
import com.v2reading.reader.R
import com.v2reading.reader.constant.AppLog
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.BookProgress
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.coroutine.Coroutine
import com.v2reading.reader.help.storage.Backup
import com.v2reading.reader.help.storage.Restore
import com.v2reading.reader.lib.dialogs.selector
import com.v2reading.reader.lib.webdav.Authorization
import com.v2reading.reader.lib.webdav.WebDav
import com.v2reading.reader.lib.webdav.WebDavException
import com.v2reading.reader.lib.webdav.WebDavFile
import com.v2reading.reader.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * webDav初始化会访问网络,不要放到主线程
 */
object AppWebDav {
    private const val defaultWebDavUrl = "https://dav.jianguoyun.com/dav/"
    private val zipFilePath = "${appCtx.externalFiles.absolutePath}${File.separator}backup.zip"
    private val bookProgressUrl get() = "${rootWebDavUrl}bookProgress/"
    private val exportsWebDavUrl get() = "${rootWebDavUrl}books/"
    val syncBookProgress get() = appCtx.getPrefBoolean(PreferKey.syncBookProgress, true)

    var authorization: Authorization? = null
        private set

    val isOk get() = authorization != null

    init {
        runBlocking {
            upConfig()
        }
    }

    val rootWebDavUrl: String
        get() {
            val configUrl = appCtx.getPrefString(PreferKey.webDavUrl)?.trim()
            var url = if (configUrl.isNullOrEmpty()) defaultWebDavUrl else configUrl
            if (!url.endsWith("/")) url = "${url}/"
            AppConfig.webDavDir?.trim()?.let {
                if (it.isNotEmpty()) {
                    url = "${url}${it}/"
                }
            }
            return url
        }

    private val backupFileName: String
        get() {
            val backupDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(System.currentTimeMillis()))
            return "backup${backupDate}.zip"
        }

    suspend fun upConfig() {
        kotlin.runCatching {
            authorization = null
            val account = appCtx.getPrefString(PreferKey.webDavAccount)
            val password = appCtx.getPrefString(PreferKey.webDavPassword)
            if (!account.isNullOrBlank() && !password.isNullOrBlank()) {
                val mAuthorization = Authorization(account, password)
                WebDav(rootWebDavUrl, mAuthorization).makeAsDir()
                WebDav(bookProgressUrl, mAuthorization).makeAsDir()
                WebDav(exportsWebDavUrl, mAuthorization).makeAsDir()
                authorization = mAuthorization
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun getBackupNames(): ArrayList<String> {
        val names = arrayListOf<String>()
        authorization?.let {
            var files = WebDav(rootWebDavUrl, it).listFiles()
            files = files.reversed()
            files.forEach { webDav ->
                val name = webDav.displayName
                if (name.startsWith("backup")) {
                    names.add(name)
                }
            }
        } ?: throw NoStackTraceException("webDav没有配置")
        return names
    }

    suspend fun showRestoreDialog(context: Context) {
        val names = withContext(IO) { getBackupNames() }
        if (names.isNotEmpty()) {
            withContext(Main) {
                context.selector(
                    title = context.getString(R.string.select_restore_file),
                    items = names
                ) { _, index ->
                    if (index in 0 until names.size) {
                        Coroutine.async {
                            restoreWebDav(names[index])
                        }.onError {
                            appCtx.toastOnUi("WebDav恢复出错\n${it.localizedMessage}")
                        }
                    }
                }
            }
        } else {
            throw NoStackTraceException("Web dav no back up file")
        }
    }

    @Throws(WebDavException::class)
    suspend fun restoreWebDav(name: String) {
        authorization?.let {
            val webDav = WebDav(rootWebDavUrl + name, it)
            webDav.downloadTo(zipFilePath, true)
            @Suppress("BlockingMethodInNonBlockingContext")
            ZipUtils.unzipFile(zipFilePath, Backup.backupPath)
            Restore.restoreDatabase()
            Restore.restoreConfig()
        }
    }

    suspend fun hasBackUp(): Boolean {
        authorization?.let {
            val url = "$rootWebDavUrl$backupFileName"
            return WebDav(url, it).exists()
        }
        return false
    }

    suspend fun lastBackUp(): Result<WebDavFile?> {
        return kotlin.runCatching {
            authorization?.let {
                var lastBackupFile: WebDavFile? = null
                WebDav(rootWebDavUrl, it).listFiles().reversed().forEach { webDavFile ->
                    if (webDavFile.displayName.startsWith("backup")) {
                        if (lastBackupFile == null
                            || webDavFile.lastModify > lastBackupFile!!.lastModify
                        ) {
                            lastBackupFile = webDavFile
                        }
                    }
                }
                lastBackupFile
            }
        }
    }

    @Throws(Exception::class)
    suspend fun backUpWebDav(path: String) {
        if (!NetworkUtils.isAvailable()) return
        authorization?.let {
            val paths = arrayListOf(*Backup.backupFileNames)
            for (i in 0 until paths.size) {
                paths[i] = path + File.separator + paths[i]
            }
            FileUtils.delete(zipFilePath)
            if (ZipUtils.zipFiles(paths, zipFilePath)) {
                val putUrl = "$rootWebDavUrl$backupFileName"
                WebDav(putUrl, it).upload(zipFilePath)
            }
        }
    }

    suspend fun exportWebDav(byteArray: ByteArray, fileName: String) {
        if (!NetworkUtils.isAvailable()) return
        try {
            authorization?.let {
                // 如果导出的本地文件存在,开始上传
                val putUrl = exportsWebDavUrl + fileName
                WebDav(putUrl, it).upload(byteArray, "text/plain")
            }
        } catch (e: Exception) {
            val msg = "WebDav导出\n${e.localizedMessage}"
            AppLog.put(msg)
            appCtx.toastOnUi(msg)
        }
    }

    fun uploadBookProgress(book: Book) {
        val authorization = authorization ?: return
        if (!syncBookProgress) return
        if (!NetworkUtils.isAvailable()) return
        Coroutine.async {
            val bookProgress = BookProgress(book)
            val json = GSON.toJson(bookProgress)
            val url = getProgressUrl(book.name, book.author)
            WebDav(url, authorization).upload(json.toByteArray(), "application/json")
        }.onError {
            AppLog.put("上传进度失败\n${it.localizedMessage}")
        }
    }

    fun uploadBookProgress(bookProgress: BookProgress) {
        val authorization = authorization ?: return
        if (!syncBookProgress) return
        if (!NetworkUtils.isAvailable()) return
        Coroutine.async {
            val json = GSON.toJson(bookProgress)
            val url = getProgressUrl(bookProgress.name, bookProgress.author)
            WebDav(url, authorization).upload(json.toByteArray(), "application/json")
        }.onError {
            AppLog.put("上传进度失败\n${it.localizedMessage}")
        }
    }

    private fun getProgressUrl(name: String, author: String): String {
        return bookProgressUrl + name + "_" + author + ".json"
    }

    /**
     * 获取书籍进度
     */
    suspend fun getBookProgress(book: Book): BookProgress? {
        authorization?.let {
            val url = getProgressUrl(book.name, book.author)
            kotlin.runCatching {
                WebDav(url, it).download().let { byteArray ->
                    val json = String(byteArray)
                    if (json.isJson()) {
                        return GSON.fromJsonObject<BookProgress>(json).getOrNull()
                    }
                }
            }
        }
        return null
    }

    suspend fun downloadAllBookProgress() {
        authorization ?: return
        if (!NetworkUtils.isAvailable()) return
        appDb.bookDao.all.forEach { book ->
            getBookProgress(book)?.let { bookProgress ->
                if (bookProgress.durChapterIndex > book.durChapterIndex
                    || (bookProgress.durChapterIndex == book.durChapterIndex
                            && bookProgress.durChapterPos > book.durChapterPos)
                ) {
                    book.durChapterIndex = bookProgress.durChapterIndex
                    book.durChapterPos = bookProgress.durChapterPos
                    book.durChapterTitle = bookProgress.durChapterTitle
                    book.durChapterTime = bookProgress.durChapterTime
                    appDb.bookDao.update(book)
                }
            }
        }
    }

}