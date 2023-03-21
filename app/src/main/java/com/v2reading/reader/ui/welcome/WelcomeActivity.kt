package com.v2reading.reader.ui.welcome

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.widget.TextView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.huawei.hms.analytics.HiAnalytics
import com.huawei.hms.analytics.HiAnalyticsTools
import com.tradplus.ads.base.bean.TPAdError
import com.tradplus.ads.base.bean.TPAdInfo
import com.tradplus.ads.base.bean.TPBaseAd
import com.tradplus.ads.open.TradPlusSdk
import com.tradplus.ads.open.splash.SplashAdListener
import com.tradplus.ads.open.splash.TPSplash
import com.v2cross.app.Intro
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseActivity
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.databinding.ActivityWelcomeBinding
import com.v2reading.reader.databinding.PrivacyPolicyBinding
import com.v2reading.reader.help.config.LocalConfig
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.ui.book.read.ReadBookActivity
import com.v2reading.reader.ui.main.MainActivity
import com.v2reading.reader.utils.getPrefBoolean
import com.v2reading.reader.utils.startActivity
import com.v2reading.reader.utils.toastOnUi
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding


class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {


    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            privacyPolicy()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    /**
     * 广告展示完毕时，从广告界面跳转至App主界面
     */
    private fun jump() {

        TradPlusSdk.initSdk(this, "A66C8A56933EA3D3E0C5604D3B509522")

//        TestDeviceUtil.getInstance().isNeedTestDevice = true

        //Pangle和Google Admob广告平台要求传入acitivity
        //Pangle和Google Admob广告平台要求传入acitivity
        val tpSplash = TPSplash(this, "55928BC2C4411E4F7B3B866F1DA4A30D")
        tpSplash.setAdListener(object:SplashAdListener(){

            override fun onAdLoaded(p0: TPAdInfo?, p1: TPBaseAd?) {
                super.onAdLoaded(p0, p1)
                tpSplash.showAd(binding.adContainer)
            }

            override fun onAdLoadFailed(p0: TPAdError?) {
                super.onAdLoadFailed(p0)
                startMainActivity()

            }

            override fun onAdClosed(p0: TPAdInfo?) {
                super.onAdClosed(p0)
                binding.adContainer.removeAllViews()
                startMainActivity()
            }


        })
        tpSplash.loadAd(null)
    }

    /**
     * 用户隐私与协议
     */
    private fun privacyPolicy() {
        if (LocalConfig.privacyPolicyOk) {
            jump()
        } else {
            val dialog = alert("") {
                val alertBinding = PrivacyPolicyBinding.inflate(layoutInflater)
                customView {
                    alertBinding.root
                }

            }

            dialog.apply {
                findViewById<TextView>(R.id.tv_agree)?.setOnClickListener {
                    LocalConfig.privacyPolicyOk = true
                    dismiss()
                    if (!TradPlusSdk.isPrivacyUserAgree()) {
                        TradPlusSdk.setPrivacyUserAgree(true)
                    }
                    jump()
                }
                findViewById<TextView>(R.id.tv_cancel)?.setOnClickListener {
                    finish()
                }
                findViewById<TextView>(R.id.tv_xieyi)?.setOnClickListener {
                    launchUrl("https://v2reading.com/user_agreed.html")
                }
                findViewById<TextView>(R.id.tv_yinsi)?.setOnClickListener {
                    launchUrl("https://v2reading.com/privacy_agreed.html")
                }
                setCancelable(false)
            }
        }

    }

    private val screenOrientation: Int
        private get() {
            val config = resources.configuration
            return if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    private val customTabsIntent by lazy {
        CustomTabsIntent.Builder().apply {
            setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            setColorSchemeParams(
                CustomTabsIntent.COLOR_SCHEME_LIGHT,
                CustomTabColorSchemeParams.Builder().apply {
                    setToolbarColor(
                        ContextCompat.getColor(
                            this@WelcomeActivity,
                            R.color.primary
                        )
                    )
                }.build()
            )
            setColorSchemeParams(
                CustomTabsIntent.COLOR_SCHEME_DARK,
                CustomTabColorSchemeParams.Builder().apply {
                    setToolbarColor(
                        ContextCompat.getColor(
                            this@WelcomeActivity,
                            R.color.primary
                        )
                    )
                }.build()
            )
        }.build()
    }

    private fun launchUrl(uri: String) = try {
        customTabsIntent.launchUrl(this, uri.toUri())
    } catch (_: ActivityNotFoundException) {

    }

    private fun startMainActivity() {

//        getPrefBoolean("first_open", true).let {
//            if (it) {
//                putPrefBoolean("first_open", false)
//                startActivity<IntroActivity>()
//                finish()
//                return
//            }
//        }

        if (!Intro.checkSha1(this)) {
            toastOnUi("软件已被修改,请下载安装正版软件")
            return
        }

        HiAnalyticsTools.enableLog()
        HiAnalytics.getInstance(this)

        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead)) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}