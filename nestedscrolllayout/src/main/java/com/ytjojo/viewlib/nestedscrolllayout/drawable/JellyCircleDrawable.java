package com.ytjojo.viewlib.nestedscrolllayout.drawable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.Pools;
import android.util.TypedValue;

/**
 *
 mHeader.setAniBackColor(mHeaderBackColor);
 mHeader.setAniForeColor(mHeaderForeColor);
 mHeader.setRadius(mHeaderCircleSmaller);
 *
 *
 mHeader.releaseDrag();
 *
 mHeader.setRefreshing(false);
 *
 *
 */

public class JellyCircleDrawable extends LoadingDrawable {

    private static final String TAG = "AnimationView";

    private int PULL_HEIGHT;
    private int PULL_DELTA;
    private float mWidthOffset;
    private boolean isRunning;
    private float mMaxPercent;


    private AnimatorStatus mAniStatus = AnimatorStatus.PULL_DOWN;
    Pools.Pool<RectF> mRectFPools;
    Pools.Pool<PointF> mPointFPools;


    @Override
    public void setPercent(float percent) {
        mHeight = (int) (PULL_HEIGHT *percent);
        checkAndSetState();
        invalidateSelf();
        if(percent > 1){
            mHeaderHeight = (int) ((mMaxPercent+1-percent)*PULL_HEIGHT);
            mWaveHeight = (int) ((PULL_DELTA+PULL_HEIGHT)* Math.max(0, percent - 1));
        }else{
            mHeaderHeight = getBounds().height();
            mWaveHeight = 0;
        }

    }

    @Override
    public void setColorSchemeColors(int[] colorSchemeColors) {
//        mBallPaint.setColor(colorSchemeColors[0]);
//        mOutPaint.setColor(colorSchemeColors[0]);
    }

    @Override
    public void start() {
        isRunning = true;
        mHandler.postDelayed(mRunnable,20);
//        releaseDrag();
        springUp();

    }

