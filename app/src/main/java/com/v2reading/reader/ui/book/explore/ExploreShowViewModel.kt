package com.v2reading.reader.ui.book.explore

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BookSource
import com.v2reading.reader.data.entities.SearchBook
import com.v2reading.reader.model.webBook.WebBook
import com.v2reading.reader.utils.msg
import com.v2reading.reader.utils.printOnDebug

import kotlinx.coroutines.Dispatchers.IO


class ExploreShowViewModel(application: Application) : BaseViewModel(application) {

    val booksData = MutableLiveData<List<SearchBook>>()
    val errorLiveData = MutableLiveData<String>()
    private var bookSource: BookSource? = null
    private var exploreUrl: String? = null
    private var page = 1

    fun initData(intent: Intent) {
        execute {
            val sourceUrl = intent.getStringExtra("sourceUrl")
            exploreUrl = intent.getStringExtra("exploreUrl")
            if (bookSource == null && sourceUrl != null) {
                bookSource = appDb.bookSourceDao.getBookSource(sourceUrl)
            }
            explore()
        }
    }

    fun explore() {
        val source = bookSource
        val url = exploreUrl
        if (source != null && url != null) {
            WebBook.exploreBook(viewModelScope, source, url, page)
                .timeout(30000L)
                .onSuccess(IO) { searchBooks ->
                    booksData.postValue(searchBooks)
                    appDb.searchBookDao.insert(*searchBooks.toTypedArray())
                    page++
                }.onError {
                    it.printOnDebug()
                    errorLiveData.postValue(it.msg)
                }
        }
    }

}