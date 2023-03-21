package com.v2reading.reader.ui.book.group

import android.app.Application
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BookGroup

class GroupViewModel(application: Application) : BaseViewModel(application) {

    fun upGroup(vararg bookGroup: BookGroup, finally: (() -> Unit)? = null) {
        execute {
            appDb.bookGroupDao.update(*bookGroup)
        }.onFinally {
            finally?.invoke()
        }
    }

    fun addGroup(groupName: String, cover: String?, finally: () -> Unit) {
        execute {
            val bookGroup = BookGroup(
                groupId = appDb.bookGroupDao.getUnusedId(),
                groupName = groupName,
                cover = cover,
                order = appDb.bookGroupDao.maxOrder.plus(1)
            )
            appDb.bookGroupDao.insert(bookGroup)
        }.onFinally {
            finally()
        }
    }

    fun delGroup(vararg bookGroup: BookGroup, finally: () -> Unit) {
        execute {
            appDb.bookGroupDao.delete(*bookGroup)
            bookGroup.forEach { group ->
                val books = appDb.bookDao.getBooksByGroup(group.groupId)
                books.forEach {
                    it.group = it.group - group.groupId
                }
                appDb.bookDao.update(*books.toTypedArray())
            }
        }.onFinally {
            finally()
        }
    }


}