    @Override
    public void stop() {

        setRefreshing(false);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isRunning = false;
                mHandler.removeCallbacks(mRunnable);
            }
        },DONE_DUR+10);
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean isFullCanvas() {
        return true;
    }

    enum AnimatorStatus {
        PULL_DOWN,
        DRAG_DOWN,
        REL_DRAG,
        SPRING_UP, // rebound to up, the position is less than PULL_HEIGHT
        POP_BALL,
        OUTER_CIR,
        REFRESHING,
        DONE,
        STOP;

        @Override
        public String toString() {
            switch (this) {
                case PULL_DOWN:
                    return "pull down";
                case DRAG_DOWN:
                    return "drag down";
                case REL_DRAG:
                    return "release drag";
                case SPRING_UP:
                    return "spring up";
                case POP_BALL:
                    return "pop ball";
                case OUTER_CIR:
                    return "outer circle";
                case REFRESHING:
                    return "refreshing...";
                case DONE:
                    return "done!";
                case STOP:
                    return "stop";
                default:
                    return "unknown state";
            }
        }
    }


    private Paint mBackPaint;
    private Paint mBallPaint;
    private Paint mOutPaint;
    private Path mPath;

    private int mHeaderHeight;
    private int mWaveHeight;


    public JellyCircleDrawable(Context context,int pullHeght) {
        initView(context,pullHeght);
    }


    private void initView(Context context,int pullHeight) {

        PULL_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
        PULL_DELTA = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
        PULL_HEIGHT = pullHeight;
        PULL_DELTA = pullHeight;
        mMaxPercent = (PULL_HEIGHT+PULL_DELTA+0.0f)/PULL_HEIGHT;
        mWidthOffset = 0.5f;
        mBackPaint = new Paint();
        mBackPaint.setAntiAlias(true);
        mBackPaint.setStyle(Paint.Style.FILL);
        mBackPaint.setColor(0xff8b90af);

        mBallPaint = new Paint();
        mBallPaint.setAntiAlias(true);
        mBallPaint.setColor(0xffffffff);
        mBallPaint.setStyle(Paint.Style.FILL);

        mOutPaint = new Paint();
        mOutPaint.setAntiAlias(true);
        mOutPaint.setColor(0xffffffff);
        mOutPaint.setStyle(Paint.Style.STROKE);
        mOutPaint.setStrokeWidth(5);


        mPath = new Path();
        mRectFPools = new Pools.SimplePool<>(2);
        mPointFPools = new Pools.SimplePool<>(3);
        mRectFPools.release(new RectF());
        mRectFPools.release(new RectF());


    }

    private int mRadius;
    private int mWidth;
    private int mHeight;

    @Override
    protected void onBoundsChange(Rect bounds) {
        mWidth = getBounds().width();
        super.onBoundsChange(bounds);
    }

    protected void checkAndSetState() {
        mRadius = mHeight / 6;
        mWidth = getBounds().width();

        if (mHeight < PULL_HEIGHT) {
            mAniStatus = AnimatorStatus.PULL_DOWN;
        }


        switch (mAniStatus) {
            case PULL_DOWN:
                if (mHeight >= PULL_HEIGHT) {
                    mAniStatus = AnimatorStatus.DRAG_DOWN;
                }
                break;
            case REL_DRAG:
                break;
        }

    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
//        canvas.getClipBounds(getBounds());

        switch (mAniStatus) {
            case PULL_DOWN:
                if(mHeight!=0)
                canvas.drawRect(0, 0, mWidth, getBounds().height(), mBackPaint);
                break;
            case REL_DRAG:
            case DRAG_DOWN:
                drawDrag(canvas);
                break;
            case SPRING_UP:
                drawSpring(canvas, getSpringDelta());
                break;
            case POP_BALL:
                drawPopBall(canvas);
                break;
            case OUTER_CIR:
                drawOutCir(canvas);
                break;
            case REFRESHING:
                drawRefreshing(canvas);
                break;
            case DONE:
                drawDone(canvas);
                break;
            case STOP:
                canvas.drawRect(0, 0, mWidth, getBounds().height(), mBackPaint);
                break;

        }
        canvas.restore();



    }

    private void drawDrag(Canvas canvas) {
        canvas.drawRect(0, 0, mWidth, PULL_HEIGHT, mBackPaint);

        mPath.reset();
        mPath.moveTo(0,0);
        mPath.lineTo(0, mHeaderHeight);
        mPath.quadTo(mWidthOffset * mWidth, mHeaderHeight + mWaveHeight,
                mWidth, mHeaderHeight);
        mPath.lineTo(mWidth, 0);
        canvas.drawPath(mPath, mBackPaint);
    }
    int mMaxSpringWaveHeight;
    private void drawSpring(Canvas canvas, int springDelta) {

        mPath.reset();
        mPath.moveTo(0, 0);
        mPath.lineTo(0, PULL_HEIGHT+PULL_DELTA);
        mPath.quadTo(mWidth / 2, PULL_HEIGHT +PULL_DELTA- springDelta,
                mWidth, PULL_HEIGHT+PULL_DELTA);
        mPath.lineTo(mWidth, 0);
        canvas.drawPath(mPath, mBackPaint);

        int curH = PULL_DELTA+ PULL_HEIGHT - springDelta / 2;

        if (curH > PULL_HEIGHT+PULL_DELTA - PULL_DELTA /4) {

            int leftX = (int) (mWidth / 2 - 2 * mRadius + getSprRatio() * mRadius);
            mPath.reset();
            mPath.moveTo(leftX, curH);
            mPath.quadTo(mWidth / 2, curH - mRadius * getSprRatio() * 2,
                    mWidth - leftX, curH);
            canvas.drawPath(mPath, mBallPaint);
        } else {
            RectF rectF = mRectFPools.acquire();
            rectF.set(mWidth / 2 - mRadius, curH - mRadius ,mWidth / 2 + mRadius, curH + mRadius);
            canvas.drawArc(rectF,
                    180, 180, true, mBallPaint);
            mRectFPools.release(rectF);
            mCenterY = curH - mRadius ;
        }

    }
    private int mCenterY;

    private void drawPopBall(Canvas canvas) {
        mPath.reset();
        mPath.moveTo(0, 0);
        mPath.lineTo(0, PULL_HEIGHT+ PULL_DELTA);
        mPath.quadTo(mWidth / 2,  PULL_DELTA +PULL_HEIGHT/2,
                mWidth, PULL_HEIGHT+PULL_DELTA);
        mPath.lineTo(mWidth, 0);
        canvas.drawPath(mPath, mBackPaint);

        int cirCentStart =PULL_DELTA+ PULL_HEIGHT - PULL_HEIGHT/4;
//        int cirCentStart = (int) (PULL_HEIGHT/2+PULL_DELTA +(PULL_HEIGHT/2)*getPopRatio());

        int cirCenY = (int) (cirCentStart -PULL_HEIGHT/4*getPopRatio());
        RectF rectF = mRectFPools.acquire();
        rectF.set(mWidth / 2 - mRadius, cirCenY - mRadius, mWidth / 2 + mRadius, cirCenY + mRadius);
        canvas.drawArc(rectF,
                180, 180, true, mBallPaint);
        mRectFPools.release(rectF);

        if (getPopRatio() < 1) {
            drawTail(canvas, cirCenY, cirCentStart + 1, getPopRatio());
        } else {
            canvas.drawCircle(mWidth / 2, cirCenY, mRadius, mBallPaint);
        }


    }

    private void drawTail(Canvas canvas, int centerY, int bottom, float fraction) {
        int bezier1w = (int) (mWidth / 2 + (mRadius * 3 / 4) * (1 - fraction));
        PointF bezier1 = mPointFPools.acquire();
        PointF bezier2 = mPointFPools.acquire();
        PointF start = mPointFPools.acquire();
        if(start == null){
            start = new PointF();
            mPointFPools.release(start);
        }
        if(bezier1 ==null){
            bezier1 = new PointF();
            mPointFPools.release(bezier1);
        }
        if(bezier2 == null){
            bezier2 = new PointF();
            mPointFPools.release(bezier2);
        }

        start.set(mWidth / 2 + mRadius, centerY);
        bezier1.set(bezier1w, bottom);
        bezier2.set(bezier1w + mRadius / 2, bottom);

        mPath.reset();
        mPath.moveTo(start.x, start.y);
        mPath.quadTo(bezier1.x, bezier1.y,
                bezier2.x, bezier2.y);
        mPath.lineTo(mWidth - bezier2.x, bezier2.y);
        mPath.quadTo(mWidth - bezier1.x, bezier1.y,
                mWidth - start.x, start.y);
        canvas.drawPath(mPath, mBallPaint);
    }

    private void drawOutCir(Canvas canvas) {
        mPath.reset();
        mPath.moveTo(0, 0);
        mPath.lineTo(0, PULL_HEIGHT+PULL_DELTA);

        mPath.quadTo(mWidth / 2, PULL_HEIGHT +PULL_DELTA -PULL_HEIGHT/2+(PULL_HEIGHT/2)*getOutRatio(),
                mWidth, PULL_HEIGHT+PULL_DELTA);
        mPath.lineTo(mWidth, 0);

        canvas.drawPath(mPath, mBackPaint);
        int innerY = PULL_HEIGHT/2 + PULL_DELTA  ;
        canvas.drawCircle(mWidth / 2, innerY, mRadius, mBallPaint);
    }

    private int mRefreshStart = 90;
    private int mRefreshStop = 90;
    private int TARGET_DEGREE = 270;
    private boolean mIsStart = true;
    private boolean mIsRefreshing = true;

    private void drawRefreshing(Canvas canvas) {
        canvas.drawRect(0, 0, mWidth, PULL_HEIGHT+PULL_DELTA, mBackPaint);

        int innerY = PULL_HEIGHT/2 + PULL_DELTA;
        canvas.drawCircle(mWidth / 2, innerY, mRadius, mBallPaint);
        int outerR = mRadius + 10;

        mRefreshStart += mIsStart ? 3 : 10;
        mRefreshStop += mIsStart ? 10 : 3;
        mRefreshStart = mRefreshStart % 360;
        mRefreshStop = mRefreshStop % 360;

        int swipe = mRefreshStop - mRefreshStart;
        swipe = swipe < 0 ? swipe + 360 : swipe;
        RectF rectF = mRectFPools.acquire();
        rectF.set(mWidth / 2 - outerR, innerY - outerR, mWidth / 2 + outerR, innerY + outerR);
        canvas.drawArc(rectF,
                mRefreshStart, swipe, false, mOutPaint);
        mRectFPools.release(rectF);
        if (swipe >= TARGET_DEGREE) {
            mIsStart = false;
        } else if (swipe <= 10) {
            mIsStart = true;
        }
        if (!mIsRefreshing) {
            applyDone();

        }

    }

    Handler mHandler = new Handler(Looper.getMainLooper());
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                mHandler.postDelayed(this, 20);
                checkAndSetState();
                invalidateSelf();
            }
        }
    };
    // stop refreshing
    public void setRefreshing(boolean isFresh) {
        mIsRefreshing = isFresh;
    }

    private void drawDone(Canvas canvas) {


        int beforeColor = mOutPaint.getColor();
        if (getDoneRatio() < 0.3) {
            canvas.drawRect(0, 0, mWidth, PULL_HEIGHT+PULL_DELTA, mBackPaint);

            int innerY =PULL_DELTA+ PULL_HEIGHT/2;
            canvas.drawCircle(mWidth / 2, innerY, mRadius, mBallPaint);
            int outerR = (int) (mRadius + 10 + 10 * getDoneRatio() / 0.3f);
            int afterColor = Color.argb((int) (0xff * (1 - getDoneRatio() / 0.3f)), Color.red(beforeColor),
                    Color.green(beforeColor), Color.blue(beforeColor));
            mOutPaint.setColor(afterColor);
            RectF rectF = mRectFPools.acquire();
            rectF.set(mWidth / 2 - outerR, innerY - outerR, mWidth / 2 + outerR, innerY + outerR);
            canvas.drawArc(rectF,
                    0, 360, false, mOutPaint);
            mRectFPools.release(rectF);
        }
        mOutPaint.setColor(beforeColor);


        if (getDoneRatio() >= 0.3 && getDoneRatio() < 0.7) {
            canvas.drawRect(0, 0, mWidth,PULL_HEIGHT+PULL_DELTA, mBackPaint);
            float fraction = (getDoneRatio() - 0.3f) / 0.4f;
            int startCentY = PULL_HEIGHT/2 +PULL_DELTA;
            int curCentY = (int) (startCentY + (PULL_HEIGHT/2) * fraction);
            canvas.drawCircle(mWidth / 2, curCentY, mRadius, mBallPaint);
            if (curCentY >= PULL_HEIGHT +PULL_DELTA- mRadius * 2) {
                drawTail(canvas, curCentY, PULL_HEIGHT+PULL_DELTA, (1 - fraction));
            }
        }

        if (getDoneRatio() >= 0.7 && getDoneRatio() <= 1) {
            float fraction = (getDoneRatio() - 0.7f) / 0.3f;
            canvas.drawRect(0, 0, mWidth, PULL_HEIGHT+PULL_DELTA, mBackPaint);
            int leftX = (int) (mWidth / 2 - mRadius - 2 * mRadius * fraction);
            mPath.reset();
            mPath.moveTo(leftX, PULL_HEIGHT+PULL_DELTA);
            mPath.quadTo(mWidth / 2, PULL_HEIGHT+PULL_DELTA - (2*mRadius * (1 - fraction)),
                    mWidth - leftX, PULL_HEIGHT+PULL_DELTA);
            canvas.drawPath(mPath, mBallPaint);
        }

    }

    private int mLastHeight;

    private int getRelHeight() {
        return (int) (mSpriDeta * (1 - getRelRatio()));
    }

    private int getSpringDelta() {
        return (int) (PULL_HEIGHT/2 * getSprRatio());
    }


    private static long REL_DRAG_DUR = 200;

    private long mStart;
    private long mStop;
    private int mSpriDeta;

    public void releaseDrag() {
        mStart = System.currentTimeMillis();
        mStop = mStart + REL_DRAG_DUR;
        mAniStatus = AnimatorStatus.REL_DRAG;
        mSpriDeta = mHeight - PULL_HEIGHT;
    }


    private float getRelRatio() {
        if (System.currentTimeMillis() >= mStop) {
            springUp();
            return 1;
        }
        float ratio = (System.currentTimeMillis() - mStart) / (float) REL_DRAG_DUR;
        return Math.min(ratio, 1);
    }

    private static long SPRING_DUR = 200;
    private long mSprStart;
    private long mSprStop;


    private void springUp() {
        mSprStart = System.currentTimeMillis();
        mSprStop = mSprStart + SPRING_DUR;
        mAniStatus = AnimatorStatus.SPRING_UP;
        invalidateSelf();
    }


    private float getSprRatio() {
        if (System.currentTimeMillis() >= mSprStop) {
            popBall();
            return 1;
        }
        float ratio = (System.currentTimeMillis() - mSprStart) / (float) SPRING_DUR;
        return Math.min(1, ratio);
    }

    private static final long POP_BALL_DUR = 200;
    private long mPopStart;
    private long mPopStop;

    private void popBall() {
        mPopStart = System.currentTimeMillis();
        mPopStop = mPopStart + POP_BALL_DUR;
        mAniStatus = AnimatorStatus.POP_BALL;
        invalidateSelf();
    }

    private float getPopRatio() {
        if (System.currentTimeMillis() >= mPopStop) {
            startOutCir();
            //TODO
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setRefreshing(false);
                }
            },4000);
            return 1;
        }

        float ratio = (System.currentTimeMillis() - mPopStart) / (float) POP_BALL_DUR;
        return Math.min(ratio, 1);
    }

    private static final long OUTER_DUR = 200;
    private long mOutStart;
    private long mOutStop;

    private void startOutCir() {
        mOutStart = System.currentTimeMillis();
        mOutStop = mOutStart + OUTER_DUR;
        mAniStatus = AnimatorStatus.OUTER_CIR;
        mRefreshStart = 90;
        mRefreshStop = 90;
        TARGET_DEGREE = 270;
        mIsStart = true;
        mIsRefreshing = true;
    }

    private float getOutRatio() {
        if (System.currentTimeMillis() >= mOutStop) {
            mAniStatus = AnimatorStatus.REFRESHING;
            mIsRefreshing = true;
            return 1;
        }
        float ratio = (System.currentTimeMillis() - mOutStart) / (float) OUTER_DUR;
        return Math.min(ratio, 1);
    }

    private static final long DONE_DUR = 800;//TODO
    private long mDoneStart;
    private long mDoneStop;

    private void applyDone() {
        mDoneStart = System.currentTimeMillis();
        mDoneStop = mDoneStart + DONE_DUR;
        mAniStatus = AnimatorStatus.DONE;
    }

    private float getDoneRatio() {
        if (System.currentTimeMillis() >= mDoneStop) {
            mAniStatus = AnimatorStatus.STOP;
            if (onViewAniDone != null) {
                onViewAniDone.viewAniDone();
            }
            invalidateSelf();
            isRunning = false;
            mHandler.removeCallbacks(mRunnable);
            return 1;
        }

        float ratio = (System.currentTimeMillis() - mDoneStart) / (float) DONE_DUR;
        return Math.min(ratio, 1);
    }


    private OnViewAniDone onViewAniDone;

    public void setOnViewAniDone(OnViewAniDone onViewAniDone) {
        this.onViewAniDone = onViewAniDone;
    }

    interface OnViewAniDone {
        void viewAniDone();
    }


    public void setAniBackColor(int color) {
        mBackPaint.setColor(color);
    }

    public void setAniForeColor(int color) {
        mBallPaint.setColor(color);
        mOutPaint.setColor(color);
//        setBackgroundColor(color);
    }

    // the height of view is smallTimes times of circle radius
    public void setRadius(int smallTimes) {
        mRadius = mHeight / smallTimes;
    }

    @Override
    public long getDelayScrollInitail() {
        return DONE_DUR;
    }

    @Override
    public void stopIimmediately() {
        mHandler.removeCallbacks(mRunnable);
        isRunning = false;
    }
}
