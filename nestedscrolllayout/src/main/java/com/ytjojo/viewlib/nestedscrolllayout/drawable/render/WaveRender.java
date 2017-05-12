package com.ytjojo.viewlib.nestedscrolllayout.drawable.render;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.ytjojo.viewlib.nestedscrolllayout.NestedScrollLayout;
import com.ytjojo.viewlib.nestedscrolllayout.PtrIndicator;
import com.ytjojo.viewlib.nestedscrolllayout.PtrUIHandler;
import com.ytjojo.viewlib.nestedscrolllayout.Utils;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.LoadingRenderer;

/**
 * Created by Administrator on 2017/5/12 0012.
 */

public class WaveRender extends LoadingRenderer implements PtrUIHandler {

    private int waveHeight;
    private int headHeight;
    private int mDefaulWaveHeight;
    private int mDefaulHeadHeight;
    private Path mPath;
    private Paint mPaint;
    private int mWaveColor;
    private ViewOutlineProvider mViewOutlineProvider;
    public WaveRender(Context context) {
        super(context);
    }
    private void init() {
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mViewOutlineProvider = new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    if (mPath.isConvex()) outline.setConvexPath(mPath);
                }
            };

        }
    }
    @Override
    protected void computeRender(float renderProgress) {

    }

    @Override
    protected void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    protected void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    protected void reset() {

    }

    public int getWaveColor() {
        return mWaveColor;
    }

    public void setWaveColor(int color) {
        this.mWaveColor = color;
        invalidateSelf();
    }

    public int getHeadHeight() {
        return headHeight;
    }

    public void setHeadHeight(int headHeight) {
        this.headHeight = headHeight;
    }

    public int getWaveHeight() {
        return waveHeight;
    }

    public void setWaveHeight(int waveHeight) {
        this.waveHeight = waveHeight;
    }

    public int getmDefaulWaveHeight() {
        return mDefaulWaveHeight;
    }

    public void setmDefaulWaveHeight(int mDefaulWaveHeight) {
        this.mDefaulWaveHeight = mDefaulWaveHeight;
    }

    public int getmDefaulHeadHeight() {
        return mDefaulHeadHeight;
    }

    public void setmDefaulHeadHeight(int mDefaulHeadHeight) {
        this.mDefaulHeadHeight = mDefaulHeadHeight;
    }

    @Override
    protected void draw(Canvas canvas) {
        mPath.reset();
        mPaint.setColor(mWaveColor);
        mPath.lineTo(0, headHeight);
        mPath.quadTo(mBounds.width()/ 2, headHeight + waveHeight,mBounds.height(), headHeight);
        mPath.lineTo(mBounds.width(), 0);
        canvas.drawPath(mPath, mPaint);
    }


    @Override
    public void onUIRefreshComplete(NestedScrollLayout frame) {
        waveHeight = 0;
        ValueAnimator animator =ValueAnimator.ofInt(headHeight,0);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                headHeight = value;
                invalidateSelf();
            }
        });
    }



    @Override
    public void onUIPositionChange(NestedScrollLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
        float percent = Math.min(1f, ptrIndicator.getCurrentPercent());
        percent = ptrIndicator.getCurrentPercent() ;
        setHeadHeight((int) (mDefaulHeadHeight* Utils.constrain(percent,0,1 )));
        setWaveHeight((int) ((mDefaulWaveHeight) * Math.max(0, percent - 1)));
        invalidateSelf();
    }

    @Override
    public void onUIReset(NestedScrollLayout parent) {

    }

    @Override
    public void onUIRefreshPrepare(NestedScrollLayout parent) {

    }

    @Override
    public void onUIRefreshBegin(NestedScrollLayout frame) {
        setHeadHeight(mDefaulHeadHeight);
        ValueAnimator animator = ValueAnimator.ofInt(getWaveHeight(),0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Log.i("anim", "value--->" + (int) animation.getAnimatedValue());
                setWaveHeight((int) animation.getAnimatedValue());
                invalidateSelf();
            }
        });
        animator.setInterpolator(new BounceInterpolator());
        animator.setDuration(200);
        animator.start();
    }

}
