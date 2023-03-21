package com.v2reading.reader

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Process.myPid
import androidx.multidex.MultiDexApplication
import com.github.liuyueyi.quick.transfer.ChineseUtils
import com.github.liuyueyi.quick.transfer.constants.TransType
import com.iyipx.tts.TTSAPP
import com.jeremyliao.liveeventbus.LiveEventBus
import com.v2reading.reader.base.AppContextWrapper
import com.v2reading.reader.constant.AppConst.channelIdDownload
import com.v2reading.reader.constant.AppConst.channelIdReadAloud
import com.v2reading.reader.constant.AppConst.channelIdWeb
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.data.appDb
import com.v2reading.reader.help.*
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.config.LocalConfig
import com.v2reading.reader.help.config.ThemeConfig.applyDayNight
import com.v2reading.reader.help.coroutine.Coroutine
import com.v2reading.reader.help.http.cronet.CronetLoader
import com.v2reading.reader.model.BookCover
import com.v2reading.reader.utils.defaultSharedPreferences
import com.v2reading.reader.utils.getPrefBoolean
import splitties.systemservices.notificationManager
import java.util.concurrent.TimeUnit

class App : MultiDexApplication() {

    private lateinit var oldConfig: Configuration

    override fun onCreate() {
        super.onCreate()

        if (!isMainProcess(this)) {
            return
        }

        instance = this
        oldConfig = Configuration(resources.configuration)
        CrashHandler(this)
        //预下载Cronet so
        CronetLoader.preDownload()
        createNotificationChannels()
        applyDayNight(this)
        LiveEventBus.config()
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)
        registerActivityLifecycleCallbacks(LifecycleHelp)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(AppConfig)
        Coroutine.async {
            //初始化封面
            BookCover.toString()
            //清除过期数据
            appDb.cacheDao.clearDeadline(System.currentTimeMillis())
            if (getPrefBoolean(PreferKey.autoClearExpired, true)) {
                val clearTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                appDb.searchBookDao.clearExpired(clearTime)
            }
            RuleBigDataHelp.clearInvalid()
            BookHelp.clearInvalidCache()
            //初始化简繁转换引擎
            when (AppConfig.chineseConverterType) {
                1 -> ChineseUtils.preLoad(true, TransType.TRADITIONAL_TO_SIMPLE)
                2 -> ChineseUtils.preLoad(true, TransType.SIMPLE_TO_TRADITIONAL)
            }
            //同步阅读记录
            if (AppWebDav.syncBookProgress) {
                AppWebDav.downloadAllBookProgress()
            }
        }
        TTSAPP.init(this)

        if (!LocalConfig.setTTS) {
            AppConfig.ttsEngine =
                "{\"title\": \"内置TTS(联网)\",\"value\": \"com.v2reading.reader\"}"
            LocalConfig.setTTS = true
        }

    }


    private fun isPidOfProcessName(context: Context, pid: Int, p_name: String?): Boolean {
        if (p_name == null) return false
        var isMain = false
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        //遍历所有进程
        for (process in am.runningAppProcesses) {
            if (process.pid === pid) {
                //进程ID相同时判断该进程名是否一致
                if (process.processName.equals(p_name)) {
                    isMain = true
                }
                break
            }
        }
        return isMain
    }


    @Throws(PackageManager.NameNotFoundException::class)
    fun getMainProcessName(context: Context): String? {
        return context.packageManager.getApplicationInfo(context.packageName, 0).processName
    }


    @Throws(PackageManager.NameNotFoundException::class)
    fun isMainProcess(context: Context?): Boolean {
        return isPidOfProcessName(context!!, myPid(), getMainProcessName(context))
    }


    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppContextWrapper.wrap(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val diff = newConfig.diff(oldConfig)
        if ((diff and ActivityInfo.CONFIG_UI_MODE) != 0) {
            applyDayNight(this)
        }
        oldConfig = Configuration(newConfig)
    }

    /**
     * 创建通知ID
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val downloadChannel = NotificationChannel(
            channelIdDownload,
            getString(com.v2reading.reader.R.string.action_download),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val readAloudChannel = NotificationChannel(
            channelIdReadAloud,
            getString(com.v2reading.reader.R.string.read_aloud),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val webChannel = NotificationChannel(
            channelIdWeb,
            getString(com.v2reading.reader.R.string.web_service),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        //向notification manager 提交channel
        notificationManager.createNotificationChannels(
            listOf(
                downloadChannel,
                readAloudChannel,
                webChannel
            )
        )
    }

    companion object {

        var instance: App? = null
        var navigationBarHeight = 0
    }

}
