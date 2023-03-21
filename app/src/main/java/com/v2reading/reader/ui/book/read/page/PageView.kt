package com.v2reading.reader.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import com.tradplus.ads.base.bean.TPAdError
import com.tradplus.ads.base.bean.TPAdInfo
import com.tradplus.ads.base.bean.TPBaseAd
import com.tradplus.ads.open.nativead.NativeAdListener
import com.tradplus.ads.open.nativead.TPNative
import com.v2reading.reader.R
import com.v2reading.reader.constant.AppConst.timeFormat
import com.v2reading.reader.data.entities.Bookmark
import com.v2reading.reader.databinding.ViewBookPageBinding
import com.v2reading.reader.help.config.ReadBookConfig
import com.v2reading.reader.help.config.ReadTipConfig
import com.v2reading.reader.model.ReadBook
import com.v2reading.reader.ui.book.read.ReadBookActivity
import com.v2reading.reader.ui.book.read.page.entities.TextPage
import com.v2reading.reader.ui.book.read.page.provider.ChapterProvider
import com.v2reading.reader.ui.widget.BatteryView
import com.v2reading.reader.utils.*
import splitties.views.backgroundColor
import java.util.*

/**
 * 阅读界面
 */
class PageView(context: Context) : FrameLayout(context) {

    private val binding = ViewBookPageBinding.inflate(LayoutInflater.from(context), this, true)
    private val readBookActivity get() = activity as? ReadBookActivity
    private var battery = 100
    private var tvTitle: BatteryView? = null
    private var tvTime: BatteryView? = null
    private var tvBattery: BatteryView? = null
    private var tvBatteryP: BatteryView? = null
    private var tvPage: BatteryView? = null
    private var tvTotalProgress: BatteryView? = null
    private var tvPageAndTotal: BatteryView? = null
    private var tvBookName: BatteryView? = null
    private var tvTimeBattery: BatteryView? = null
    private var tvTimeBatteryP: BatteryView? = null
    var current = false

    val headerHeight: Int
        get() {
            val h1 = if (ReadBookConfig.hideStatusBar) 0 else context.statusBarHeight
            val h2 = if (binding.llHeader.isGone) 0 else binding.llHeader.height
            return h1 + h2
        }

