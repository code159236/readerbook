package com.v2reading.reader.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.viewpager.widget.ViewPager;

public class BottomPagesView extends View {

    private static float density = 0;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;
    private int scrollPosition;
    private int currentPage;
    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    private RectF rect = new RectF();
    private float animatedProgress;
    private ViewPager viewPager;
    private int pagesCount;

    public BottomPagesView(Context context, ViewPager pager, int count) {
        super(context);
        viewPager = pager;
        pagesCount = count;
        density = context.getResources().getDisplayMetrics().density;
    }

    public void setPageOffset(int position, float offset) {
        progress = offset;
        scrollPosition = position;
        invalidate();
    }

    public void setCurrentPage(int page) {
        currentPage = page;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        float d = dp(5);

        paint.setColor(0xffbbbbbb);

        int x;
        currentPage = viewPager.getCurrentItem();
        for (int a = 0; a < pagesCount; a++) {
            if (a == currentPage) {
                continue;
            }
            x = a * dp(11);
            rect.set(x, 0, x + dp(5), dp(5));
            canvas.drawRoundRect(rect, dp(2.5f), dp(2.5f), paint);
        }

        paint.setColor(0xff2ca5e0);

        x = currentPage * dp(11);
        if (progress != 0) {
            if (scrollPosition >= currentPage) {
                rect.set(x, 0, x + dp(5) + dp(11) * progress, dp(5));
            } else {
                rect.set(x - dp(11) * (1.0f - progress), 0, x + dp(5), dp(5));
            }
        } else {
            rect.set(x, 0, x + dp(5), dp(5));
        }
        canvas.drawRoundRect(rect, dp(2.5f), dp(2.5f), paint);
    }

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }
}
