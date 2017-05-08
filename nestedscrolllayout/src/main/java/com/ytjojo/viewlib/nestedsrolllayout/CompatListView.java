package com.ytjojo.viewlib.nestedsrolllayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ListViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ListView;

/**
 * Created by mkishan on 9/27/2015.
 */
public class CompatListView extends ListView implements NestedScrollingChild {
    private static final int INVALID_POINTER = -1;
    private NestedScrollingChildHelper mHelper;
    private int mLastMotionY;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;
    public static boolean isLolipop = Build.VERSION.SDK_INT >=21;
    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts his finger).
     */
    private boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;


    /**
     * Whether arrow scrolling is animated.
     */
    private boolean mSmoothScrollingEnabled = true;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;

    public CompatListView(Context context) {
        this(context, null);
    }

    public CompatListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHelper = new NestedScrollingChildHelper(this);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mScroller = ScrollerCompat.create(context, null);
        setupAnimators();
        if(!ViewCompat.isNestedScrollingEnabled(this)){
            ViewCompat.setNestedScrollingEnabled(this,true);
        }
    }

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    public CompatListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mHelper.isNestedScrollingEnabled();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
//        Log.e(getClass().getName(),velocityY + "list y");
        return mHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mHelper.dispatchNestedPreFling(velocityX, velocityY);
    }


    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        if(isLolipop){
//            return super.onTouchEvent(ev);
//        }
//        initVelocityTrackerIfNotExists();
//
//        MotionEvent vtev = MotionEvent.obtain(ev);
//
//        final int actionMasked = MotionEventCompat.getActionMasked(ev);
//
//        if (actionMasked == MotionEvent.ACTION_DOWN) {
//            mNestedYOffset = 0;
//        }
//        vtev.offsetLocation(0, mNestedYOffset);
//
//        switch (actionMasked) {
//            case MotionEvent.ACTION_DOWN: {
//                if (getChildCount() == 0) {
//                    return false;
//                }
//
//                mIsBeingDragged = false;
//                // Remember where the motion event started
//                mLastMotionY = (int) ev.getY();
//                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
//                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
//                break;
//            }
//            case MotionEvent.ACTION_MOVE:
//                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
//                        mActivePointerId);
//                if (activePointerIndex == -1) {
//                    Log.e("CompatListView", "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
//                    break;
//                }
//                boolean isHandler =true;
//                final int oldY = getScrollY();
//                final int y = (int) MotionEventCompat.getY(ev, activePointerIndex);
//
//                int deltaY = mLastMotionY - y;
//                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
//                    mIsBeingDragged = true;
////                    superOnTouch(ev);
////                    ev.offsetLocation(0,deltaY);
////                    superOnTouch(ev);
//                    if (deltaY > 0) {
//                        deltaY -= mTouchSlop;
//                    } else {
//                        deltaY += mTouchSlop;
//                    }
//                }
//
//                if (mIsBeingDragged) {
//
//                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
//                        deltaY -= mScrollConsumed[1];
//                        vtev.offsetLocation(0, mScrollOffset[1]);
//                        mNestedYOffset += mScrollOffset[1];
//
//                    }
//                    android.support.v4.widget.ListViewCompat.scrollListBy(this,deltaY);
//                    mLastMotionY = y - mScrollOffset[1];
//                    final int scrolledDeltaY = getScrollY() - oldY;
//                    final int unconsumedY = deltaY - scrolledDeltaY;
//
//                    if (dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset)) {
//                        mLastMotionY -= mScrollOffset[1];
//                        vtev.offsetLocation(0, mScrollOffset[1]);
//                        mNestedYOffset += mScrollOffset[1];
//                    }
//
//
//                }
//                if (mVelocityTracker != null) {
//                    mVelocityTracker.addMovement(vtev);
//                }
//                vtev.recycle();
//                return isHandler;
//            case MotionEvent.ACTION_UP:
//                if (mIsBeingDragged) {
//                    final VelocityTracker velocityTracker = mVelocityTracker;
//                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
//                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(velocityTracker,
//                            mActivePointerId);
//
//                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
//                       isHandler = flingWithNestedDispatch(-initialVelocity,null);
//                    }else{
//                        isHandler =true;
//                        callCancel();
//                    }
//                }else{
//                    isHandler =superOnTouch(ev);
//                }
//                mActivePointerId = INVALID_POINTER;
//                endDrag();
//                vtev.recycle();
//                return isHandler;
//            case MotionEvent.ACTION_CANCEL:
//                mActivePointerId = INVALID_POINTER;
//                endDrag();
//                break;
//            case MotionEventCompat.ACTION_POINTER_DOWN: {
//                final int index = MotionEventCompat.getActionIndex(ev);
//                mLastMotionY = (int) MotionEventCompat.getY(ev, index);
//                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
//                break;
//            }
//            case MotionEventCompat.ACTION_POINTER_UP:
//                onSecondaryPointerUp(ev);
//                mLastMotionY = (int) MotionEventCompat.getY(ev,
//                        MotionEventCompat.findPointerIndex(ev, mActivePointerId));
//                break;
//        }
//
//        if (mVelocityTracker != null) {
//            mVelocityTracker.addMovement(vtev);
//        }
//        boolean value = superOnTouch(ev);
//        vtev.recycle();
//        return value;
//    }
    int mSuperEventOffsetLocationY = 0;
    int mFirstDownY;
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isLolipop) {
            return super.onTouchEvent(ev);
        }
        initVelocityTrackerIfNotExists();

        MotionEvent vtev = MotionEvent.obtain(ev);

        final int actionMasked = MotionEventCompat.getActionMasked(ev);

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
            mSuperEventOffsetLocationY = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {
                    return false;
                }
                mIsBeingDragged = false;
                // Remember where the motion event started
                mLastMotionY = (int) ev.getY();
                mFirstDownY = mLastMotionY;
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
                        mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e("CompatListView", "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }
                boolean isHandler = true;

                final int oldY = getScrollY();
                final int y = (int) MotionEventCompat.getY(ev, activePointerIndex);
                int firstMoveY = 0;
                int deltaY = mLastMotionY - y;
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    mIsBeingDragged = true;
//                    superOnTouch(ev);
//                    ev.offsetLocation(0, deltaY);
//                    firstMoveY = deltaY;
//                    superOnTouch(ev);
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }

                if (mIsBeingDragged) {
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                        deltaY -= mScrollConsumed[1];
                        vtev.offsetLocation(0, mScrollOffset[1]);
                        mNestedYOffset += mScrollOffset[1];
                        Log.e(getClass().getName(), "mNestedYOffset = " + mNestedYOffset + " cury=" + y + " deltaY=" + deltaY + " mScrollOffset[1]= " + mScrollOffset[1] + " mScrollConsumed[1]=" + mScrollConsumed[1]);
//                        Log.e(getClass().getName(),deltaY + "deltaY" +mScrollConsumed[1] + "mScrollConsumed"+ "mScrollOffset[1]"+ mScrollOffset[1]);
//                        Log.e(getClass().getName(),mLastMotionY + "mLastMotionY" +mScrollConsumed[1] + "mScrollConsumed"+ "mScrollOffset[1]"+ mScrollOffset[1]);
                    }

                    mSuperEventOffsetLocationY += deltaY;
                    ev.setLocation(ev.getX(activePointerIndex), mFirstDownY - mSuperEventOffsetLocationY);
                    Log.e(getClass().getName(), "mSuperEventOffsetLocationY = " + mSuperEventOffsetLocationY + "mNestedYOffset = " + mNestedYOffset + " cury=" + y + " deltaY=" + deltaY);
                    isHandler = true;

