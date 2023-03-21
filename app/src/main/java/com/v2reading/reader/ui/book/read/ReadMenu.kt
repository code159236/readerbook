package com.v2reading.reader.ui.book.read

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.v2reading.reader.R
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.databinding.ViewReadMenuBinding
import com.v2reading.reader.help.IntentData
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.config.LocalConfig
import com.v2reading.reader.help.config.ReadBookConfig
import com.v2reading.reader.help.config.ThemeConfig
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.theme.*
import com.v2reading.reader.model.ReadBook
import com.v2reading.reader.ui.book.info.BookInfoActivity
import com.v2reading.reader.ui.browser.WebViewActivity
import com.v2reading.reader.ui.widget.seekbar.SeekBarChangeListener
import com.v2reading.reader.utils.*
import splitties.views.*

/**
 * 阅读界面菜单
 */
class ReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var cnaShowMenu: Boolean = false
    private val callBack: CallBack get() = activity as CallBack
    private val binding = ViewReadMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var menuTopIn: Animation
    private lateinit var menuTopOut: Animation
    private lateinit var menuBottomIn: Animation
    private lateinit var menuBottomOut: Animation
    private val bgColor: Int = context.bottomBackground
    private val textColor: Int = context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
    private val bottomBackgroundList: ColorStateList = Selector.colorBuild()
        .setDefaultColor(bgColor)
        .setPressedColor(ColorUtils.darkenColor(bgColor))
        .create()
    private var onMenuOutEnd: (() -> Unit)? = null

    private val sourceMenu by lazy {
        PopupMenu(context, binding.tvSourceAction).apply {
            inflate(R.menu.book_read_source)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_edit_source -> callBack.openSourceEditActivity()
                    R.id.menu_disable_source -> callBack.disableSource()
                }
                true
            }
        }
    }

    init {
        initView()
        bindEvent()
    }

    private fun initView() = binding.run {
        if (AppConfig.isNightTheme) {
            fabNightTheme.setImageResource(R.drawable.ic_daytime)
        } else {
            fabNightTheme.setImageResource(R.drawable.ic_brightness)
        }
        initAnimation()
        val brightnessBackground = GradientDrawable()
        brightnessBackground.cornerRadius = 5F.dpToPx()
        brightnessBackground.setColor(ColorUtils.adjustAlpha(bgColor, 0.5f))
//        llBrightness.background = brightnessBackground
        llBottomBg.setBackgroundColor(bgColor)
        fabSearch.backgroundTintList = bottomBackgroundList
        fabSearch.setColorFilter(textColor)
        fabAutoPage.backgroundTintList = bottomBackgroundList
        fabAutoPage.setColorFilter(textColor)
        fabReplaceRule.backgroundTintList = bottomBackgroundList
        fabReplaceRule.setColorFilter(textColor)
        fabNightTheme.backgroundTintList = bottomBackgroundList
        fabNightTheme.setColorFilter(textColor)
        tvPre.setTextColor(textColor)
        tvNext.setTextColor(textColor)
        ivCatalog.setColorFilter(textColor)
        tvCatalog.setTextColor(textColor)
        ivReadAloud.setColorFilter(textColor)
        tvReadAloud.setTextColor(textColor)
        ivFont.setColorFilter(textColor)
        tvFont.setTextColor(textColor)
        ivSetting.setColorFilter(textColor)
        tvSetting.setTextColor(textColor)
        vwBg.setOnClickListener(null)
    }



    fun runMenuIn() {
        this.visible()
        binding.titleBar.visible()
        binding.bottomMenu.visible()
        binding.titleBar.startAnimation(menuTopIn)
        binding.bottomMenu.startAnimation(menuBottomIn)
    }

    fun runMenuOut(onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            binding.titleBar.startAnimation(menuTopOut)
            binding.bottomMenu.startAnimation(menuBottomOut)
        }
    }


    private fun bindEvent() = binding.run {
        titleBar.toolbar.setOnClickListener {
            ReadBook.book?.let {
                context.startActivity<BookInfoActivity> {
                    putExtra("name", it.name)
                    putExtra("author", it.author)
                }
            }
        }
        val chapterViewClickListener = OnClickListener {
            if (ReadBook.isLocalBook) {
                return@OnClickListener
            }
            if (AppConfig.readUrlInBrowser) {
                context.openUrl(tvChapterUrl.text.toString().substringBefore(",{"))
            } else {
                context.startActivity<WebViewActivity> {
                    val url = tvChapterUrl.text.toString()
                    putExtra("title", tvChapterName.text)
                    putExtra("url", url)
                    IntentData.put(url, ReadBook.bookSource?.getHeaderMap(true))
                }
            }
        }
        val chapterViewLongClickListener = OnLongClickListener {
            if (ReadBook.isLocalBook) {
                return@OnLongClickListener true
            }
            context.alert(R.string.open_fun) {
                setMessage(R.string.use_browser_open)
                okButton {
                    AppConfig.readUrlInBrowser = true
                }
                noButton {
                    AppConfig.readUrlInBrowser = false
                }
            }
            true
        }
        tvChapterName.setOnClickListener(chapterViewClickListener)
        tvChapterName.setOnLongClickListener(chapterViewLongClickListener)
        tvChapterUrl.setOnClickListener(chapterViewClickListener)
        tvChapterUrl.setOnLongClickListener(chapterViewLongClickListener)
        //登录
        tvLogin.setOnClickListener {
            callBack.showLogin()
        }
        //购买
        tvPay.setOnClickListener {
            callBack.payAction()
        }
        //站点操作
        tvSourceAction.onClick {
            sourceMenu.show()
        }

        //阅读进度
        seekReadPage.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReadBook.skipToPage(seekBar.progress)
            }

        })

        //搜索
        fabSearch.setOnClickListener {
            runMenuOut {
                callBack.openSearchActivity(null)
            }
        }

        //自动翻页
        fabAutoPage.setOnClickListener {
            runMenuOut {
                callBack.autoPage()
            }
        }

        //替换
        fabReplaceRule.setOnClickListener { callBack.openReplaceRule() }

        //夜间模式
        fabNightTheme.setOnClickListener {
            AppConfig.isNightTheme = !AppConfig.isNightTheme
            ThemeConfig.applyDayNight(context)
        }

        //上一章
        tvPre.setOnClickListener { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }

        //下一章
        tvNext.setOnClickListener { ReadBook.moveToNextChapter(true) }

        //目录
        llCatalog.setOnClickListener {
            runMenuOut {
                callBack.openChapterList()
            }
        }

        //亮度
        llBrightness.setOnClickListener {
            runMenuOut {
                callBack.showReadBrightnessStyle()
            }
        }
        //朗读
        llReadAloud.setOnClickListener {
            runMenuOut {
                callBack.onClickReadAloud()
            }
        }
        llReadAloud.onLongClick {
            runMenuOut { callBack.showReadAloudDialog() }
        }
        //界面
        llFont.setOnClickListener {
            runMenuOut {
                callBack.showReadStyle()
            }
        }

        //设置
        llSetting.setOnClickListener {
            runMenuOut {
                callBack.showMoreSetting()
            }
        }

    }

    private fun initAnimation() {
        //显示菜单
        menuTopIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_in)
        menuBottomIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_in)
        menuTopIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.tvSourceAction.isGone = ReadBook.isLocalBook
                binding.tvLogin.isGone = ReadBook.bookSource?.loginUrl.isNullOrEmpty()
                binding.tvPay.isGone = ReadBook.bookSource?.loginUrl.isNullOrEmpty()
                        || ReadBook.curTextChapter?.isVip != true
                        || ReadBook.curTextChapter?.isPay == true
                callBack.upSystemUiVisibility()
