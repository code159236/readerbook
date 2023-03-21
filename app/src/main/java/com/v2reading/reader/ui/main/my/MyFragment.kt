package com.v2reading.reader.ui.main.my

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.v2reading.reader.BuildConfig
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseFragment
import com.v2reading.reader.constant.AppConst
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.databinding.FragmentMyConfigBinding
import com.v2reading.reader.help.config.ThemeConfig
import com.v2reading.reader.lib.dialogs.selector
import com.v2reading.reader.lib.prefs.NameListPreference
import com.v2reading.reader.lib.prefs.SwitchPreference
import com.v2reading.reader.lib.prefs.fragment.PreferenceFragment
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.model.UpdateResponse
import com.v2reading.reader.service.WebService
import com.v2reading.reader.ui.about.AboutActivity
import com.v2reading.reader.ui.about.DonateActivity
import com.v2reading.reader.ui.about.ReadRecordActivity
import com.v2reading.reader.ui.book.bookmark.AllBookmarkActivity
import com.v2reading.reader.ui.book.source.manage.BookSourceActivity
import com.v2reading.reader.ui.config.ConfigActivity
import com.v2reading.reader.ui.config.ConfigTag
import com.v2reading.reader.ui.replace.ReplaceRuleActivity
import com.v2reading.reader.ui.widget.dialog.RateDialog
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.NetworkUtils.getRaw
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import constant.DownLoadBy
import constant.UiType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import listener.OnBtnClickListener
import model.UiConfig
import model.UpdateConfig
import update.UpdateAppUtils
import java.util.*


class MyFragment : BaseFragment(R.layout.fragment_my_config) {

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = MyPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    //    override fun onCompatCreateOptionsMenu(menu: Menu) {
//        menuInflater.inflate(R.menu.main_my, menu)
//    }
//
//    override fun onCompatOptionsItemSelected(item: MenuItem) {
//        when (item.itemId) {
//            R.id.menu_help -> {
//                val text = String(requireContext().assets.open("help/appHelp.md").readBytes())
//                showDialogFragment(TextDialog(text, TextDialog.Mode.MD))
//            }
//        }
//    }


    override fun onResume() {
        super.onResume()

        binding.reward.userFragmentCoin.text = CoinUtil.getCoins(requireContext()).toString()
        binding.reward.userFragmentCoinToday.text =
            CoinUtil.getTodayCoins(requireContext()).toString()
        binding.reward.todayReadDuration.text =
            (getPrefLong(
                AppConst.dateOnlyFormat.format(Date()) + PreferKey.todayReadTime,
                0
            ) / 60000).toString()

    }


    /**
     * 配置
     */
    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            putPrefBoolean(PreferKey.webService, WebService.isRun)
            addPreferencesFromResource(R.xml.pref_main)
//            if (AppConfig.isGooglePlay) {
//                findPreference<PreferenceCategory>("aboutCategory")
//                    ?.removePreferenceRecursively("donate")
//            }

            findPreference<Preference>("update")?.let {
                it.summary = "当前版本:${BuildConfig.VERSION_NAME}"
            }
            findPreference<SwitchPreference>("webService")?.onLongClick {
                if (!WebService.isRun) {
                    return@onLongClick false
                }
                context?.selector(arrayListOf("复制地址", "浏览器打开")) { _, i ->
                    when (i) {
                        0 -> context?.sendToClip(it.summary.toString())
                        1 -> context?.openUrl(it.summary.toString())
                    }
                }
                true
            }
            observeEventSticky<String>(EventBus.WEB_SERVICE) {
                findPreference<SwitchPreference>(PreferKey.webService)?.let {
                    it.isChecked = WebService.isRun
                    it.summary = if (WebService.isRun) {
                        WebService.hostAddress
                    } else {
                        getString(R.string.web_service_desc)
                    }
                }
            }
            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.webService -> {
                    if (requireContext().getPrefBoolean("webService")) {
                        WebService.start(requireContext())
                    } else {
                        WebService.stop(requireContext())
                    }
                }
                "recordLog" -> LogUtils.upLevel()
            }
        }

        /****************
         *
         * 发起添加群流程。群号：全网小说动漫听书全能(933390835) 的 key 为： GkLHePGnKvhaEXQExI__gQyc_-OIAJAG
         * 调用 joinQQGroup(GkLHePGnKvhaEXQExI__gQyc_-OIAJAG) 即可发起手Q客户端申请加群 全网小说动漫听书全能(933390835)
         *
         * @param key 由官网生成的key
         * @return 返回true表示呼起手Q成功，返回false表示呼起失败
         */
        fun joinQQGroup(key: String): Boolean {
            val intent = Intent()
            intent.data =
                Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key")
            // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                startActivity(intent)
                true
            } catch (e: java.lang.Exception) {
                // 未安装手Q或安装的版本不支持
                toastOnUi("尚未安装手机QQ")
                false
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "replaceManage" -> startActivity<ReplaceRuleActivity>()
                "bookmark" -> startActivity<AllBookmarkActivity>()
                "setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.OTHER_CONFIG)
                }
                "qq" -> joinQQGroup("GkLHePGnKvhaEXQExI__gQyc_-OIAJAG")
                "web_dav_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.BACKUP_CONFIG)
                }
                "theme_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.THEME_CONFIG)
                }
                "readRecord" -> startActivity<ReadRecordActivity>()
                "donate" -> startActivity<DonateActivity>()
                "about" -> startActivity<AboutActivity>()
                "review" -> showDialogFragment(RateDialog())