    init {
        if (!isInEditMode) {
            //设置背景防止切换背景时文字重叠
            setBackgroundColor(context.getCompatColor(R.color.background))
            upStyle()
        }
        binding.contentTextView.upView = {
            setProgress(it)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        upBg()
    }

    fun upStyle() = binding.run {
        upTipStyle()
        ReadBookConfig.let {
            val tipColor = with(ReadTipConfig) {
                if (tipColor == 0) it.textColor else tipColor
            }
            tvHeaderLeft.setColor(tipColor)
            tvHeaderMiddle.setColor(tipColor)
            tvHeaderRight.setColor(tipColor)
            tvFooterLeft.setColor(tipColor)
            tvFooterMiddle.setColor(tipColor)
            tvFooterRight.setColor(tipColor)
            upStatusBar()
            llHeader.setPadding(
                it.headerPaddingLeft.dpToPx(),
                it.headerPaddingTop.dpToPx(),
                it.headerPaddingRight.dpToPx(),
                it.headerPaddingBottom.dpToPx()
            )
            llFooter.setPadding(
                it.footerPaddingLeft.dpToPx(),
                it.footerPaddingTop.dpToPx(),
                it.footerPaddingRight.dpToPx(),
                it.footerPaddingBottom.dpToPx()
            )
            vwTopDivider.visible(it.showHeaderLine)
            vwBottomDivider.visible(it.showFooterLine)
        }
        contentTextView.upVisibleRect()
        upTime()
        upBattery(battery)

    }

    /**
     * 显示状态栏时隐藏header
     */
    fun upStatusBar() = with(binding.vwStatusBar) {
        setPadding(paddingLeft, context.statusBarHeight, paddingRight, paddingBottom)
        isGone = ReadBookConfig.hideStatusBar || readBookActivity?.isInMultiWindow == true
    }

    private fun upTipStyle() = binding.run {
        tvHeaderLeft.tag = null
        tvHeaderMiddle.tag = null
        tvHeaderRight.tag = null
        tvFooterLeft.tag = null
        tvFooterMiddle.tag = null
        tvFooterRight.tag = null
        llHeader.isGone = when (ReadTipConfig.headerMode) {
            1 -> false
            2 -> true
            else -> !ReadBookConfig.hideStatusBar
        }
        llFooter.isGone = when (ReadTipConfig.footerMode) {
            1 -> true
            else -> false
        }
        ReadTipConfig.apply {
            tvHeaderLeft.isGone = tipHeaderLeft == none
            tvHeaderRight.isGone = tipHeaderRight == none
            tvHeaderMiddle.isGone = tipHeaderMiddle == none
            tvFooterLeft.isInvisible = tipFooterLeft == none
            tvFooterRight.isGone = tipFooterRight == none
//            tvFooterMiddle.isGone = false
        }
        tvTitle = getTipView(ReadTipConfig.chapterTitle)?.apply {
            tag = ReadTipConfig.chapterTitle
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvTime = getTipView(ReadTipConfig.time)?.apply {
            tag = ReadTipConfig.time
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvBattery = getTipView(ReadTipConfig.battery)?.apply {
            tag = ReadTipConfig.battery
            isBattery = true
            textSize = 11f
        }
        tvPage = getTipView(ReadTipConfig.page)?.apply {
            tag = ReadTipConfig.page
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvTotalProgress = getTipView(ReadTipConfig.totalProgress)?.apply {
            tag = ReadTipConfig.totalProgress
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvPageAndTotal = getTipView(ReadTipConfig.pageAndTotal)?.apply {
            tag = ReadTipConfig.pageAndTotal
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvBookName = getTipView(ReadTipConfig.bookName)?.apply {
            tag = ReadTipConfig.bookName
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvTimeBattery = getTipView(ReadTipConfig.timeBattery)?.apply {
            tag = ReadTipConfig.timeBattery
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 11f
        }
        tvBatteryP = getTipView(ReadTipConfig.batteryPercentage)?.apply {
            tag = ReadTipConfig.batteryPercentage
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvTimeBatteryP = getTipView(ReadTipConfig.timeBatteryPercentage)?.apply {
            tag = ReadTipConfig.timeBatteryPercentage
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
    }

    private fun getTipView(tip: Int): BatteryView? = binding.run {
        return when (tip) {
            ReadTipConfig.tipHeaderLeft -> tvHeaderLeft
            ReadTipConfig.tipHeaderMiddle -> tvHeaderMiddle
            ReadTipConfig.tipHeaderRight -> tvHeaderRight
            ReadTipConfig.tipFooterLeft -> tvFooterLeft
            ReadTipConfig.tipFooterMiddle -> tvFooterMiddle
            ReadTipConfig.tipFooterRight -> tvFooterRight
            else -> null
        }
    }

    fun upBg() {
        binding.vwRoot.backgroundColor = ReadBookConfig.bgMeanColor
        binding.vwBg.background = ReadBookConfig.bg
        upBgAlpha()
    }

    fun upBgAlpha() {
        binding.vwBg.alpha = ReadBookConfig.bgAlpha / 100f
    }

    fun upTime() {
        tvTime?.text = timeFormat.format(Date(System.currentTimeMillis()))
        upTimeBattery()
    }

    @SuppressLint("SetTextI18n")
    fun upBattery(battery: Int) {
        this.battery = battery
        tvBattery?.setBattery(battery)
        tvBatteryP?.text = "$battery%"
        upTimeBattery()
    }

    @SuppressLint("SetTextI18n")
    private fun upTimeBattery() {
        tvTimeBattery?.let {
            val time = timeFormat.format(Date(System.currentTimeMillis()))
            it.text = "$time "
            it.gravity = Gravity.CENTER

            when (battery) {
                in 0..10 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_one).apply { tint(it.currentTextColor) }
                in 10..20 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_two).apply { tint(it.currentTextColor) }
                in 20..30 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_three).apply { tint(it.currentTextColor) }
                in 30..40 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_four).apply { tint(it.currentTextColor) }
                in 40..50 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_five).apply { tint(it.currentTextColor) }
                in 50..60 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_six).apply { tint(it.currentTextColor) }
                in 60..70 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_seven).apply { tint(it.currentTextColor) }
                in 70..80 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_eight).apply { tint(it.currentTextColor) }
                in 80..90 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_nine).apply { tint(it.currentTextColor) }
                in 90..100 -> it.drawableStart =
                    drawable(R.drawable.reader_icon_bettery_ten).apply { tint(it.currentTextColor) }
                else -> {
                    it.drawableStart =
                        drawable(R.drawable.reader_icon_bettery_ten).apply { tint(it.currentTextColor) }
                }
            }

            it.compoundDrawablePadding = resources.getDimension(R.dimen.dp_3).toInt()

        }
    }