//                    ListViewCompat.scrollListBy(this,deltaY);
                    mLastMotionY = y - mScrollOffset[1];
                    boolean canDispatch = false;
                    if(deltaY >0){
                        if(!ViewCompat.canScrollVertically(this,1)){
                            canDispatch = true;
                        }
                    }else if(deltaY < 0){
                        if(!ViewCompat.canScrollVertically(this,-1)){
                            canDispatch = true;
                        }
                    }
                    if(canDispatch){
                        final int scrolledDeltaY =getScrollDy(ev);
                        final int unconsumedY = deltaY - scrolledDeltaY;
                        if (dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset)) {
                            mLastMotionY -= mScrollOffset[1];
                            vtev.offsetLocation(0, mScrollOffset[1]);
                            mNestedYOffset += mScrollOffset[1];
                            mSuperEventOffsetLocationY -= mScrollOffset[1];
//                        Log.e(getClass().getName(),deltaY + "deltaY" +unconsumedY + "unconsumedY"+ "unconsumedY"+ mScrollOffset[1]);
                        }
                    }else{
                        superOnTouch(ev);
                    }



                }
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(vtev);
                }
//                mSuperEventOffsetLocationY += firstMoveY;
                vtev.recycle();
                return isHandler;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(velocityTracker,
                            mActivePointerId);
                    ev.offsetLocation(0, mSuperEventOffsetLocationY);
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        isHandler = flingWithNestedDispatch(-initialVelocity, ev);
                    } else {
                        isHandler = superOnTouch(vtev);
                    }
                } else {
                    isHandler = superOnTouch(vtev);
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                vtev.recycle();
                return isHandler;
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = (int) MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) MotionEventCompat.getY(ev,
                        MotionEventCompat.findPointerIndex(ev, mActivePointerId));
                break;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }

        ev.setLocation(ev.getX(), mFirstDownY - mSuperEventOffsetLocationY);
        boolean value = superOnTouch(ev);
        vtev.recycle();
        return value;
    }
    private boolean superOnTouch(MotionEvent ev) {
//        final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
//                mActivePointerId);
//        int cury = (int) MotionEventCompat.getY(ev, activePointerIndex);
//        Log.e(getClass().getName(), (cury) + "cur mFirstY =" + mFirstDownY + mIsBeingDragged);
        return super.onTouchEvent(ev);
    }

    private int getScrollDy(MotionEvent ev){
        View c = this.getChildAt(0);
        if (c == null) {
            return 0;
        }
        int top = c.getTop();
        superOnTouch(ev);
        return c.getTop() -top;
    }

    private void callCancel() {
        final long now = SystemClock.uptimeMillis();
        final MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
        super.onTouchEvent(cancelEvent);
        cancelEvent.recycle();
    }

    private void endDrag() {
        mIsBeingDragged = false;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        stopNestedScroll();
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >>
                MotionEventCompat.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose TOP new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = (int) MotionEventCompat.getY(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private boolean flingWithNestedDispatch(int velocityY, MotionEvent event) {
        final boolean canFling = (velocityY < 0 && ViewCompat.canScrollVertically(this, -1)) || (velocityY > 0 && ViewCompat.canScrollVertically(this, 1));
        if (!dispatchNestedPreFling(0, velocityY)) {
            dispatchNestedFling(0, velocityY, canFling);
            if (canFling) {
                if(event!=null){
                    return super.onTouchEvent(event);
                }else{
                }

            } else {
                if(event !=null){
                    return super.onTouchEvent(event);
                }else{
                    callCancel();
                }

            }

            return true;
        } else {
            callCancel();
            return true;
        }
    }

    ValueAnimator mValueAnimator;
    ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private ScrollerCompat mScroller;
    private float mOrginalValue;
    private void setupAnimators() {
        mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                if ((value != 0f && value != 1f) && value == mOrginalValue) {
                    return;
                }
                if (mScroller.computeScrollOffset()) {

                    int oldY = getScrollY();
                    int y = mScroller.getCurrY();
                    int delty = y -oldY;
                    if (oldY != y) {
                        ListViewCompat.scrollListBy(CompatListView.this,delty);
                    }
                    if(!ViewCompat.canScrollVertically(CompatListView.this,-delty)){
                        stopScroll();
                    }
                } else {
                    stopScroll();
                }
                mOrginalValue = value;
            }
        };
        mValueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mValueAnimator.setRepeatCount(Animation.INFINITE);
        mValueAnimator.setRepeatMode(ValueAnimator.RESTART);
        mValueAnimator.setDuration(1000);
        //fuck you! the default interpolator is AccelerateDecelerateInterpolator
        mValueAnimator.setInterpolator(new LinearInterpolator());
    }

    public void startAnim() {
        mValueAnimator.removeAllUpdateListeners();
        mValueAnimator.addUpdateListener(mUpdateListener);
        mValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimator.setDuration(1000);
        mValueAnimator.start();
    }

    public void stopScroll() {
        mValueAnimator.removeUpdateListener(mUpdateListener);
        mValueAnimator.removeAllUpdateListeners();
        mValueAnimator.setRepeatCount(0);
        mValueAnimator.setDuration(0);
        mValueAnimator.end();
        mScroller.abortAnimation();
    }
    private void fingListView(int velocityY){
        if(velocityY>0){
            mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0,0,
                    getMeasuredHeight()*2);
        }else{
            mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0,0,
                    getMeasuredHeight()*2);
        }
        startAnim();
    }


}