package com.github.ytjojo.supernestedlayout.drawable;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/5/25 0025.
 */

public class CardiogramDrawable extends LoadingDrawable {
    RectF mRectF = new RectF();
    float mStartAngle = 90;
    float  mSweepAngle =135;
    float mMaxAngle = 135;
    float mEndAngle = 135;
    float mRaduis;

    Path mPath;
    public CardiogramDrawable(Context context){
        super();
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);
        mPath = new Path();
    }
    @Override
    public void setPercent(float percent) {
        invalidateSelf();
        if(percent>1){
            percent =1f;
        }
        if(percent<=1){
            mSweepAngle = mMaxAngle*(percent*0.9f+0.1f);

            mEndAngle = 900 -percent*(450);
            mStartAngle = mEndAngle - mSweepAngle;
//            mStartAngle = 90;
//            mSweepAngle = -45;
        }

    }
    PathMeasure mMeasure;
    Path mDstPath;
    float mPathLength;
    float[] mLocation;
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mRaduis= 3f/4f*Math.min(getHeight(),getWidth())/2f;
        mRectF.set(getWidth()/2-mRaduis,getHeight()/2-mRaduis,getWidth()/2+mRaduis,getHeight()/2+mRaduis);
        mPath.reset();
        float xStart= getWidth()/2 - mRaduis;
        mPath.moveTo(xStart,getHeight()/2);
        mPath.lineTo(xStart + 2*mRaduis/5,getHeight()/2 );
        mPath.lineTo(xStart + 3*mRaduis/5,getHeight()/2 - mRaduis/3);
        mPath.lineTo(xStart +4*mRaduis/5,getHeight()/2 );
        mPath.lineTo(xStart +mRaduis,getHeight()/2 -3* mRaduis/5);

        mPath.lineTo(xStart +7*mRaduis/5,getHeight()/2 + 3*mRaduis/5);
        mPath.lineTo(xStart+8*mRaduis/5,getHeight()/2 );
        mPath.lineTo(xStart +2*mRaduis,getHeight()/2);
        mMeasure = new PathMeasure(mPath, false);
        mPathLength = mMeasure.getLength();
        mDstPath = new Path();
        mLocation = new float[2];
    }

    boolean mDrawPoint;
    boolean mDrawPath;
    @Override
    public void draw(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(mRectF, mStartAngle,mEndAngle-mStartAngle,false,paint);
        if(mDrawPath){
            canvas.drawPath(mDstPath,paint);
            mDstPath.reset();
        }
        if(mDrawPoint){
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(mLocation[0],mLocation[1],3,paint);
        }

    }

    @Override
    public ArrayList<ValueAnimator> onCreateAnimators() {
        ArrayList<ValueAnimator> animators = new ArrayList<>();
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f,2.75f);
        addUpdateListener(valueAnimator, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
              float value = (float) animation.getAnimatedValue();
                if(value<=1f){
                    mDrawPath = false;
                    mEndAngle = 450-value*135;
                    mStartAngle = mEndAngle - mSweepAngle;
//                    if(mStartAngle < 180){
//                        mStartAngle = 180;
//                    }
                }else if(value <= 2.5f){
                    mDrawPath = true;
                    mStartAngle = 180;
                    float ratio = (value-1f)/1.5f;
                    mEndAngle = 450-135-ratio*135;
                    mDstPath.reset();
                    mMeasure.getSegment(0,mPathLength*ratio,mDstPath,true);
                    mMeasure.getPosTan(mPathLength*ratio,mLocation,null);
                    if(ratio>0.99f){
                        mDrawPoint = false;
                    }else {
                        mDrawPoint = true;
                    }

                }else {
                    mDrawPoint = false;
                    mDrawPath = true;
                    mStartAngle = 180;
                    float ratio = (value-2.5f)/0.25f;
                    mEndAngle = 180;
                    mDstPath.reset();
                    mDstPath.set(mPath);

//                    mMeasure.getSegment(mPathLength*ratio,mPathLength,mDstPath,true);
                }
                postInvalidate();
            }
        });
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setRepeatCount(-1);
        valueAnimator.setDuration(1200);
        valueAnimator.setRepeatMode(ValueAnimator.RESTART);
        animators.add(valueAnimator);
        return animators;
    }

    @Override
    public void onReset() {
        mDrawPath = false;
    }
}
