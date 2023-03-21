package com.v2reading.reader.ui.book.changesource

import android.app.Application
import android.os.Bundle
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.BookChapter
import com.v2reading.reader.exception.NoStackTraceException
import com.v2reading.reader.model.webBook.WebBook

@Suppress("MemberVisibilityCanBePrivate")
class ChangeChapterSourceViewModel(application: Application) :
    ChangeBookSourceViewModel(application) {

    var chapterIndex: Int = 0
    var chapterTitle: String = ""

    override fun initData(arguments: Bundle?) {
        super.initData(arguments)
        arguments?.let { bundle ->
            bundle.getString("chapterTitle")?.let {
                chapterTitle = it
            }
            chapterIndex = bundle.getInt("chapterIndex")
        }
    }

    fun getContent(
        book: Book,
        chapter: BookChapter,
        nextChapterUrl: String?,
        success: (content: String) -> Unit,
        error: (msg: String) -> Unit
    ) {
        execute {
            val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                ?: throw NoStackTraceException("站点不存在")
            WebBook.getContentAwait(this, bookSource, book, chapter, nextChapterUrl, false)
        }.onSuccess {
            success.invoke(it)
        }.onError {
            error.invoke(it.localizedMessage ?: "获取正文出错")
        }
    }

}