    val TAG = "read"
    private fun loadNormalNative() {
        tpNative = TPNative(readBookActivity, "91D345DAF9184D6F590010A69060AF82")
        tpNative?.setAdListener(object : NativeAdListener() {
            override fun onAdLoaded(tpAdInfo: TPAdInfo, tpBaseAd: TPBaseAd?) {
                Log.i(TAG, "onAdLoaded: " + tpAdInfo.adSourceName + "加载成功")
            }

            override fun onAdClicked(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdClicked: " + tpAdInfo.adSourceName + "被点击")
            }

            override fun onAdImpression(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdImpression: " + tpAdInfo.adSourceName + "展示")
            }

            override fun onAdShowFailed(tpAdError: TPAdError?, tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdShowFailed: " + tpAdInfo.adSourceName + "展示失败")
            }

            override fun onAdLoadFailed(tpAdError: TPAdError) {
                Log.i(
                    TAG,
                    "onAdLoadFailed: 加载失败 , code : " + tpAdError.getErrorCode() + ", msg :" + tpAdError.getErrorMsg()
                )
            }

            override fun onAdClosed(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdClosed: " + tpAdInfo.adSourceName + "广告关闭")
            }
        })
        tpNative?.loadAd()
    }

    var tpNative: TPNative? = null

    fun setContent(textPage: TextPage, resetPageOffset: Boolean = true) {
        setProgress(textPage)
        if (resetPageOffset) {
            resetPageOffset()
        }
        if (textPage.index == textPage.pageSize - 3 && current) {
            if (tpNative != null) {
                tpNative?.reloadAd()
            } else
                loadNormalNative()
        }
        if (textPage.index == textPage.pageSize - 1 && current) {
            binding.adContainer.isGone = false
            if (tpNative?.isReady == true) {
                tpNative?.showAd(binding.adContainer, R.layout.tp_native_ad_list_item, "")
            }

        } else binding.adContainer.isGone = true

        binding.contentTextView.setContent(textPage)
    }

    fun setContentDescription(content: String) {
        binding.contentTextView.contentDescription = content
    }

    fun resetPageOffset() {
        binding.contentTextView.resetPageOffset()
    }

    @SuppressLint("SetTextI18n")
    fun setProgress(textPage: TextPage) = textPage.apply {
        tvBookName?.text = ReadBook.book?.name
        tvTitle?.text = textPage.title
        tvPage?.text = "${index.plus(1)}/$pageSize"
        tvTotalProgress?.text = readProgress
        tvPageAndTotal?.text = "${index.plus(1)}/$pageSize  $readProgress"
    }

    fun scroll(offset: Int) {
        binding.contentTextView.scroll(offset)
    }

    fun upSelectAble(selectAble: Boolean) {
        binding.contentTextView.selectAble = selectAble
    }

    fun longPress(
        x: Float, y: Float,
        select: (relativePagePos: Int, lineIndex: Int, charIndex: Int) -> Unit,
    ) {
        return binding.contentTextView.longPress(x, y - headerHeight, select)
    }

    fun selectText(
        x: Float, y: Float,
        select: (relativePagePos: Int, lineIndex: Int, charIndex: Int) -> Unit,
    ) {
        return binding.contentTextView.selectText(x, y - headerHeight, select)
    }

    fun selectStartMove(x: Float, y: Float) {
        binding.contentTextView.selectStartMove(x, y - headerHeight)
    }

    fun selectStartMoveIndex(relativePagePos: Int, lineIndex: Int, charIndex: Int) {
        binding.contentTextView.selectStartMoveIndex(relativePagePos, lineIndex, charIndex)
    }

    fun selectEndMove(x: Float, y: Float) {
        binding.contentTextView.selectEndMove(x, y - headerHeight)
    }

    fun selectEndMoveIndex(relativePagePos: Int, lineIndex: Int, charIndex: Int) {
        binding.contentTextView.selectEndMoveIndex(relativePagePos, lineIndex, charIndex)
    }

    fun cancelSelect() {
        binding.contentTextView.cancelSelect()
    }

    fun createBookmark(): Bookmark? {
        return binding.contentTextView.createBookmark()
    }

    fun relativePage(relativePagePos: Int): TextPage {
        return binding.contentTextView.relativePage(relativePagePos)
    }

    val selectedText: String get() = binding.contentTextView.getSelectedText()

    val textPage get() = binding.contentTextView.textPage
}