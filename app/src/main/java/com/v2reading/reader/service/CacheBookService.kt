package com.v2reading.reader.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseService
import com.v2reading.reader.constant.AppConst
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.constant.IntentAction
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.model.CacheBook
import com.v2reading.reader.ui.book.cache.CacheActivity
import com.v2reading.reader.utils.activityPendingIntent
import com.v2reading.reader.utils.postEvent
import com.v2reading.reader.utils.servicePendingIntent
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import kotlin.math.min

class CacheBookService : BaseService() {

    companion object {
        var isRun = false
            private set
    }

    private val threadCount = AppConfig.threadCount
    private var cachePool =
        Executors.newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    private var downloadJob: Job? = null

    private val notificationBuilder by lazy {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.offline_cache))
            .setContentIntent(activityPendingIntent<CacheActivity>("cacheActivity"))
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            servicePendingIntent<CacheBookService>(IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onCreate() {
        super.onCreate()
        isRun = true
        upNotification(getString(R.string.starting_download))
        launch {
            while (isActive) {
                delay(1000)
                upNotification(CacheBook.downloadSummary)
                postEvent(EventBus.UP_DOWNLOAD, "")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                IntentAction.start -> addDownloadData(
                    intent.getStringExtra("bookUrl"),
                    intent.getIntExtra("start", 0),
                    intent.getIntExtra("end", 0)
                )
                IntentAction.remove -> removeDownload(intent.getStringExtra("bookUrl"))
                IntentAction.stop -> stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRun = false
        cachePool.close()
        CacheBook.cacheBookMap.clear()
        super.onDestroy()
        postEvent(EventBus.UP_DOWNLOAD, "")
    }

    private fun addDownloadData(bookUrl: String?, start: Int, end: Int) {
        bookUrl ?: return
        execute {
            val cacheBook = CacheBook.getOrCreate(bookUrl) ?: return@execute
            cacheBook.addDownload(start, end)
            upNotification(CacheBook.downloadSummary)
            if (downloadJob == null) {
                download()
            }
        }
    }

    private fun removeDownload(bookUrl: String?) {
        CacheBook.cacheBookMap[bookUrl]?.stop()
        if (CacheBook.cacheBookMap.isEmpty()) {
            stopSelf()
        }
    }

    private fun download() {
        downloadJob?.cancel()
        downloadJob = launch(cachePool) {
            while (isActive) {
                if (!CacheBook.isRun) {
                    CacheBook.stop(this@CacheBookService)
                    return@launch
                }
                CacheBook.cacheBookMap.forEach {
                    while (CacheBook.onDownloadCount > threadCount) {
                        delay(100)
                    }
                    it.value.download(this, cachePool)
                }
            }
        }
    }

    /**
     * 更新通知
     */
    private fun upNotification(notificationContent: String) {
        notificationBuilder.setContentText(notificationContent)
        val notification = notificationBuilder.build()
        startForeground(AppConst.notificationIdCache, notification)
    }

}