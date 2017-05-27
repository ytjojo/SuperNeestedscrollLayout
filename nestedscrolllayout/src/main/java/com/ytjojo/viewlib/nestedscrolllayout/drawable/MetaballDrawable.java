package com.ytjojo.viewlib.nestedscrolllayout.drawable;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.ytjojo.viewlib.nestedscrolllayout.Utils;

import java.util.ArrayList;

/**
 * https://github.com/dodola/MetaballLoading
 *
 */
public class MetaballDrawable extends LoadingDrawable {

    private float handle_len_rate = 2f;
    private float radius = 30;
    private final int ITEM_COUNT = 6;
    private int mItemDivider = 60;
    private final float SCALE_RATE = 0.3f;
    private float maxLength;
    private ArrayList<Circle> mCirclePaths = new ArrayList<>();
    private float mInterpolatedTime;
    private Circle circle;

    public MetaballDrawable(Context context) {
        super();
        init(context);
    }


    @Override
    public void setPercent(float percent) {

    }

    @Override
    public void setColorSchemeColors(int[] colorSchemeColors) {
        setColor(colorSchemeColors[0]);
    }

    @Override
    public void stopIimmediately() {
        stop();
    }

    private class Circle {
        float[] center;
        float radius;
    }

    public void setPaintMode(int mode) {
        mPaint.setStyle(mode == 0 ? Paint.Style.STROKE : Paint.Style.FILL);
        postInvalidate();
    }

