package com.v2reading.reader.ui.widget.luckypan

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ScrollerCompat
import com.v2reading.reader.R
import com.v2reading.reader.utils.Util
import java.util.*


class RotatePan @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(
    context, attrs, defStyleAttr
) {
    private var panNum = 0
    private val dPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var InitAngle = 0
    private var radius = 0
    private val verPanRadius: Int
    private val diffRadius: Int
    private var images: Array<Int>? = null
    private var strs: Array<String>? = null
    private var bitmaps: MutableList<Bitmap> = ArrayList()
//    private val mDetector: GestureDetectorCompat
    private val scroller: ScrollerCompat
    private val screenWidth: Int
    private val screeHeight: Int
    private fun checkPanState(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.luckpan)
        panNum = typedArray.getInteger(R.styleable.luckpan_pannum, 0)
        if (360 % panNum != 0) throw RuntimeException("can't split pan for all icon.")
        val nameArray = typedArray.getResourceId(R.styleable.luckpan_namesPan, -1)
        if (nameArray == -1) throw RuntimeException("Can't find pan name.")
        strs = context.resources.getStringArray(nameArray)
        val iconArray = typedArray.getResourceId(R.styleable.luckpan_iconsPan, -1)
        if (iconArray == -1) throw RuntimeException("Can't find pan icon.")
        val iconStrs = context.resources.getStringArray(iconArray)
        val iconLists: MutableList<Int> = ArrayList()
        for (i in iconStrs.indices) {
            iconLists.add(
                context.resources.getIdentifier(
                    iconStrs[i],
                    "mipmap",
                    context.packageName
                )
            )
        }
        images = iconLists.toTypedArray()
        typedArray.recycle()
        if (strs == null || images == null) throw RuntimeException("Can't find string or icon resources.")
        if (strs!!.size != panNum || images!!.size != panNum) throw RuntimeException("The string length or icon length  isn't equals panNum.")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var MinValue = Math.min(screenWidth, screeHeight)
        MinValue -= Util.dip2px(context, 38f) * 2
        setMeasuredDimension(MinValue, MinValue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        val MinValue = Math.min(width, height)
        radius = MinValue / 2
        val rectF = RectF(
            getPaddingLeft().toFloat(),
            getPaddingTop().toFloat(),
            width.toFloat(),
            height.toFloat()
        )
        var angle = if (panNum % 4 == 0) InitAngle else InitAngle - diffRadius
        for (i in 0 until panNum) {
            if (i % 2 == 0) {
                canvas.drawArc(rectF, angle.toFloat(), verPanRadius.toFloat(), true, dPaint)
            } else {
                canvas.drawArc(rectF, angle.toFloat(), verPanRadius.toFloat(), true, sPaint)
            }
            angle += verPanRadius
        }
        for (i in 0 until panNum) {
            drawIcon(
                width / 2,
                height / 2,
                radius,
                if (panNum % 4 == 0) (InitAngle + diffRadius).toFloat() else InitAngle.toFloat(),
                i,
                canvas
            )
            InitAngle += verPanRadius
        }
        for (i in 0 until panNum) {
            drawText(
                if (panNum % 4 == 0) (InitAngle + diffRadius + diffRadius * 3 / 4).toFloat() else InitAngle + diffRadius.toFloat(),
                strs!![i],
                2 * radius,
                textPaint,
                canvas,
                rectF
            )
            InitAngle += verPanRadius
        }
    }

    private fun drawText(
        startAngle: Float,
        string: String,
        mRadius: Int,
        mTextPaint: Paint,
        mCanvas: Canvas,
        mRange: RectF
    ) {
        val path = Path()
        path.addArc(mRange, startAngle, verPanRadius.toFloat())
        val textWidth = mTextPaint.measureText(string)

        //圆弧的水平偏移
        val hOffset =
            if (panNum % 4 == 0) (mRadius * Math.PI / panNum / 2).toFloat() else (mRadius * Math.PI / panNum / 2 - textWidth / 2).toFloat()
        //圆弧的垂直偏移
        val vOffset = (mRadius / 2 / 6).toFloat()
        mCanvas.drawTextOnPath(string, path, hOffset, vOffset, mTextPaint)
    }

    private fun drawIcon(
        xx: Int,
        yy: Int,
        mRadius: Int,
        startAngle: Float,
        i: Int,
        mCanvas: Canvas
    ) {
        val imgWidth = mRadius / 4
        val angle = Math.toRadians((verPanRadius + startAngle).toDouble())
            .toFloat()

        //确定图片在圆弧中 中心点的位置
        val x = (xx + (mRadius / 2 + mRadius / 12) * Math.cos(angle.toDouble())).toFloat()
        val y = (yy + (mRadius / 2 + mRadius / 12) * Math.sin(angle.toDouble())).toFloat()

        // 确定绘制图片的位置
        val rect = RectF(
            x - imgWidth * 2 / 3, y - imgWidth * 2 / 3, x + imgWidth
                    * 2 / 3, y + imgWidth * 2 / 3
        )
        val bitmap = bitmaps[i]
        mCanvas.drawBitmap(bitmap, null, rect, null)
    }

    fun setImages(bitmaps: MutableList<Bitmap>) {
        this.bitmaps = bitmaps
        this.invalidate()
    }

    fun setStr(strs: Array<String>) {
        this.strs = strs
        this.invalidate()
    }

    /**
     * 开始转动
     * @param pos 如果 pos = -1 则随机，如果指定某个值，则转到某个指定区域
     */
    fun startRotate(pos: Int) {

        //Rotate lap.
        var lap = (Math.random() * 8).toInt() + 4

        //Rotate angle.
        var angle = 0
        if (pos < 0) {
            angle = (Math.random() * 360).toInt()
        } else {
            val initPos = queryPosition()
            if (pos > initPos) {
                angle = (pos - initPos) * verPanRadius
                lap -= 1
                angle = 360 - angle
            } else if (pos < initPos) {
                angle = (initPos - pos) * verPanRadius
            } else {
                //nothing to do.
            }
        }

        //All of the rotate angle.
        val increaseDegree = lap * 360 + angle
        val time = (lap + angle / 360) * ONE_WHEEL_TIME
        var DesRotate = increaseDegree + InitAngle

        //TODO 为了每次都能旋转到转盘的中间位置
        val offRotate = DesRotate % 360 % verPanRadius
        DesRotate -= offRotate
        DesRotate += diffRadius
        val animtor = ValueAnimator.ofInt(InitAngle, DesRotate)
        animtor.interpolator = AccelerateDecelerateInterpolator()
        animtor.duration = time
        animtor.addUpdateListener { animation ->
            val updateValue = animation.animatedValue as Int
            InitAngle = (updateValue % 360 + 360) % 360
            ViewCompat.postInvalidateOnAnimation(this@RotatePan)
        }
        animtor.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if ((parent as LuckPanLayout).animationEndListener != null) {
                    (parent as LuckPanLayout).setStartBtnEnable(true)
                    (parent as LuckPanLayout).setDelayTime(LuckPanLayout.DEFAULT_TIME_PERIOD)
                    (parent as LuckPanLayout).animationEndListener!!.endAnimation(queryPosition())
                }
            }
        })
        animtor.start()
    }

    private fun queryPosition(): Int {
        InitAngle = (InitAngle % 360 + 360) % 360
        var pos = InitAngle / verPanRadius
        if (panNum == 4) pos++
        return calcumAngle(pos)
    }

    private fun calcumAngle(pos: Int): Int {
        var pos = pos
        pos = if (pos >= 0 && pos <= panNum / 2) {
            panNum / 2 - pos
        } else {
            panNum - pos + panNum / 2
        }
        return pos
    }

    override fun onDetachedFromWindow() {
        clearAnimation()
        if (parent is LuckPanLayout) {
            (parent as LuckPanLayout).handler.removeCallbacksAndMessages(null)
        }
        super.onDetachedFromWindow()
    }

    // TODO ==================================== 手势处理 ===============================================================
    override fun onTouchEvent(event: MotionEvent): Boolean {
//        val consume = mDetector.onTouchEvent(event)
//        if (consume) {
//            parent.parent.requestDisallowInterceptTouchEvent(true)
//            return true
//        }
        return super.onTouchEvent(event)
    }

    fun setRotate(rotation: Int) {
        var rotation = rotation
        rotation = (rotation % 360 + 360) % 360
        InitAngle = rotation
        ViewCompat.postInvalidateOnAnimation(this)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            setRotate(scroller.currY)
        }
        super.computeScroll()
    }

    private inner class RotatePanGestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return super.onDown(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return false
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val centerX = (this@RotatePan.left + this@RotatePan.right) * 0.5f
            val centerY = (this@RotatePan.top + this@RotatePan.bottom) * 0.5f
            val scrollTheta = vectorToScalarScroll(
                distanceX, distanceY, e2.x - centerX, e2.y -
                        centerY
            )
            val rotate = InitAngle -
                    scrollTheta.toInt() / FLING_VELOCITY_DOWNSCALE
            setRotate(rotate)
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val centerX = (this@RotatePan.left + this@RotatePan.right) * 0.5f
            val centerY = (this@RotatePan.top + this@RotatePan.bottom) * 0.5f
            val scrollTheta = vectorToScalarScroll(
                velocityX, velocityY, e2.x - centerX, e2.y -
                        centerY
            )
            scroller.abortAnimation()
            scroller.fling(
                0, InitAngle, 0, scrollTheta.toInt() / FLING_VELOCITY_DOWNSCALE,
                0, 0, Int.MIN_VALUE, Int.MAX_VALUE
            )
            return true
        }
    }

    //TODO 判断滑动的方向
    private fun vectorToScalarScroll(dx: Float, dy: Float, x: Float, y: Float): Float {
        val l = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val crossX = -y
        val dot = crossX * dx + x * dy
        val sign = Math.signum(dot)
        return l * sign
    }

    companion object {
        const val FLING_VELOCITY_DOWNSCALE = 4

        //旋转一圈所需要的时间
        private const val ONE_WHEEL_TIME: Long = 800
    }

    init {
        screeHeight = resources.displayMetrics.heightPixels
        screenWidth = resources.displayMetrics.widthPixels
//        mDetector = GestureDetectorCompat(context, RotatePanGestureListener())
        scroller = ScrollerCompat.create(context)
        checkPanState(context, attrs)
        InitAngle = 360 / panNum
        verPanRadius = 360 / panNum
        diffRadius = verPanRadius / 2
        dPaint.color = Color.rgb(255, 133, 132)
        sPaint.color = Color.rgb(254, 104, 105)
        textPaint.color = Color.WHITE
        textPaint.textSize = Util.dip2px(context, 16f).toFloat()
        isClickable = true
        for (i in 0 until panNum) {
            val bitmap = BitmapFactory.decodeResource(context.resources, images!![i])
            bitmaps.add(bitmap)
        }
    }
}