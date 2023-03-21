package com.v2reading.reader.ui.book.read

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.constant.AppConst.charsets
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.databinding.ActivityBookReadBinding
import com.v2reading.reader.databinding.DialogDownloadChoiceBinding
import com.v2reading.reader.databinding.DialogEditTextBinding
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.config.LocalConfig
import com.v2reading.reader.help.config.ReadBookConfig
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.dialogs.selector
import com.v2reading.reader.lib.theme.ThemeStore
import com.v2reading.reader.lib.theme.backgroundColor
import com.v2reading.reader.lib.theme.bottomBackground
import com.v2reading.reader.model.CacheBook
import com.v2reading.reader.model.ReadBook
import com.v2reading.reader.ui.book.read.config.BgTextConfigDialog
import com.v2reading.reader.ui.book.read.config.ClickActionConfigDialog
import com.v2reading.reader.ui.book.read.config.PaddingConfigDialog
import com.v2reading.reader.ui.document.HandleFileContract
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import java.util.*

/**
 * 阅读界面
 */
abstract class BaseReadBookActivity :
    VMBaseActivity<ActivityBookReadBinding, ReadBookViewModel>(imageBg = false) {

    override val binding by viewBinding(ActivityBookReadBinding::inflate)
    override val viewModel by viewModels<ReadBookViewModel>()
    var bottomDialog = 0
    private val selectBookFolderResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let {
            ReadBook.book?.let { book ->
                viewModel.loadChapterList(book)
            }
        } ?: ReadBook.upMsg("没有权限访问")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ReadBook.msg = null
        setOrientation()
        upLayoutInDisplayCutoutMode()
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.navigationBar.setBackgroundColor(bottomBackground)
        viewModel.permissionDenialLiveData.observe(this) {
            selectBookFolderResult.launch {
                mode = HandleFileContract.DIR_SYS
                title = "选择书籍所在文件夹"
            }
        }
        if (!LocalConfig.readHelpVersionIsLast) {
            showClickRegionalConfig()
        }

    }


    fun showPaddingConfig() {
        showDialogFragment<PaddingConfigDialog>()
    }

    fun showBgTextConfig() {
        showDialogFragment<BgTextConfigDialog>()
    }

    fun showClickRegionalConfig() {
        showDialogFragment<ClickActionConfigDialog>()
    }

    /**
     * 屏幕方向
     */
    @SuppressLint("SourceLockedOrientationActivity")
    fun setOrientation() {
        when (AppConfig.screenOrientation) {
            "0" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            "1" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "2" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "3" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    /**
     * 更新状态栏,导航栏
     */
    fun upSystemUiVisibility(
        isInMultiWindow: Boolean,
        toolBarHide: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.run {
                if (toolBarHide && ReadBookConfig.hideNavigationBar) {
                    hide(WindowInsets.Type.navigationBars())
                } else {
                    show(WindowInsets.Type.navigationBars())
                }
                if (toolBarHide && ReadBookConfig.hideStatusBar) {
                    hide(WindowInsets.Type.statusBars())
                } else {
                    show(WindowInsets.Type.statusBars())
                }
            }
        }
        upSystemUiVisibilityO(isInMultiWindow, toolBarHide)
        if (toolBarHide) {
            setLightStatusBar(ReadBookConfig.durConfig.curStatusIconDark())
        } else {
            val statusBarColor = ThemeStore.statusBarColor(this, AppConfig.isTransparentStatusBar)
            setLightStatusBar(ColorUtils.isColorLight(statusBarColor))
        }
    }

    @Suppress("DEPRECATION")
    private fun upSystemUiVisibilityO(
        isInMultiWindow: Boolean,
        toolBarHide: Boolean = true
    ) {
        var flag = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        if (!isInMultiWindow) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        if (ReadBookConfig.hideNavigationBar) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            if (toolBarHide) {
                flag = flag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        }
        if (ReadBookConfig.hideStatusBar && toolBarHide) {
            flag = flag or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        window.decorView.systemUiVisibility = flag
    }

    override fun upNavigationBarColor() {
        upNavigationBar()
        when {
            binding.readMenu.isVisible -> super.upNavigationBarColor()
            bottomDialog > 0 -> super.upNavigationBarColor()
            !AppConfig.immNavigationBar -> super.upNavigationBarColor()
            else -> setNavigationBarColorAuto(ReadBookConfig.bgMeanColor)
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun upNavigationBar() {
        binding.navigationBar.run {
            if (bottomDialog > 0 || binding.readMenu.isVisible) {
                val navigationBarHeight =
                    if (ReadBookConfig.hideNavigationBar) navigationBarHeight else 0
                when (navigationBarGravity) {
                    Gravity.BOTTOM -> layoutParams =
                        (layoutParams as FrameLayout.LayoutParams).apply {
                            height = navigationBarHeight
                            width = MATCH_PARENT
                            gravity = Gravity.BOTTOM
                        }
                    Gravity.LEFT -> layoutParams =
                        (layoutParams as FrameLayout.LayoutParams).apply {
                            height = MATCH_PARENT
                            width = navigationBarHeight
                            gravity = Gravity.LEFT
                        }
                    Gravity.RIGHT -> layoutParams =
                        (layoutParams as FrameLayout.LayoutParams).apply {
                            height = MATCH_PARENT
                            width = navigationBarHeight
                            gravity = Gravity.RIGHT
                        }
                }
                visible()
            } else {
                gone()
            }
        }
    }

    /**
     * 保持亮屏
     */
    fun keepScreenOn(on: Boolean) {
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * 适配刘海
     */
    private fun upLayoutInDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ReadBookConfig.readBodyToLh) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    fun showDownloadDialog() {
        ReadBook.book?.let { book ->
            alert(titleResource = R.string.offline_cache) {
                val alertBinding = DialogDownloadChoiceBinding.inflate(layoutInflater).apply {
                    root.setBackgroundColor(root.context.backgroundColor)
                    editStart.setText((book.durChapterIndex + 1).toString())
                    editEnd.setText(book.totalChapterNum.toString())
                }
                customView { alertBinding.root }
                yesButton {

                    alertBinding.run {
                        val start = editStart.text!!.toString().let {
                            if (it.isEmpty()) 0 else it.toInt()
                        }
                        val end = editEnd.text!!.toString().let {
                            if (it.isEmpty()) book.totalChapterNum else it.toInt()
                        }
                        toastOnUi("后台已开始自动缓存!")
                        startCache(book, start, end)
                    }

                }
                noButton()
            }
        }
    }

    private fun startCache(book: Book, start: Int, end: Int) {
        CacheBook.start(this, book, start - 1, end - 1)
    }

    fun showCharsetConfig() {
        alert(R.string.set_charset) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "charset"
                editView.setFilterValues(charsets)
                editView.setText(ReadBook.book?.charset)
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    ReadBook.setCharset(it)
                }
            }
            cancelButton()
        }
    }

    fun showPageAnimConfig(success: () -> Unit) {
        val items = arrayListOf<String>()
        items.add(getString(R.string.btn_default_s))
        items.add(getString(R.string.page_anim_simulation))
        items.add(getString(R.string.page_anim_slide))
        items.add(getString(R.string.page_anim_cover))
        items.add(getString(R.string.page_anim_scroll))
        items.add(getString(R.string.page_anim_none))
        selector(R.string.page_anim, items) { _, i ->
            ReadBook.book?.setPageAnim(i - 1)
            success()
        }
    }

    fun isPrevKey(keyCode: Int): Boolean {
        val prevKeysStr = getPrefString(PreferKey.prevKeys)
        return prevKeysStr?.split(",")?.contains(keyCode.toString()) ?: false
    }

    fun isNextKey(keyCode: Int): Boolean {
        val nextKeysStr = getPrefString(PreferKey.nextKeys)
        return nextKeysStr?.split(",")?.contains(keyCode.toString()) ?: false
    }
}