//                "coin_shop" -> {
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        val tag = try {
//                            AppConst.dateOnlyFormat.format(Date(URL("https://www.baidu.com").openConnection().date)) == AppConst.dateOnlyFormat.format(
//                                Date()
//                            )
//                        } catch (e: Exception) {
//                            false
//                        }
//
//                        if (!tag) {
//                            toastOnUi(R.string.get_time_error)
//                            return@launch
//                        }
//
//                        launch(Dispatchers.Main) {
//                            startActivity<ShopActivity>()
//                        }
//
//                    }
//                }
//                "treasure_center" -> {
//
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        val tag = try {
//                            AppConst.dateOnlyFormat.format(Date(URL("https://www.baidu.com").openConnection().date)) == AppConst.dateOnlyFormat.format(
//                                Date()
//                            )
//                        } catch (e: Exception) {
//                            false
//                        }
//
//                        if (!tag) {
//                            toastOnUi(R.string.get_time_error)
//                            return@launch
//                        }
//
//                        launch(Dispatchers.Main) {
//                            startActivity<WelfareCenterActivity>()
//                        }
//
//                    }
//
//                }
                "update" -> {
                    checkUpdate()
                }
                "web" -> {
                    launchUrl("https://v2reading.com")
                }
                "xieyi" -> {
                    launchUrl("https://v2reading.com/user_agreed.html")
                }
                "yinsi" -> {
                    launchUrl("https://v2reading.com/privacy_agreed.html")
                }
                "share" -> {
                    context?.share(
                        "刚发现一款全网小说都能看的app,分享给你了,网站地址是https://v2reading.com ,浏览器打开网站点击下载安装就行",
                        getString(R.string.app_name)
                    )
                }
            }
            return super.onPreferenceTreeClick(preference)
        }

        private val customTabsIntent by lazy {
            CustomTabsIntent.Builder().apply {
                setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
                setColorSchemeParams(
                    CustomTabsIntent.COLOR_SCHEME_LIGHT,
                    CustomTabColorSchemeParams.Builder().apply {
                        setToolbarColor(
                            ContextCompat.getColor(
                                context!!,
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
                                context!!,
                                R.color.primary
                            )
                        )
                    }.build()
                )
            }.build()
        }

        private fun launchUrl(uri: String) = try {
            customTabsIntent.launchUrl(context!!, uri.toUri())
        } catch (_: ActivityNotFoundException) {

        }

        private fun checkUpdate() {

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = getRaw("reader/newversion.json")
                    val updateBean: UpdateResponse? =
                        GSON.fromJsonObject<UpdateResponse>(response).getOrNull()

//                    putPrefBoolean(PreferKey.globalAd, updateBean?.ad == 1)

                    if ((updateBean?.versionCode ?: 0) > BuildConfig.VERSION_CODE) {

                        launch(Dispatchers.Main) {

                            UpdateAppUtils
                                .getInstance()
                                .apkUrl(updateBean?.url ?: "")
                                .updateTitle(updateBean?.updateTitle ?: "")
                                .updateContent(updateBean?.updateContent ?: "")
                                .updateConfig(UpdateConfig().apply {
                                    downloadBy = DownLoadBy.APP
                                    serverVersionName = updateBean?.versionName ?: ""
                                    serverVersionCode = updateBean?.versionCode ?: 0
                                    checkWifi = true
                                    force =
                                        updateBean?.forceVersionCode!! > BuildConfig.VERSION_CODE
                                })
                                .uiConfig(
                                    UiConfig(
                                        uiType = UiType.PLENTIFUL,
                                        updateLogoImgRes = R.mipmap.ic_launcher
                                    )
                                )
                                // 设置 取消 按钮点击事件
                                .setCancelBtnClickListener(object : OnBtnClickListener {
                                    override fun onClick(): Boolean {

                                        return false // 事件是否消费，是否需要传递下去。false-会执行原有点击逻辑，true-只执行本次设置的点击逻辑
                                    }
                                })
                                // 设置 立即更新 按钮点击事件
                                .setUpdateBtnClickListener(object : OnBtnClickListener {
                                    override fun onClick(): Boolean {

                                        return false // 事件是否消费，是否需要传递下去。false-会执行原有点击逻辑，true-只执行本次设置的点击逻辑
                                    }
                                })
                                .update()
                        }

                    } else {
                        toastOnUi("已是最新版本")
                    }

                } catch (e: Exception) {

                }
            }

        }

    }


}