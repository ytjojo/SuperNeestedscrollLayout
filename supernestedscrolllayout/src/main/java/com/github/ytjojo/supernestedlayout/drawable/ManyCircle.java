package com.github.ytjojo.supernestedlayout.drawable;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;

/**
 * Created by bigwen on 2016/1/14.
 */
public class ManyCircle extends LoadingDrawable {
    private float maxRadius = 16;
    private ValueAnimator valueAnimator;
    private float radiu = 10;


    private int width;
    private int height;
    private float pi2;
    private float r;

    @Override
    public void setPercent(float percent) {
        if(percent >1f){
            percent = 1f;
        }
        radiu = maxRadius*percent;
        invalidateSelf();
    }

    @Override
    public void setColorSchemeColors(int[] colorSchemeColors) {
        setColor(colorSchemeColors[0]);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        height = width = Math.min(getHeight()/2,getWidth()/2);
        radiu = height/7;
        maxRadius = height/5;
        pi2 = 2*(float)Math.PI;
        r = width-2*maxRadius;
    }

    @Override
    public void draw(Canvas canvas,Paint paint) {


        canvas.drawCircle((float) (centerX() + r * Math.sin(0)), (float) (height + r * Math.cos(0)), f(radiu+0), paint);
        canvas.drawCircle((float) (centerX() + r * Math.sin(pi2 /8)), (float) (height + r * Math.cos(pi2 /8)), f(radiu+2), paint);
        canvas.drawCircle((float) (centerX() + r * Math.sin(pi2 /8*2)), (float) (height + r * Math.cos(pi2 /8*2)), f(radiu+4), paint);
        canvas.drawCircle((float) (centerX() + r * Math.sin(pi2 /8*3)), (float) (height + r * Math.cos(pi2 /8*3)), f(radiu+6), paint);

        canvas.drawCircle((float) (centerX() + r * Math.sin(pi2 /8*4)), (float) (height + r * Math.cos(pi2 /8*4)), f(radiu+8), paint);
        canvas.drawCircle((float) (centerX() + r * Math.sin(pi2 /8*5)), (float) (height + r * Math.cos(pi2 /8*5)), f(radiu+10), paint);
        canvas.drawCircle((float) (centerX() + r * Math.sin(pi2 /8*6)), (float) (height + r * Math.cos(pi2 /8*6)), f(radiu+12), paint);
        canvas.drawCircle((float) (centerX() + r * Math.sin(pi2 /8*7)), (float) (height + r * Math.cos(pi2 /8*7)), f(radiu+14), paint);




    }



    @Override
    public ArrayList<ValueAnimator> onCreateAnimators() {
        ArrayList<ValueAnimator> animators=new ArrayList<>();
        valueAnimator = ValueAnimator.ofFloat(0, maxRadius);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setRepeatCount(-1);
        valueAnimator.setDuration(1000);
        addUpdateListener(valueAnimator, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                radiu = (float) valueAnimator.getAnimatedValue();
                invalidateSelf();
            }
        });
        animators.add(valueAnimator);
        return animators;
    }


    @Override
    public void stopIimmediately() {
        stop();
    }

    //分段函数
    private float f(float x) {
        if (x <=maxRadius / 2) {
            return x;
        } else if(x<maxRadius){
            return maxRadius - x;
        }else
        if(x<maxRadius*3/2)
        {
            return x-maxRadius;
        }else {
            return 2*maxRadius-x;
        }
    }

}