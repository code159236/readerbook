package com.v2reading.reader.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager


/**
 * Created by shaowen.jiang@wellcare.cn on 2022/7/11.
 */
class NoHScrollViewPager : ViewPager {
    private var isCanScroll = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * 设置其是否能滑动换页
     * @param isCanScroll false 不能换页， true 可以滑动换页
     */
    fun setScanScroll(isCanScroll: Boolean) {
        this.isCanScroll = isCanScroll
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return isCanScroll && super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return isCanScroll && super.onTouchEvent(ev)
    }
}