//                binding.llBrightness.visible(showBrightnessView)
            }

            @SuppressLint("RtlHardcoded")
            override fun onAnimationEnd(animation: Animation) {
                val navigationBarHeight =
                    if (ReadBookConfig.hideNavigationBar) {
                        activity?.navigationBarHeight ?: 0
                    } else {
                        0
                    }
                binding.run {
                    vwMenuBg.setOnClickListener { runMenuOut() }
                    root.padding = 0
                    when (activity?.navigationBarGravity) {
                        Gravity.BOTTOM -> root.bottomPadding = navigationBarHeight
                        Gravity.LEFT -> root.leftPadding = navigationBarHeight
                        Gravity.RIGHT -> root.rightPadding = navigationBarHeight
                    }
                }
                callBack.upSystemUiVisibility()
//                if (!LocalConfig.readMenuHelpVersionIsLast) {
//                    callBack.showReadMenuHelp()
//                }
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })

        //隐藏菜单
        menuTopOut = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_out)
        menuBottomOut =
            AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_out)
        menuTopOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.vwMenuBg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                this@ReadMenu.invisible()
                binding.titleBar.invisible()
                binding.bottomMenu.invisible()
                cnaShowMenu = false
                onMenuOutEnd?.invoke()
                callBack.upSystemUiVisibility()
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })
    }

    fun upBookView() {
        binding.titleBar.title = ReadBook.book?.name
        ReadBook.curTextChapter?.let {
            binding.tvChapterName.text = it.title
            binding.tvChapterName.visible()
            if (!ReadBook.isLocalBook) {
                binding.tvChapterUrl.text = it.url
                binding.tvChapterUrl.visible()
            } else {
                binding.tvChapterUrl.gone()
            }
            binding.seekReadPage.max = it.pageSize.minus(1)
            binding.seekReadPage.progress = ReadBook.durPageIndex
            binding.tvPre.isEnabled = ReadBook.durChapterIndex != 0
            binding.tvNext.isEnabled = ReadBook.durChapterIndex != ReadBook.chapterSize - 1
        } ?: let {
            binding.tvChapterName.gone()
            binding.tvChapterUrl.gone()
        }
    }

    fun setSeekPage(seek: Int) {
        binding.seekReadPage.progress = seek
    }

    fun setAutoPage(autoPage: Boolean) = binding.run {
        if (autoPage) {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page_stop)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
        fabAutoPage.setColorFilter(textColor)
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun openSourceEditActivity()
        fun showReadStyle()
        fun showReadBrightnessStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showReadMenuHelp()
        fun showLogin()
        fun payAction()
        fun disableSource()
    }

}
