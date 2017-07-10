package com.github.ytjojo.supernestedlayout.drawable;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.Pools;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by baoyz on 14/10/29.
 */
public abstract class LoadingDrawable extends Drawable implements Drawable.Callback, Animatable {


    public abstract void setPercent(float percent);
    private AnimatorSet mAnimatorSet;

    public void setColorSchemeColors(int[] colorSchemeColors) {
        setColor(colorSchemeColors[0]);
    }

    /**
     * animationSet.play(anim1).with(anim2).after(0)
     * @param list
     * @return
     */
    public AnimatorSet createAnimatorSet(ArrayList<ValueAnimator> list){
        return null;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleDrawable(this, what, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, what);
        }
    }

    public boolean isFullCanvas() {
        return false;
    }

    public long getDelayScrollInitail() {
        return 0;
    }

    private HashMap<ValueAnimator, ValueAnimator.AnimatorUpdateListener> mUpdateListeners = new HashMap<>();

    private ArrayList<ValueAnimator> mAnimators;
    private int alpha = 255;
    private static final Rect ZERO_BOUNDS_RECT = new Rect();
    protected Rect drawBounds = ZERO_BOUNDS_RECT;

    private boolean mHasAnimators;

    protected Paint mPaint = new Paint();

    public LoadingDrawable() {
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
    }


    public int getColor() {
        return mPaint.getColor();
    }

    public void setColor(int color) {
        mPaint.setColor(color);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public void draw(Canvas canvas) {
        draw(canvas, mPaint);
    }

    public void draw(Canvas canvas, Paint paint) {

    }

    public ArrayList<ValueAnimator> onCreateAnimators() {
        return null;
    }

    @Override
    public void start() {
        ensureAnimators();

        if (mAnimators == null) {
            return;
        }

        // If the animators has not ended, do nothing.
        if (isStarted()) {
            return;
        }
        startAnimators();
        invalidateSelf();
    }

    private void startAnimators() {
        if(mAnimatorSet!=null){
            mAnimatorSet.start();
            return;
        }
        for (int i = 0; i < mAnimators.size(); i++) {
            ValueAnimator animator = mAnimators.get(i);

            //when the animator restart , add the updateListener again because they
            // was removed by animator stop .
            ValueAnimator.AnimatorUpdateListener updateListener = mUpdateListeners.get(animator);
            if (updateListener != null) {
                animator.addUpdateListener(updateListener);
            }

            animator.start();
        }
    }

    private void stopAnimators() {
        if(mAnimatorSet!=null){
            mAnimatorSet.end();
            return;
        }
        if (mAnimators != null) {
            for (ValueAnimator animator : mAnimators) {
                if (animator != null && animator.isRunning()) {
                    animator.removeAllUpdateListeners();
                    animator.end();
                }
            }
        }
    }

    private void ensureAnimators() {
        if (!mHasAnimators) {
            mAnimators = onCreateAnimators();
            mAnimatorSet = createAnimatorSet(mAnimators);
            mHasAnimators = true;
        }
    }

    @Override
    public void stop() {
        stopAnimators();
    }

    public void stopIimmediately() {
        stop();
    }

    public boolean isStarted() {
        if(mAnimatorSet !=null){
            return mAnimatorSet.isStarted();
        }

        for (ValueAnimator animator : mAnimators) {
            return animator.isStarted();
        }
        return false;
    }

    @Override
    public boolean isRunning() {
        if(mAnimatorSet !=null){
            mAnimatorSet.isRunning();
        }
        for (ValueAnimator animator : mAnimators) {
            return animator.isRunning();
        }
        return false;
    }

    /**
     * Your should use this to add AnimatorUpdateListener when
     * create animator , otherwise , animator doesn't work when
     * the animation restart .
     *
     * @param updateListener
     */
    public void addUpdateListener(ValueAnimator animator, ValueAnimator.AnimatorUpdateListener updateListener) {
        mUpdateListeners.put(animator, updateListener);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        setDrawBounds(bounds);
    }

    public void setDrawBounds(Rect drawBounds) {
        setDrawBounds(drawBounds.left, drawBounds.top, drawBounds.right, drawBounds.bottom);
    }

    public void setDrawBounds(int left, int top, int right, int bottom) {
        this.drawBounds = new Rect(left, top, right, bottom);
    }

    public void postInvalidate() {
        invalidateSelf();
    }

    public Rect getDrawBounds() {
        return drawBounds;
    }

    public int getWidth() {
        return drawBounds.width();
    }

    public int getHeight() {
        return drawBounds.height();
    }

    public int centerX() {
        return drawBounds.centerX();
    }

    public int centerY() {
        return drawBounds.centerY();
    }

    public float exactCenterX() {
        return drawBounds.exactCenterX();
    }

    public float exactCenterY() {
        return drawBounds.exactCenterY();
    }

    public void onReset() {
    }

    Pools.Pool<RectF> mRectFPools;
    Pools.Pool<PointF> mPointFPools;

    public RectF acquireRectF() {
        if (mRectFPools == null) {
            mRectFPools = new Pools.SimplePool<>(6);
        }
        RectF rectF = mRectFPools.acquire();
        if (rectF == null) {
            rectF = new RectF();
        }
        return rectF;
    }

    public void release(RectF rectF) {
        if (mRectFPools == null) {
            mRectFPools = new Pools.SimplePool<>(6);
        }
        try {
            mRectFPools.release(rectF);
        } catch (IllegalStateException e) {

        }
    }

    public PointF acquirePointF() {
        if (mPointFPools == null) {
            mPointFPools = new Pools.SimplePool<>(6);
        }
        PointF pointF = mPointFPools.acquire();
        if (pointF == null) {
            pointF = new PointF();
        }
        return pointF;
    }

    public void release(PointF pointF) {
        if (mPointFPools == null) {
            mPointFPools = new Pools.SimplePool<>(6);
        }
        try {
            mPointFPools.release(pointF);
        } catch (IllegalStateException e) {

        }

    }

}