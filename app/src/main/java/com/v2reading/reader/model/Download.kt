package com.v2reading.reader.model

import android.content.Context
import com.v2reading.reader.constant.IntentAction
import com.v2reading.reader.service.DownloadService
import com.v2reading.reader.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}