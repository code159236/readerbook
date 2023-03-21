package com.v2reading.reader.ui.rss.article

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.constant.AppLog
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.RssArticle
import com.v2reading.reader.data.entities.RssSource
import com.v2reading.reader.model.rss.Rss
import com.v2reading.reader.utils.printOnDebug

import com.v2reading.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {
    val loadFinally = MutableLiveData<Boolean>()
    var isLoading = true
    var order = System.currentTimeMillis()
    private var nextPageUrl: String? = null
    var sortName: String = ""
    var sortUrl: String = ""
    var page = 1

    fun init(bundle: Bundle?) {
        bundle?.let {
            sortName = it.getString("sortName") ?: ""
            sortUrl = it.getString("sortUrl") ?: ""
        }
    }

    fun loadContent(rssSource: RssSource) {
        isLoading = true
        page = 1
        Rss.getArticles(viewModelScope, sortName, sortUrl, rssSource, page)
            .onSuccess(Dispatchers.IO) {
                nextPageUrl = it.second
                it.first.let { list ->
                    list.forEach { rssArticle ->
                        rssArticle.order = order--
                    }
                    appDb.rssArticleDao.insert(*list.toTypedArray())
                    if (!rssSource.ruleNextPage.isNullOrEmpty()) {
                        appDb.rssArticleDao.clearOld(rssSource.sourceUrl, sortName, order)
                        loadFinally.postValue(true)
                    } else {
                        withContext(Dispatchers.Main) {
                            loadFinally.postValue(false)
                        }
                    }
                    isLoading = false
                }
            }.onError {
                loadFinally.postValue(false)
                AppLog.put("rss获取内容失败", it)
                context.toastOnUi(it.localizedMessage)
            }
    }

    fun loadMore(rssSource: RssSource) {
        isLoading = true
        page++
        val pageUrl = nextPageUrl
        if (!pageUrl.isNullOrEmpty()) {
            Rss.getArticles(viewModelScope, sortName, pageUrl, rssSource, page)
                .onSuccess(Dispatchers.IO) {
                    nextPageUrl = it.second
                    loadMoreSuccess(it.first)
                }
                .onError {
                    it.printOnDebug()
                    loadFinally.postValue(false)
                }
        } else {
            loadFinally.postValue(false)
        }
    }

    private fun loadMoreSuccess(articles: MutableList<RssArticle>) {
        articles.let { list ->
            if (list.isEmpty()) {
                loadFinally.postValue(false)
                return@let
            }
            val firstArticle = list.first()
            val dbArticle = appDb.rssArticleDao
                .get(firstArticle.origin, firstArticle.link)
            if (dbArticle != null) {
                loadFinally.postValue(false)
            } else {
                list.forEach { rssArticle ->
                    rssArticle.order = order--
                }
                appDb.rssArticleDao.insert(*list.toTypedArray())
            }
        }
        isLoading = false
    }

}