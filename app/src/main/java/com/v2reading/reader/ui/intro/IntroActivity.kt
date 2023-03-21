package com.v2reading.reader.ui.intro

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.database.DataSetObserver
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RoundRectShape
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.util.StateSet
import android.util.TypedValue
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.v2reading.reader.R
import com.v2reading.reader.ui.welcome.WelcomeActivity
import com.v2reading.reader.ui.widget.BottomPagesView

import javax.microedition.khronos.egl.*
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10
import kotlin.math.ceil


class IntroActivity : AppCompatActivity() {
    private var viewPager: ViewPager? = null
    private var bottomPages: BottomPagesView? = null
    private lateinit var textView: TextView
    private lateinit var startMessagingButton: TextView
    private var frameLayout2: FrameLayout? = null
    private var lastPage = 0
    private var justCreated = false
    private var startPressed = false
    private var titles: MutableList<String> = mutableListOf()
    private var messages: MutableList<String> = mutableListOf()
    private var currentViewPagerPage = 0
    private var eglThread: EGLThread? = null
    private var currentDate: Long = 0
    private var justEndDragging = false
    private var dragging = false
    private var startDragX = 0
    private var destroyed = false
    private var density = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_TMessages)
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        density = resources.displayMetrics.density

//        preferences.edit().putLong("intro_crashed_time", System.currentTimeMillis()).commit()
        titles = mutableListOf(
            getString(R.string.intro_title_1),
            getString(R.string.intro_title_2),
            getString(R.string.intro_title_3),
            getString(R.string.intro_title_4),
            getString(R.string.intro_title_5),
            getString(R.string.intro_title_6)
        )
        messages = mutableListOf(
            getString(R.string.intro_content_1),
            getString(R.string.intro_content_2),
            getString(R.string.intro_content_3),
            getString(R.string.intro_content_4),
            getString(R.string.intro_content_5),
            getString(R.string.intro_content_6)
        )

        val scrollView = ScrollView(this)
        scrollView.isFillViewport = true
        val frameLayout: FrameLayout = object : FrameLayout(this) {
            override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                super.onLayout(changed, left, top, right, bottom)
                val oneFourth = (bottom - top) / 4
                var y: Int = (oneFourth * 3 - dp(275)) / 2
                frameLayout2!!.layout(
                    0,
                    y,
                    frameLayout2!!.measuredWidth,
                    y + frameLayout2!!.measuredHeight
                )
                y += dp(272)
                var x = (measuredWidth - bottomPages!!.measuredWidth) / 2
                bottomPages!!.layout(
                    x,
                    y,
                    x + bottomPages!!.measuredWidth,
                    y + bottomPages!!.measuredHeight
                )
                viewPager!!.layout(0, 0, viewPager!!.measuredWidth, viewPager!!.measuredHeight)
                y = oneFourth * 3 + (oneFourth - startMessagingButton.measuredHeight) / 2
                x = (measuredWidth - startMessagingButton.measuredWidth) / 2
                startMessagingButton.layout(
                    x,
                    y,
                    x + startMessagingButton.measuredWidth,
                    y + startMessagingButton.measuredHeight
                )
                y -= dp(30)
                x = (measuredWidth - textView.measuredWidth) / 2
                textView.layout(x, y - textView.measuredHeight, x + textView.measuredWidth, y)
            }
        }
        frameLayout.setBackgroundColor(-0x1)
        scrollView.addView(
            frameLayout,
            LayoutHelper.createScroll(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.LEFT or Gravity.TOP
            )
        )
        frameLayout2 = FrameLayout(this)
        frameLayout.addView(
            frameLayout2,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.LEFT or Gravity.TOP,
                0,
                78,
                0,
                0
            )
        )
        val textureView = TextureView(this)
        frameLayout2?.addView(textureView, LayoutHelper.createFrame(200, 150, Gravity.CENTER))
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (eglThread == null && surface != null) {
                    eglThread = EGLThread(surface)
                    eglThread?.setSurfaceTextureSize(width, height)
                    eglThread?.postRunnable { eglThread?.drawRunnable?.run() }
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (eglThread != null) {
                    eglThread?.setSurfaceTextureSize(width, height)
                }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                if (eglThread != null) {
                    eglThread?.shutdown()
                    eglThread = null
                }
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        viewPager = ViewPager(this)
        viewPager?.adapter = IntroAdapter()
        viewPager?.pageMargin = 0
        viewPager?.offscreenPageLimit = 1
        frameLayout.addView(
            viewPager,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        )
        viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                bottomPages!!.setPageOffset(position, positionOffset)
                val width = viewPager?.measuredWidth?.toFloat() ?: 0
                if (width == 0f) {
                    return
                }
                val offset =
                    (position.toFloat() * width.toFloat() + positionOffsetPixels - currentViewPagerPage.toFloat() * width.toFloat()) / width.toFloat()
                com.v2reading.app.Intro.setScrollOffset(offset)
            }

            override fun onPageSelected(i: Int) {
                currentViewPagerPage = i
            }

            override fun onPageScrollStateChanged(i: Int) {
                if (i == ViewPager.SCROLL_STATE_DRAGGING) {
                    dragging = true
                    startDragX = viewPager?.currentItem!! * viewPager?.measuredWidth!!
                } else if (i == ViewPager.SCROLL_STATE_IDLE || i == ViewPager.SCROLL_STATE_SETTLING) {
                    if (dragging) {
                        justEndDragging = true
                        dragging = false
                    }
                    if (lastPage != viewPager?.currentItem) {
                        lastPage = viewPager?.currentItem!!
                    }
                }
            }
        })
        startMessagingButton = TextView(this)
        startMessagingButton.text = "Start Reading"
        startMessagingButton.gravity = Gravity.CENTER
        startMessagingButton.setTextColor(-0x1)
        startMessagingButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        startMessagingButton.setBackgroundDrawable(
            createSimpleSelectorRoundRectDrawable(
                dp(4),
                -0xFF78A0,
                -0xFE93B3
            )
        )

        startMessagingButton.setPadding(dp(34), 0, dp(34), 0)
        frameLayout.addView(
            startMessagingButton,
            LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT,
                42,
                Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM,
                10,
                0,
                10,
                76
            )
        )
        startMessagingButton.setOnClickListener(View.OnClickListener {
            if (startPressed) {
                return@OnClickListener
            }
            startPressed = true
            val intent2 = Intent(this@IntroActivity, WelcomeActivity::class.java)
            startActivity(intent2)
            destroyed = true
            finish()
        })

        bottomPages = BottomPagesView(this, viewPager, 6)
        frameLayout.addView(
            bottomPages,
            LayoutHelper.createFrame(66, 5, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 350, 0, 0)
        )
        textView = TextView(this)
        textView.setTextColor(-0xec6c2e)
        textView.gravity = Gravity.CENTER
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        frameLayout.addView(
            textView,
            LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT,
                30,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                0,
                0,
                0,
                20
            )
        )
        textView.setOnClickListener(View.OnClickListener { v: View? ->
            if (startPressed) {
                return@OnClickListener
            }
            startPressed = true
            val intent2 = Intent(this@IntroActivity, WelcomeActivity::class.java)
            startActivity(intent2)
            destroyed = true
            finish()
        })
        if (resources.getBoolean(R.bool.isTablet)) {
            val frameLayout3 = FrameLayout(this)
            setContentView(frameLayout3)
            val imageView: View = ImageView(this)
            val drawable = resources.getDrawable(R.drawable.catstile) as BitmapDrawable
            drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            imageView.setBackgroundDrawable(drawable)
            frameLayout3.addView(
                imageView,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
            )
            val frameLayout4 = FrameLayout(this)
            frameLayout4.setBackgroundResource(R.drawable.btnshadow)
            frameLayout4.addView(
                scrollView,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
            )
            frameLayout3.addView(frameLayout4, LayoutHelper.createFrame(498, 528, Gravity.CENTER))
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            setContentView(scrollView)
        }

    }

    override fun onResume() {
        super.onResume()
        if (justCreated) {
            if (LayoutHelper.LocaleController.isRTL) {
                viewPager!!.currentItem = 6
                lastPage = 6
            } else {
                viewPager!!.currentItem = 0
                lastPage = 0
            }
            justCreated = false
        }
    }

    override fun onBackPressed() {
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyed = true
    }

    fun dp(value: Int): Int {
        return if (value == 0) {
            0
        } else ceil((density * value).toDouble()).toInt()
    }

    inner class IntroAdapter : PagerAdapter() {
        override fun getCount(): Int {
            return titles.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val headerTextView = TextView(container.context)
            val messageTextView = TextView(container.context)
            val frameLayout: FrameLayout = object : FrameLayout(container.context) {
                override fun onLayout(
                    changed: Boolean,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int
                ) {
                    val oneFourth = (bottom - top) / 4
                    var y: Int = (oneFourth * 3 - dp(275)) / 2
                    y += dp(166)
                    var x: Int = dp(18)
                    headerTextView.layout(
                        x,
                        y,
                        x + headerTextView.measuredWidth,
                        y + headerTextView.measuredHeight
                    )
                    y += dp(42)
                    x = dp(16)
                    messageTextView.layout(
                        x,
                        y,
                        x + messageTextView.measuredWidth,
                        y + messageTextView.measuredHeight
                    )
                }
            }
            headerTextView.setTextColor(-0xdededf)
            headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26f)
            headerTextView.gravity = Gravity.CENTER
            frameLayout.addView(
                headerTextView,
                LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.TOP or Gravity.LEFT,
                    18,
                    244,
                    18,
                    0
                )
            )
            messageTextView.setTextColor(-0x7f7f80)
            messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            messageTextView.gravity = Gravity.CENTER
            frameLayout.addView(
                messageTextView,
                LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.TOP or Gravity.LEFT,
                    16,
                    286,
                    16,
                    0
                )
            )
            container.addView(frameLayout, 0)
            headerTextView.text = titles[position]
            messageTextView.text = (messages[position])
            return frameLayout
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            super.setPrimaryItem(container, position, `object`)
            bottomPages?.setCurrentPage(position)
            currentViewPagerPage = position
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }

        override fun restoreState(arg0: Parcelable?, arg1: ClassLoader?) {}
        override fun saveState(): Parcelable? {
            return null
        }

        override fun unregisterDataSetObserver(observer: DataSetObserver) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer)
            }
        }
    }


    inner class EGLThread(private val surfaceTexture: SurfaceTexture) : DispatchQueue("EGLThread") {
        private var egl10: EGL10? = null
        private var eglDisplay: EGLDisplay? = null
        private var eglConfig: EGLConfig? = null
        private var eglContext: EGLContext? = null
        private var eglSurface: EGLSurface? = null
        private var gl: GL? = null
        private var initied = false
        private val textures = IntArray(23)
        private val lastRenderCallTime: Long = 0
        private fun initGL(): Boolean {
            egl10 = EGLContext.getEGL() as EGL10
            eglDisplay = egl10!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            if (eglDisplay === EGL10.EGL_NO_DISPLAY) {
////                if (BuildVars.LOGS_ENABLED) {
////                    FileLog.e("eglGetDisplay failed " + GLUtils.getEGLErrorString(egl10.eglGetError()));
////                }
                finish()
                return false
            }
            val version = IntArray(2)
            if (!egl10!!.eglInitialize(eglDisplay, version)) {
////                if (BuildVars.LOGS_ENABLED) {
////                    FileLog.e("eglInitialize failed " + GLUtils.getEGLErrorString(egl10.eglGetError()));
////                }
                finish()
                return false
            }
            val configsCount = IntArray(1)
            val configs = arrayOfNulls<EGLConfig>(1)
            val configSpec = intArrayOf(
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 24,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_SAMPLE_BUFFERS, 1,
                EGL10.EGL_SAMPLES, 2,
                EGL10.EGL_NONE
            )
            eglConfig =
                if (!egl10!!.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
//                if (BuildVars.LOGS_ENABLED) {
//                    FileLog.e("eglChooseConfig failed " + GLUtils.getEGLErrorString(egl10.eglGetError()));
//                }
                    finish()
                    return false
                } else if (configsCount[0] > 0) {
                    configs[0]
                } else {
//                if (BuildVars.LOGS_ENABLED) {
//                    FileLog.e("eglConfig not initialized");
//                }
                    finish()
                    return false
                }
            val attrib_list = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
            eglContext =
                egl10!!.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list)
            if (eglContext == null) {
//                if (BuildVars.LOGS_ENABLED) {
//                    FileLog.e("eglCreateContext failed " + GLUtils.getEGLErrorString(egl10.eglGetError()));
//                }
                finish()
                return false
            }
            eglSurface = if (surfaceTexture is SurfaceTexture) {
                egl10!!.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null)
            } else {
                finish()
                return false
            }
            if (eglSurface == null || eglSurface === EGL10.EGL_NO_SURFACE) {
//                if (BuildVars.LOGS_ENABLED) {
//                    FileLog.e("createWindowSurface failed " + GLUtils.getEGLErrorString(egl10.eglGetError()));
//                }
                finish()
                return false
            }
            if (!egl10!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
//                if (BuildVars.LOGS_ENABLED) {
//                    FileLog.e("eglMakeCurrent failed " + GLUtils.getEGLErrorString(egl10.eglGetError()));
//                }
                finish()
                return false
            }
            gl = eglContext!!.gl
            GLES20.glGenTextures(23, textures, 0)
            loadTexture(R.drawable.intro_fast_arrow_shadow, 0)
            loadTexture(R.drawable.intro_fast_arrow, 1)
            loadTexture(R.drawable.intro_fast_body, 2)
            loadTexture(R.drawable.intro_fast_spiral, 3)
            loadTexture(R.drawable.intro_ic_bubble_dot, 4)
            loadTexture(R.drawable.intro_ic_bubble, 5)
            loadTexture(R.drawable.intro_ic_cam_lens, 6)
            loadTexture(R.drawable.intro_ic_cam, 7)
            loadTexture(R.drawable.intro_ic_pencil, 8)
            loadTexture(R.drawable.intro_ic_pin, 9)
            loadTexture(R.drawable.intro_ic_smile_eye, 10)
            loadTexture(R.drawable.intro_ic_smile, 11)
            loadTexture(R.drawable.intro_ic_videocam, 12)
            loadTexture(R.drawable.intro_knot_down, 13)
            loadTexture(R.drawable.intro_knot_up, 14)
            loadTexture(R.drawable.intro_powerful_infinity_white, 15)
            loadTexture(R.drawable.intro_powerful_infinity, 16)
            loadTexture(R.drawable.intro_powerful_mask, 17)
            loadTexture(R.drawable.intro_powerful_star, 18)
            loadTexture(R.drawable.intro_private_door, 19)
            loadTexture(R.drawable.intro_private_screw, 20)
            loadTexture(R.drawable.intro_tg_plane, 21)
            loadTexture(R.drawable.intro_tg_sphere, 22)
            com.v2reading.app.Intro.setTelegramTextures(textures[22], textures[21])
            com.v2reading.app.Intro.setPowerfulTextures(textures[17], textures[18], textures[16], textures[15])
            com.v2reading.app.Intro.setPrivateTextures(textures[19], textures[20])
            com.v2reading.app.Intro.setFreeTextures(textures[14], textures[13])
            com.v2reading.app.Intro.setFastTextures(textures[2], textures[3], textures[1], textures[0])
            com.v2reading.app.Intro.setIcTextures(
                textures[4],
                textures[5],
                textures[6],
                textures[7],
                textures[8],
                textures[9],
                textures[10],
                textures[11],
                textures[12]
            )
            com.v2reading.app.Intro.onSurfaceCreated()
            currentDate = System.currentTimeMillis() - 1000
            return true
        }

        fun finish() {
            if (eglSurface != null) {
                egl10!!.eglMakeCurrent(
                    eglDisplay,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT
                )
                egl10!!.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = null
            }
            if (eglContext != null) {
                egl10!!.eglDestroyContext(eglDisplay, eglContext)
                eglContext = null
            }
            if (eglDisplay != null) {
                egl10!!.eglTerminate(eglDisplay)
                eglDisplay = null
            }
        }

        val drawRunnable: Runnable = object : Runnable {
            override fun run() {
                if (!initied) {
                    return
                }
                if (eglContext != egl10!!.eglGetCurrentContext() || eglSurface != egl10!!.eglGetCurrentSurface(
                        EGL10.EGL_DRAW
                    )
                ) {
                    if (!egl10!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
//                        if (BuildVars.LOGS_ENABLED) {
//                            FileLog.e("eglMakeCurrent failed " + GLUtils.getEGLErrorString(egl10.eglGetError()));
//                        }
                        return
                    }
                }
                val time = (System.currentTimeMillis() - currentDate) / 1000.0f
                com.v2reading.app.Intro.setPage(currentViewPagerPage)
                com.v2reading.app.Intro.setDate(time)
                com.v2reading.app.Intro.onDrawFrame()
                egl10!!.eglSwapBuffers(eglDisplay, eglSurface)
                postRunnable({ this.run() }, 16)
            }
        }

        private fun loadTexture(resId: Int, index: Int) {
            val drawable = resources.getDrawable(resId)
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[index])
                GLES20.glTexParameteri(
                    GL10.GL_TEXTURE_2D,
                    GL10.GL_TEXTURE_MIN_FILTER,
                    GL10.GL_LINEAR
                )
                GLES20.glTexParameteri(
                    GL10.GL_TEXTURE_2D,
                    GL10.GL_TEXTURE_MAG_FILTER,
                    GL10.GL_LINEAR
                )
                GLES20.glTexParameteri(
                    GL10.GL_TEXTURE_2D,
                    GL10.GL_TEXTURE_WRAP_S,
                    GL10.GL_CLAMP_TO_EDGE
                )
                GLES20.glTexParameteri(
                    GL10.GL_TEXTURE_2D,
                    GL10.GL_TEXTURE_WRAP_T,
                    GL10.GL_CLAMP_TO_EDGE
                )
                GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
            }
        }

        fun shutdown() {
            postRunnable {
                finish()
                val looper = Looper.myLooper()
                looper?.quit()
            }
        }

        fun setSurfaceTextureSize(width: Int, height: Int) {
            com.v2reading.app.Intro.onSurfaceChanged(width, height, Math.min(width / 150.0f, height / 150.0f), 0)
        }

        override fun run() {
            initied = initGL()
            super.run()
        }

    }

    companion object {
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        private const val EGL_OPENGL_ES2_BIT = 4
    }

    fun createSimpleSelectorRoundRectDrawable(
        rad: Int,
        defaultColor: Int,
        pressedColor: Int
    ): Drawable? {
        return createSimpleSelectorRoundRectDrawable(rad, defaultColor, pressedColor, pressedColor)
    }

    fun createSimpleSelectorRoundRectDrawable(
        rad: Int,
        defaultColor: Int,
        pressedColor: Int,
        maskColor: Int
    ): Drawable? {
        val defaultDrawable = ShapeDrawable(
            RoundRectShape(
                floatArrayOf(
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat()
                ), null, null
            )
        )
        defaultDrawable.paint.color = defaultColor
        val pressedDrawable = ShapeDrawable(
            RoundRectShape(
                floatArrayOf(
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat()
                ), null, null
            )
        )
        pressedDrawable.paint.color = maskColor

        val colorStateList =
            ColorStateList(arrayOf(StateSet.WILD_CARD), intArrayOf(pressedColor))
        return RippleDrawable(colorStateList, defaultDrawable, pressedDrawable)

    }

}