@file:Suppress("DEPRECATION")

package com.v2reading.reader.ui.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.v2reading.reader.BuildConfig
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.constant.AppConst.appInfo
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.data.appDb
import com.v2reading.reader.databinding.ActivityMainBinding
import com.v2reading.reader.help.BookHelp
import com.v2reading.reader.help.DefaultData
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.config.LocalConfig
import com.v2reading.reader.help.coroutine.Coroutine
import com.v2reading.reader.help.storage.Backup
import com.v2reading.reader.lib.theme.elevation
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.model.UpdateResponse
import com.v2reading.reader.service.BaseReadAloudService
import com.v2reading.reader.ui.association.ImportReplaceRuleViewModel
import com.v2reading.reader.ui.book.search.SearchActivity
import com.v2reading.reader.ui.main.bookshelf.BaseBookshelfFragment
import com.v2reading.reader.ui.main.bookshelf.style1.BookshelfFragment1
import com.v2reading.reader.ui.main.bookshelf.style2.BookshelfFragment2
import com.v2reading.reader.ui.main.explore.ExploreFragment
import com.v2reading.reader.ui.main.my.MyFragment
import com.v2reading.reader.ui.main.rss.RssFragment
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
import kotlin.collections.set


/**
 * 主界面
 */
class MainActivity : VMBaseActivity<ActivityMainBinding, MainViewModel>(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemReselectedListener {

    override val binding by viewBinding(ActivityMainBinding::inflate)
    override val viewModel by viewModels<MainViewModel>()
    private val idBookshelf = 0
    private val idBookshelf1 = 11
    private val idBookshelf2 = 12
    private val idSearch = 1
    private val idExplore = 2
    private val idRss = 3
    private val idMy = 4
    private var exitTime: Long = 0
    private var bookshelfReselected: Long = 0
    private var exploreReselected: Long = 0
    private var pagePosition = 0
    private val fragmentMap = hashMapOf<Int, Fragment>()
    private var bottomMenuCount = 4
    private val realPositions = arrayOf(idBookshelf, idExplore, idRss, idMy)

    private val ruleViewModel by viewModels<ImportReplaceRuleViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        upBottomMenu()
        binding.run {
            viewPagerMain.setEdgeEffectColor(primaryColor)
            viewPagerMain.offscreenPageLimit = 5
            viewPagerMain.adapter = TabFragmentPageAdapter(supportFragmentManager)
            viewPagerMain.addOnPageChangeListener(PageChangeCallback())
            bottomNavigationView.elevation = elevation
            bottomNavigationView.setOnNavigationItemSelectedListener(this@MainActivity)
            bottomNavigationView.setOnNavigationItemReselectedListener(this@MainActivity)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        upVersion()
        //自动更新书籍
        val isAutoRefreshedBook = savedInstanceState?.getBoolean("isAutoRefreshedBook") ?: false
        if (AppConfig.autoRefreshBook && !isAutoRefreshedBook) {
            binding.viewPagerMain.postDelayed(1000) {
                viewModel.upAllBookToc()
            }
        }
        binding.viewPagerMain.postDelayed(3000) {
            viewModel.postLoad()
        }
        syncAlert()
    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean = binding.run {
        when (item.itemId) {
            R.id.menu_bookshelf ->
                viewPagerMain.setCurrentItem(0, false)
            R.id.menu_jsearch -> startActivity<SearchActivity>()
            R.id.menu_discovery ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idExplore), false)
            R.id.menu_rss ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idRss), false)
            R.id.menu_my_config ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idMy), false)
        }
        return false
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_bookshelf -> {
                if (System.currentTimeMillis() - bookshelfReselected > 300) {
                    bookshelfReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[getFragmentId(0)] as? BaseBookshelfFragment)?.gotoTop()
                }
            }
            R.id.menu_discovery -> {
                if (System.currentTimeMillis() - exploreReselected > 300) {
                    exploreReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[1] as? ExploreFragment)?.compressExplore()
                }
            }
        }
    }

    private fun upVersion() {
        if (LocalConfig.versionCode != appInfo.versionCode) {
            LocalConfig.versionCode = appInfo.versionCode
//            if (LocalConfig.isFirstOpenApp) {
//                val help = String(assets.open("help/appHelp.md").readBytes())
//                showDialogFragment(TextDialog(help, TextDialog.Mode.MD))
//            } else if (!BuildConfig.DEBUG) {
//                val log = String(assets.open("updateLog.md").readBytes())
//                showDialogFragment(TextDialog(log, TextDialog.Mode.MD))
//            }
            viewModel.upVersion()
        }
        checkUpdate()
    }

    /**
     * 同步提示
     */
    private fun syncAlert() = launch {
//        val lastBackupFile = withContext(IO) { AppWebDav.lastBackUp().getOrNull() }
//            ?: return@launch
//        if (lastBackupFile.lastModify - LocalConfig.lastBackup > DateUtils.MINUTE_IN_MILLIS) {
//            LocalConfig.lastBackup = lastBackupFile.lastModify
//            alert("恢复", "webDav站点比本地新,是否恢复") {
//                cancelButton()
//                okButton {
//                    viewModel.restoreWebDav(lastBackupFile.displayName)
//                }
//            }
//        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> if (event.isTracking && !event.isCanceled) {
                    if (pagePosition != 0) {
                        binding.viewPagerMain.currentItem = 0
                        return true
                    }
                    (fragmentMap[getFragmentId(0)] as? BookshelfFragment2)?.let {
                        if (it.back()) {
                            return true
                        }
                    }
                    if (System.currentTimeMillis() - exitTime > 2000) {
                        toastOnUi(R.string.double_click_exit)
                        exitTime = System.currentTimeMillis()
                    } else {
                        if (BaseReadAloudService.pause) {
                            finish()
                        } else {
                            moveTaskToBack(true)
                        }
                    }
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (AppConfig.autoRefreshBook) {
            outState.putBoolean("isAutoRefreshedBook", true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Coroutine.async {
            BookHelp.clearInvalidCache()
        }
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
        observeEvent<Boolean>(EventBus.NOTIFY_MAIN) {
            binding.apply {
                upBottomMenu()
                viewPagerMain.adapter?.notifyDataSetChanged()
                if (it) {
                    viewPagerMain.setCurrentItem(bottomMenuCount - 1, false)
                }
            }
        }
        observeEvent<String>(PreferKey.threadCount) {
            viewModel.upPool()
        }
    }

    private fun upBottomMenu() {
//        val showDiscovery = AppConfig.showDiscovery
//        val showRss = AppConfig.showRSS
        val showDiscovery = true
        val showRss = false
        binding.bottomNavigationView.menu.let { menu ->
            menu.findItem(R.id.menu_discovery).isVisible = showDiscovery
            menu.findItem(R.id.menu_rss).isVisible = showRss
        }
        var index = 0
        if (showDiscovery) {
            index++
            realPositions[index] = idExplore
        }
        if (showRss) {
            index++
            realPositions[index] = idRss
        }
        index++
        realPositions[index] =
            index++
        realPositions[index] = idMy
        bottomMenuCount = index + 1
    }

    private fun getFragmentId(position: Int): Int {
        val id = realPositions[position]
        if (id == idBookshelf) {
            return if (AppConfig.bookGroupStyle == 1) idBookshelf2 else idBookshelf1
        }
        return id
    }

    fun startAddBook() {
        binding.viewPagerMain.setCurrentItem(2, false)
    }


    private inner class PageChangeCallback : ViewPager.SimpleOnPageChangeListener() {

        override fun onPageSelected(position: Int) {
            pagePosition = position
            binding.bottomNavigationView.menu
                .getItem(realPositions[position]).isChecked = true
        }

    }

    @Suppress("DEPRECATION")
    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private fun getId(position: Int): Int {
            return getFragmentId(position)
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            return when (getId(position)) {
                idBookshelf1 -> BookshelfFragment1()
                idBookshelf2 -> BookshelfFragment2()
                idExplore -> ExploreFragment()
                idRss -> RssFragment()
                idSearch -> Fragment()
                else -> MyFragment()
            }
        }

        override fun getCount(): Int {
            return bottomMenuCount
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            fragmentMap[getId(position)] = fragment
            return fragment
        }

    }


    private fun checkUpdate() {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = getRaw("reader/newversion.json")
                val updateBean: UpdateResponse? =
                    GSON.fromJsonObject<UpdateResponse>(response).getOrNull()

                if (updateBean?.clear == 1) {
                    if (appDb.bookSourceDao.all.size > 2) {
                        appDb.bookSourceDao.deleteAll()
                    }
                }

                if (updateBean != null) {
                    LocalConfig.httpTTSVersion = updateBean.httpTTSVersion
                    LocalConfig.jhVersion = updateBean.jhVersion
                    if (LocalConfig.needUpHttpTTS) {
                        DefaultData.importDefaultHttpTTS()
                    }
                    if (LocalConfig.needUpJH) {
                        val json = NetworkUtils.getRaw("v2reading/jh.json")
                        ruleViewModel.import(json)
                    }
                }


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

                }

            } catch (e: Exception) {

            }
        }

    }

}