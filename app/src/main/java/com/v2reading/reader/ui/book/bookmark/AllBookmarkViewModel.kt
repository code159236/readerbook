package com.v2reading.reader.ui.book.bookmark

import android.app.Application
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Bookmark

class AllBookmarkViewModel(application: Application) : BaseViewModel(application) {


    fun initData(onSuccess: (bookmarks: List<Bookmark>) -> Unit) {
        execute {
            appDb.bookmarkDao.all
        }.onSuccess {
            onSuccess.invoke(it)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        execute {
            appDb.bookmarkDao.delete(bookmark)
        }
    }

}