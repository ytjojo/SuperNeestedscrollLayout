package com.ytjojo.viewlib.nestedscrolllayout.drawable;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.animation.Interpolator;

import com.ytjojo.viewlib.nestedscrolllayout.Utils;

import java.util.ArrayList;

/**
 * Created by guohuanwen on 2015/10/5.
 */
public class WindowsLoad extends LoadingDrawable {
    private float pi = (float) Math.PI;
    private String TAG = "WindowsLoad";
    private float mRadiu;
    private float circleR;
    private ValueAnimator circleAnimator1;
    private ValueAnimator circleAnimator2;
    private ValueAnimator circleAnimator3;
    private float mTrajectoryRadius;


    public WindowsLoad(Context context) {
        mRadiu = Utils.dip2px(context,3);
    }


    float[] circleCentre;
    float[] start1;
    float[] start2;
    float[] start3;
    ArrayList<PointF> mPointFs;
    ArrayList<float[]> mStartLocations;

    @Override
    public void setPercent(float percent) {
        invalidateSelf();
    }

    @Override
    public void setColorSchemeColors(int[] colorSchemeColors) {
        setColor(colorSchemeColors[0]);
    }

    @Override
    public void draw(Canvas canvas,Paint paint) {
        for (int i = 0; i < 3; i++) {
            canvas.drawCircle(mPointFs.get(i).x, mPointFs.get(i).y, mRadiu, paint);
        }
    }

    @Override
    public void onReset() {
        start1 = new float[]{getWidth() / 2, 2 * mRadiu};
        start2 = onCiecleCoordinate(-0.25f, start1, circleCentre);
        start3 = onCiecleCoordinate(-0.25f, start2, circleCentre);
        mPointFs.get(0).set(start1[0],start1[1]);
        mPointFs.get(1).set(start2[0],start2[1]);
        mPointFs.get(2).set(start3[0],start3[1]);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        ArrayList<ValueAnimator> animators=new ArrayList<>();
        if(mPointFs ==null){
            mPointFs = new ArrayList<>();
            for (int i = 0; i <3 ; i++) {
                mPointFs.add(new PointF());
            }
        }
        mTrajectoryRadius = Math.min(getHeight(),getWidth())/2 -2*mRadiu;
        circleCentre = new float[]{getWidth() / 2, getHeight() / 2};
        start1 = new float[]{getWidth() / 2, 2*mRadiu};
        start2 = onCiecleCoordinate(-0.25f, start1, circleCentre);
        start3 = onCiecleCoordinate(-0.25f, start2, circleCentre);
        mPointFs.get(0).set(start1[0],start1[1]);
        mPointFs.get(1).set(start2[0],start2[1]);
        mPointFs.get(2).set(start3[0],start3[1]);
        mStartLocations = new ArrayList<>();
        mStartLocations.add(start1);
        mStartLocations.add(start2);
        mStartLocations.add(start3);
    }


    @Override
    public ArrayList<ValueAnimator> onCreateAnimators() {
        ArrayList<ValueAnimator> animators=new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int index=i;
            final ValueAnimator valueAnimator =
                    getCircleData(mStartLocations.get(i), circleCentre, i*80);
            addUpdateListener(valueAnimator,new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float curx = (float) (circleCentre[0] + circleR * Math.cos((float) animation.getAnimatedValue()));
                    float cury = (float) (circleCentre[1] + circleR * Math.sin((float) animation.getAnimatedValue()));
                    mPointFs.get(index).set(curx,cury);
                    postInvalidate();
                }
            });
            animators.add(valueAnimator);
        }
        return animators;
    }

    @Override
    public void stopIimmediately() {

    }


    private SlowToQuick slowToQuick = new SlowToQuick();

    private ValueAnimator getCircleData(float[] startCoordinate, float[] RCoordinate, int delay) {
        float x1 = startCoordinate[0];
        float y1 = startCoordinate[1];
        float x0 = RCoordinate[0];
        float y0 = RCoordinate[1];
        circleR = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        float param = (float) (Math.abs(y1 - y0) / circleR);
        if (param < -1.0) {
            param = -1.0f;
        } else if (param > 1.0) {
            param = 1.0f;
        }
        float a = (float) Math.asin(param);
        if (x1 >= x0 && y1 >= y0) {
            a = a;
        } else if (x1 < x0 && y1 >= y0) {
            a = pi - a;
        } else if (x1 < x0 && y1 < y0) {
            a = a + pi;
        } else {
            a = 2 * pi - a;
        }
        ValueAnimator circleAnimator = ValueAnimator.ofFloat(a, a + 2 * pi);
        circleAnimator.setDuration(1000);
        circleAnimator.setInterpolator(slowToQuick);
//        circleAnimator.setInterpolator(new EaseCubicInterpolator(0.31f, 0.85f,0.77f, 0.14f));
        circleAnimator.setInterpolator(new HesitateInterpolator());
        circleAnimator.setRepeatCount(-1);
        circleAnimator.setStartDelay(delay);
        return circleAnimator;
    }

    //获取同一个圆上，间隔固定角度的点坐标
    private float[] onCiecleCoordinate(float angle, float[] start, float[] center) {
        float x1 = start[0];
        float y1 = start[1];
        float x0 = center[0];
        float y0 = center[1];
//        float R = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        float R = mTrajectoryRadius;
        float param = (float) (Math.abs(y1 - y0) / R);
        if (param < -1.0) {
            param = -1.0f;
        } else if (param > 1.0) {
            param = 1.0f;
        }
        float a = (float) Math.asin(param);
        if (x1 >= x0 && y1 >= y0) {
            a = a;
        } else if (x1 < x0 && y1 >= y0) {
            a = pi - a;
        } else if (x1 < x0 && y1 < y0) {
            a = a + pi;
        } else {
            a = 2 * pi - a;
        }
        float x = (float) (center[0] + R * Math.cos(a + angle));
        float y = (float) (center[1] + R * Math.sin(a + angle));
        return new float[]{x, y};
    }

    class SlowToQuick implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            return input * input;
        }
    }
    class HesitateInterpolator implements Interpolator {

        private static final double POW = 1.0/2.0;

        @Override
        public float getInterpolation(float input) {
            return input < 0.5
                    ? (float) Math.pow(input * 2, POW) * 0.5f
                    : (float) Math.pow((1 - input) * 2, POW) * -0.5f + 1;
        }
    }
}