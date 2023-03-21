package com.v2reading.reader.ui.book.manage

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.BookSource
import com.v2reading.reader.help.coroutine.Coroutine
import com.v2reading.reader.model.webBook.WebBook
import com.v2reading.reader.utils.toastOnUi


class BookshelfManageViewModel(application: Application) : BaseViewModel(application) {

    val batchChangeSourceState = mutableStateOf(false)
    val batchChangeSourceSize = mutableStateOf(0)
    val batchChangeSourcePosition = mutableStateOf(0)
    var batchChangeSourceCoroutine: Coroutine<Unit>? = null

    fun upCanUpdate(books: List<Book>, canUpdate: Boolean) {
        execute {
            val array = Array(books.size) {
                books[it].copy(canUpdate = canUpdate)
            }
            appDb.bookDao.update(*array)
        }
    }

    fun updateBook(vararg book: Book) {
        execute {
            appDb.bookDao.update(*book)
        }
    }

    fun deleteBook(vararg book: Book) {
        execute {
            appDb.bookDao.delete(*book)
        }
    }

    fun changeSource(books: List<Book>, source: BookSource) {
        batchChangeSourceCoroutine?.cancel()
        batchChangeSourceCoroutine = execute {
            batchChangeSourceSize.value = books.size
            books.forEachIndexed { index, book ->
                batchChangeSourcePosition.value = index + 1
                if (book.isLocalBook()) return@forEachIndexed
                if (book.origin == source.bookSourceUrl) return@forEachIndexed
                WebBook.preciseSearchAwait(this, source, book.name, book.author)
                    .onFailure {
                        context.toastOnUi("获取书籍出错\n${it.localizedMessage}")
                    }.getOrNull()?.let { newBook ->
                        WebBook.getChapterListAwait(this, source, newBook)
                            .onFailure {
                                context.toastOnUi("获取目录出错\n${it.localizedMessage}")
                            }.getOrNull()?.let { toc ->
                                book.changeTo(newBook, toc)
                                appDb.bookDao.insert(newBook)
                                appDb.bookChapterDao.insert(*toc.toTypedArray())
                            }
                    }
            }
        }.onFinally {
            batchChangeSourceState.value = false
        }
    }

}