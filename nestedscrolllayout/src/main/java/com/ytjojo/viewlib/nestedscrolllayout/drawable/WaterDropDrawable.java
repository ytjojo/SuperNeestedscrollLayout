package com.ytjojo.viewlib.nestedscrolllayout.drawable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Handler;

import java.security.InvalidParameterException;



/**
 * Created by baoyz on 14/10/31.
 */
public class WaterDropDrawable extends RefreshDrawable implements Runnable {

    private static final float MAX_LEVEL = 10000;
    private static final float MAX_LEVEL_PER_CIRCLE = MAX_LEVEL / 4;

    private int mLevel;
    private Point p1, p2, p3, p4;
    private Paint mPaint;
    private Path mPath;
    private int mCurOffset;
    private int mWidth;
    private int[] mColorSchemeColors;
    private Handler mHandler = new Handler();
    private boolean isRunning;
    int mMaxOffset;
    public WaterDropDrawable(Context context,int maxOffset) {
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPath = new Path();
        p1 = new Point();
        p2 = new Point();
        p3 = new Point();
        p4 = new Point();
        this.mMaxOffset =maxOffset;


    }

    @Override
    public void draw(Canvas canvas) {

        canvas.save();
//        canvas.translate(0,mOffsetY);
        if(mCurOffset <getBounds().height()){

            canvas.translate(0,getBounds().height()- mCurOffset);
        }else{

        }
        mPath.reset();
        mPath.moveTo(p1.x, p1.y);
        mPath.cubicTo(p3.x, p3.y, p4.x, p4.y, p2.x, p2.y);

        canvas.drawPath(mPath, mPaint);

        canvas.restore();
    }
    int mOffsetY;
    @Override
    protected void onBoundsChange(Rect bounds) {
        mWidth = bounds.width();
        isTriger = false;
        updateBounds(0f);
        super.onBoundsChange(bounds);

        mPath.reset();
        mPath.moveTo( mWidth/2 , 0);
        mPath.cubicTo(-mWidth/2,mMaxOffset,mWidth/2,mMaxOffset,mWidth/2,0);
        mOffsetY =( mMaxOffset - getPathRect(mPath).height())/2;


    }
    public static Rect getPathRect(Path path){
        RectF pahtBounds = new RectF();
        path.computeBounds(pahtBounds,true);
        Region region = new Region();
        region.setPath(path,new Region((int) pahtBounds.left,(int) pahtBounds.top,(int) pahtBounds.right,(int) pahtBounds.bottom));
        Rect rect = region.getBounds();
        return rect;
    }

    boolean isTriger;
    private void updateBounds(float percent) {
        mCurOffset = (int) (mMaxOffset *percent);
        int height = mCurOffset;

        if (height > mMaxOffset) {
            height = mMaxOffset;
        }
        int offsetX = 0;
        int offsetY = 0;
        if(percent>=1.2){
            onRefreshBegin();
        }
        if(percent<1){
            isTriger = false;
            p1.set(0, offsetY);
            p2.set(mWidth, offsetY);
            p3.set(mWidth / 3 -height, height);
            p4.set(3* mWidth / 4 + height/2, height);
        }else if(percent >= 1&& percent< 1.2f){
            if(!isTriger){
                offsetX = (int) (mWidth / 2 * ((percent-1)/0.2f));
                offsetY = (int) (mOffsetY* ((percent-1)/0.2f));
                p1.set(offsetX, offsetY);
                p2.set(mWidth - offsetX, offsetY);
                p3.set(mWidth / 2 - height, height);
                p4.set(mWidth / 2 + height, height);
            }

        }else{

        }

    }

    @Override
    public void setColorSchemeColors(int[] colorSchemeColors) {
        if (colorSchemeColors == null || colorSchemeColors.length < 4)
            throw new InvalidParameterException("The color scheme length must be 4");
        mPaint.setColor(colorSchemeColors[0]);
        mColorSchemeColors = colorSchemeColors;
    }

    @Override
    public void setPercent(float percent) {
        mPaint.setColor(evaluate(percent, mColorSchemeColors[0], mColorSchemeColors[1]));
        updateBounds(percent);
        invalidateSelf();
    }

    private void onRefreshBegin(){
        if(!isTriger){
            p1.set(mWidth/2, mOffsetY);
            p2.set(mWidth/2, mOffsetY);
            p3.set(mWidth / 2 - mMaxOffset, mMaxOffset);
            p4.set(mWidth / 2 + mMaxOffset, mMaxOffset);
            isTriger = true;
        }
    }
    private void updateLevel(int level) {
        onRefreshBegin();
        int animationLevel = level == MAX_LEVEL ? 0 : level;

        int stateForLevel = (int) (animationLevel / MAX_LEVEL_PER_CIRCLE);

        float percent = level % 2500 / 2500f;
        int startColor = mColorSchemeColors[stateForLevel];
        int endColor = mColorSchemeColors[(stateForLevel + 1) % 4];
        mPaint.setColor(evaluate(percent, startColor, endColor));
        int value = 0;
        boolean isLeft= false;
        if(level>=0 && level <=2500){
            isLeft = true;
            value = (int) (mMaxOffset/5* level/2500f);
        }else if(level>2500 && level<=5000){
            isLeft = true;
            value = (int) (mMaxOffset/5* (5000-level)/2500f);
        }else if(level>5000 && level<=7500){
            value = (int) (mMaxOffset/5* (level - 5000)/2500f);
        }else {
            value = (int) (mMaxOffset/5* (10000 - level)/2500f);
        }
        p3.set(mWidth / 2 - mMaxOffset + (isLeft?value:value/3), mMaxOffset +value/2);
        p4.set(mWidth / 2 + mMaxOffset - (isLeft?value/3:value), mMaxOffset +value/2);

    }


    @Override
    public void start() {
        mLevel = 2500;
        isRunning = true;
        mHandler.postDelayed(this, 20);
    }

    @Override
    public void stop() {
        mHandler.removeCallbacks(this);
        isRunning = false;
        isTriger = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void run() {
        mLevel += 60;
        if (mLevel > MAX_LEVEL)
            mLevel = 0;
        if (isRunning) {
            mHandler.postDelayed(this, 20);
            updateLevel(mLevel);
            invalidateSelf();
        }
    }

    private int evaluate(float fraction, int startValue, int endValue) {
        int startInt = startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }

}