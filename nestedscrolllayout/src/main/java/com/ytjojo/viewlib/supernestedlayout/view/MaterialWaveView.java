package com.ytjojo.viewlib.supernestedlayout.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.ytjojo.viewlib.supernestedlayout.SuperNestedLayout;
import com.ytjojo.viewlib.supernestedlayout.PtrIndicator;
import com.ytjojo.viewlib.supernestedlayout.PtrUIHandler;


public class MaterialWaveView extends View implements PtrUIHandler {
    private int waveHeight;
    private int mHeadHeight;
    private int mDefaulWaveHeight;
    private int mDefaulHeadHeight;
    private Path mPath;
    private Paint paint;
    private int mWaveColor;
    private ViewOutlineProvider mViewOutlineProvider;

    public MaterialWaveView(Context context) {
        this(context, null, 0);
    }

    public MaterialWaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mPath = new Path();
        paint = new Paint();
        paint.setAntiAlias(true);
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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(mViewOutlineProvider);
        }
    }

    public int getWaveColor() {
        return mWaveColor;
    }

    public void setWaveColor(int color) {
        this.mWaveColor = color;
        invalidate();
    }

    public int getmHeadHeight() {
        return mHeadHeight;
    }

    public void setHeadHeight(int mHeadHeight) {
        this.mHeadHeight = mHeadHeight;
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

    public void setDefaulWaveHeight(int mDefaulWaveHeight) {
        this.mDefaulWaveHeight = mDefaulWaveHeight;
    }

    public int getmDefaulHeadHeight() {
        return mDefaulHeadHeight;
    }

    public void setDefaulHeadHeight(int mDefaulHeadHeight) {
        this.mDefaulHeadHeight = mDefaulHeadHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPath.reset();
        paint.setColor(mWaveColor);
        mPath.lineTo(0, mHeadHeight);
        mPath.quadTo(getMeasuredWidth() / 2, mHeadHeight + waveHeight, getMeasuredWidth(), mHeadHeight);
        mPath.lineTo(getMeasuredWidth(), 0);
        canvas.drawPath(mPath, paint);
    }


    @Override
    public void onUIRefreshComplete(SuperNestedLayout frame) {
//        waveHeight = 0;
//        ValueAnimator animator =ValueAnimator.ofInt(mHeadHeight,0);
//        animator.setDuration(200);
//        animator.setInterpolator(new DecelerateInterpolator());
//        animator.start();
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                int value = (int) animation.getAnimatedValue();
//                mHeadHeight = value;
//                invalidate();
//            }
//        });
    }



    @Override
    public void onUIPositionChange(SuperNestedLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
        float percent = Math.min(1f, ptrIndicator.getCurrentPercent());
        percent = ptrIndicator.getCurrentPercent() ;
        if(percent<=1){
            setHeadHeight(getMeasuredHeight());
            setWaveHeight(0);
        }else{
            setHeadHeight((int) (mDefaulHeadHeight*( 3- percent)));
            setWaveHeight((int) ((mDefaulWaveHeight) * Math.max(0, percent - 1)));

        }
        invalidate();
    }

    @Override
    public void onUIReset(SuperNestedLayout parent) {

    }

    @Override
    public void onUIRefreshPrepare(SuperNestedLayout parent) {
        setHeadHeight(getMeasuredHeight());
        setWaveHeight(getMeasuredHeight());
    }

    @Override
    public void onUIRefreshBegin(SuperNestedLayout frame) {
//        setHeadHeight(mDefaulHeadHeight);
//        ValueAnimator animator = ValueAnimator.ofInt(getWaveHeight(),0);
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                Log.i("anim", "value--->" + (int) animation.getAnimatedValue());
//                setWaveHeight((int) animation.getAnimatedValue());
//                invalidate();
//            }
//        });
//        animator.setInterpolator(new BounceInterpolator());
//        animator.setDuration(200);
//        animator.start();
    }



}