    private void init(Context context) {
       radius = Utils.dip2px(context,15);
       mItemDivider = (int) (2*radius);

        mPaint.setColor(0xff4db9ff);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);

    }

    private float[] getVector(float radians, float length) {
        float x = (float) (Math.cos(radians) * length);
        float y = (float) (Math.sin(radians) * length);
        return new float[]{
                x, y
        };
    }



    /**
     * @param canvas          画布
     * @param j
     * @param i
     * @param v               控制两个圆连接时候长度，间接控制连接线的粗细，该值为1的时候连接线为直线
     * @param handle_len_rate
     * @param maxDistance
     */
    private void metaball(Canvas canvas, int j, int i, float v, float handle_len_rate, float maxDistance) {
        final Circle circle1 = mCirclePaths.get(i);
        final Circle circle2 = mCirclePaths.get(j);

        RectF ball1 = new RectF();
        ball1.left = circle1.center[0] - circle1.radius;
        ball1.top = circle1.center[1] - circle1.radius;
        ball1.right = ball1.left + circle1.radius * 2;
        ball1.bottom = ball1.top + circle1.radius * 2;

        RectF ball2 = new RectF();
        ball2.left = circle2.center[0] - circle2.radius;
        ball2.top = circle2.center[1] - circle2.radius;
        ball2.right = ball2.left + circle2.radius * 2;
        ball2.bottom = ball2.top + circle2.radius * 2;

        float[] center1 = new float[]{
                ball1.centerX(),
                ball1.centerY()
        };
        float[] center2 = new float[]{
                ball2.centerX(),
                ball2.centerY()
        };
        float d = getDistance(center1, center2);

        float radius1 = ball1.width() / 2;
        float radius2 = ball2.width() / 2;
        float pi2 = (float) (Math.PI / 2);
        float u1, u2;


        if (d > maxDistance) {
//            canvas.drawCircle(ball1.centerX(), ball1.centerY(), circle1.radius, paint);
            canvas.drawCircle(ball2.centerX(), ball2.centerY(), circle2.radius, mPaint);
        } else {
            float scale2 = 1 + SCALE_RATE * (1 - d / maxDistance);
            float scale1 = 1 - SCALE_RATE * (1 - d / maxDistance);
            radius2 *= scale2;
//            radius1 *= scale1;
//            canvas.drawCircle(ball1.centerX(), ball1.centerY(), radius1, paint);
            canvas.drawCircle(ball2.centerX(), ball2.centerY(), radius2, mPaint);

        }

//        Log.d("Metaball_radius", "radius1:" + radius1 + ",radius2:" + radius2);
        if (radius1 == 0 || radius2 == 0) {
            return;
        }

        if (d > maxDistance || d <= Math.abs(radius1 - radius2)) {
            return;
        } else if (d < radius1 + radius2) {
            u1 = (float) Math.acos((radius1 * radius1 + d * d - radius2 * radius2) /
                    (2 * radius1 * d));
            u2 = (float) Math.acos((radius2 * radius2 + d * d - radius1 * radius1) /
                    (2 * radius2 * d));
        } else {
            u1 = 0;
            u2 = 0;
        }
//        Log.d("Metaball", "center2:" + Arrays.toString(center2) + ",center1:" + Arrays.toString(center1));
        float[] centermin = new float[]{center2[0] - center1[0], center2[1] - center1[1]};

        float angle1 = (float) Math.atan2(centermin[1], centermin[0]);
        float angle2 = (float) Math.acos((radius1 - radius2) / d);
        float angle1a = angle1 + u1 + (angle2 - u1) * v;
        float angle1b = angle1 - u1 - (angle2 - u1) * v;
        float angle2a = (float) (angle1 + Math.PI - u2 - (Math.PI - u2 - angle2) * v);
        float angle2b = (float) (angle1 - Math.PI + u2 + (Math.PI - u2 - angle2) * v);

//        Log.d("Metaball", "angle1:" + angle1 + ",angle2:" + angle2 + ",angle1a:" + angle1a + ",angle1b:" + angle1b + ",angle2a:" + angle2a + ",angle2b:" + angle2b);


        float[] p1a1 = getVector(angle1a, radius1);
        float[] p1b1 = getVector(angle1b, radius1);
        float[] p2a1 = getVector(angle2a, radius2);
        float[] p2b1 = getVector(angle2b, radius2);

        float[] p1a = new float[]{p1a1[0] + center1[0], p1a1[1] + center1[1]};
        float[] p1b = new float[]{p1b1[0] + center1[0], p1b1[1] + center1[1]};
        float[] p2a = new float[]{p2a1[0] + center2[0], p2a1[1] + center2[1]};
        float[] p2b = new float[]{p2b1[0] + center2[0], p2b1[1] + center2[1]};


//        Log.d("Metaball", "p1a:" + Arrays.toString(p1a) + ",p1b:" + Arrays.toString(p1b) + ",p2a:" + Arrays.toString(p2a) + ",p2b:" + Arrays.toString(p2b));

        float[] p1_p2 = new float[]{p1a[0] - p2a[0], p1a[1] - p2a[1]};

        float totalRadius = (radius1 + radius2);
        float d2 = Math.min(v * handle_len_rate, getLength(p1_p2) / totalRadius);
        d2 *= Math.min(1, d * 2 / (radius1 + radius2));
//        Log.d("Metaball", "d2:" + d2);
        radius1 *= d2;
        radius2 *= d2;

        float[] sp1 = getVector(angle1a - pi2, radius1);
        float[] sp2 = getVector(angle2a + pi2, radius2);
        float[] sp3 = getVector(angle2b - pi2, radius2);
        float[] sp4 = getVector(angle1b + pi2, radius1);
//        Log.d("Metaball", "sp1:" + Arrays.toString(sp1) + ",sp2:" + Arrays.toString(sp2) + ",sp3:" + Arrays.toString(sp3) + ",sp4:" + Arrays.toString(sp4));


        Path path1 = new Path();
        path1.moveTo(p1a[0], p1a[1]);
        path1.cubicTo(p1a[0] + sp1[0], p1a[1] + sp1[1], p2a[0] + sp2[0], p2a[1] + sp2[1], p2a[0], p2a[1]);
        path1.lineTo(p2b[0], p2b[1]);
        path1.cubicTo(p2b[0] + sp3[0], p2b[1] + sp3[1], p1b[0] + sp4[0], p1b[1] + sp4[1], p1b[0], p1b[1]);
        path1.lineTo(p1a[0], p1a[1]);
        path1.close();
        canvas.drawPath(path1, mPaint);

    }

    private float getLength(float[] b) {
        return (float) Math.sqrt(b[0] * b[0] + b[1] * b[1]);
    }

    private float getDistance(float[] b1, float[] b2) {
        float x = b1[0] - b2[0];
        float y = b1[1] - b2[1];
        float d = x * x + y * y;
        return (float) Math.sqrt(d);
    }


    //测试用
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                break;
//            case MotionEvent.ACTION_MOVE:
//                Circle circle = mCirclePaths.get(0);
//                circle.center[0] = event.getX();
//                circle.center[1] = event.getY();
//                invalidate();
//                break;
//            case MotionEvent.ACTION_UP:
//                break;
//        }
//
//        return true;
//    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        int dx = getWidth()-getIntrinsicWidth();
        int dy = getHeight() - getIntrinsicHeight();
        if(dx !=0||dy != 0){
            canvas.translate(dx,dy);
        }
        circle = mCirclePaths.get(0);
        circle.center[0] = maxLength * mInterpolatedTime;

        RectF ball1 = new RectF();
        ball1.left = circle.center[0] - circle.radius;
        ball1.top = circle.center[1] - circle.radius;
        ball1.right = ball1.left + circle.radius * 2;
        ball1.bottom = ball1.top + circle.radius * 2;
        canvas.drawCircle(ball1.centerX(), ball1.centerY(), circle.radius, mPaint);


        for (int i = 1, l = mCirclePaths.size(); i < l; i++) {
            metaball(canvas, i, 0, 0.6f, handle_len_rate, radius * 4f);
        }
        canvas.restore();
    }

    @Override
    public int getIntrinsicHeight() {
        if(mBoudsHeight >0){
            return mBoudsHeight;
        }
        return (int)(2 * radius * 3.5f);
    }

    @Override
    public int getIntrinsicWidth() {
        if(mBoudsHeight>0){
            return mBoudsWidth;
        }
        return (int) (ITEM_COUNT * (radius * 2 + mItemDivider));
    }
    private int mBoudsWidth;
    private int mBoudsHeight;
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        radius = Math.min(getHeight(),getWidth())/7f;
        mItemDivider = (int) (2 *radius);
        mBoudsWidth = getWidth();
        mBoudsHeight = getHeight();

        mCirclePaths.clear();
        Circle circlePath = new Circle();
        circlePath.center = new float[]{(radius + mItemDivider),getHeight()/2};
        circlePath.radius = radius / 4 * 3;
        mCirclePaths.add(circlePath);

        for (int i = 1; i < ITEM_COUNT; i++) {
            circlePath = new Circle();
            circlePath.center = new float[]{(radius * 2 + mItemDivider) * i, getHeight()/2};
            circlePath.radius = radius;
            mCirclePaths.add(circlePath);
        }
        maxLength = (radius * 2 + mItemDivider) * ITEM_COUNT;
    }


    @Override
    public ArrayList<ValueAnimator> onCreateAnimators() {
        ArrayList<ValueAnimator> animators=new ArrayList<>();
       ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f,1f);
        valueAnimator.setDuration(2000);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.setRepeatMode(ValueAnimator.REVERSE);
        addUpdateListener(valueAnimator, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mInterpolatedTime = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        animators.add(valueAnimator);
        return animators;

    }

    @Override
    public void onReset() {
        super.onReset();
        mInterpolatedTime = 0f;
    }
}