package com.v2reading.reader.ui.widget.luckypan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import com.v2reading.reader.utils.Util


class LuckPanLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(
    context, attrs, defStyleAttr
) {
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yellowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var radius = 0
    private var CircleX = 0
    private var CircleY = 0
    private var canvas: Canvas? = null
    private var isYellow = false
    private var delayTime = 500
    private var rotatePan: RotatePan? = null
    private var startBtn: ImageView? = null
    private val screenWidth: Int
    private val screeHeight: Int
    private var MinValue = 0
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        MinValue = Math.min(screenWidth, screeHeight)
        MinValue -= Util.dip2px(context, 10f) * 2
        setMeasuredDimension(MinValue, MinValue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        this.canvas = canvas
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        val MinValue = Math.min(width, height)
        radius = MinValue / 2
        CircleX = getWidth() / 2
        CircleY = getHeight() / 2
        canvas.drawCircle(CircleX.toFloat(), CircleY.toFloat(), radius.toFloat(), backgroundPaint)
        drawSmallCircle(isYellow)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val centerX = (right - left) / 2
        val centerY = (bottom - top) / 2
        var panReady = false
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is RotatePan) {
                rotatePan = child
                val panWidth = child.getWidth()
                val panHeight = child.getHeight()
                child.layout(
                    centerX - panWidth / 2,
                    centerY - panHeight / 2,
                    centerX + panWidth / 2,
                    centerY + panHeight / 2
                )
                panReady = true
            } else if (child is ImageView) {
                if (TextUtils.equals(child.getTag() as String, START_BTN_TAG)) {
                    startBtn = child
                    val btnWidth = child.getWidth()
                    val btnHeight = child.getHeight()
                    child.layout(
                        centerX - btnWidth / 2,
                        centerY - btnHeight / 2,
                        centerX + btnWidth / 2,
                        centerY + btnHeight / 2
                    )
                }
            }
        }
        if (!panReady) throw RuntimeException("Have you add RotatePan in LuckPanLayout element ?")
    }

    private fun drawSmallCircle(FirstYellow: Boolean) {
        var FirstYellow = FirstYellow
        val pointDistance = radius - Util.dip2px(context, 10f)
        var i = 0
        while (i <= 360) {
            val x = (pointDistance * Math.sin(Util.change(i.toDouble()))).toInt() + CircleX
            val y = (pointDistance * Math.cos(Util.change(i.toDouble()))).toInt() + CircleY
            if (FirstYellow) canvas!!.drawCircle(
                x.toFloat(), y.toFloat(), Util.dip2px(
                    context, 4f
                ).toFloat(), yellowPaint
            ) else canvas!!.drawCircle(
                x.toFloat(), y.toFloat(), Util.dip2px(
                    context, 4f
                ).toFloat(), whitePaint
            )
            FirstYellow = !FirstYellow
            i += 20
        }
    }

    /**
     * 开始旋转
     *
     * @param pos       转到指定的转盘，-1 则随机
     * @param delayTime 外围灯光闪烁的间隔时间
     */
    fun rotate(pos: Int, delayTime: Int) {
        rotatePan!!.startRotate(pos)
        setDelayTime(delayTime)
        setStartBtnEnable(false)
    }

    fun setStartBtnEnable(enable: Boolean) {
        if (startBtn != null) startBtn!!.isEnabled =
            enable else throw RuntimeException("Have you add start button in LuckPanLayout element ?")

    }

    fun setStartBtnImage(@DrawableRes resId: Int) {
        startBtn?.setImageResource(resId)
    }

    private fun startLuckLight() {
        postDelayed(object : Runnable {
            override fun run() {
                isYellow = !isYellow
                invalidate()
                postDelayed(this, delayTime.toLong())
            }
        }, delayTime.toLong())
    }

    fun setDelayTime(delayTime: Int) {
        this.delayTime = delayTime
    }

    interface AnimationEndListener {
        fun endAnimation(position: Int)
    }

    var animationEndListener: AnimationEndListener? = null

    companion object {
        /**
         * LuckPan 中间对应的Button必须设置tag为 startbtn.
         */
        private const val START_BTN_TAG = "startbtn"
        const val DEFAULT_TIME_PERIOD = 500
    }

    init {
        backgroundPaint.color = Color.rgb(255, 92, 93)
        whitePaint.color = Color.WHITE
        yellowPaint.color = Color.YELLOW
        screeHeight = resources.displayMetrics.heightPixels
        screenWidth = resources.displayMetrics.widthPixels
        startLuckLight()
    }
}