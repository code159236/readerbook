package com.v2reading.reader.ui.main.explore

import android.app.Application
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BookSource

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSource) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.insert(bookSource)
        }
    }

}