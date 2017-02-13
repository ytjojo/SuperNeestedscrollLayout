package com.ytjojo.viewlib.nestedsrolllayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Created by Administrator on 2016/9/26 0026.
 */
public class TouchEventWatcher {
    private static final String TAG = "TouchEventWatcher";
    NestedScrollLayout mParent;
    OnScrollListener mOnScrollListener;
    private ScrollerCompat mScroller;
    /**
     * Position of the last motion event.
     */
    private int mLastMotionY;
    private int mLastMotionX;
    private float mOneDrectionDeltaY;
    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts his finger).
     */
    private boolean mIsBeingDragged = false;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private boolean mLastScrollDrectionIsUp;
    private float mOrginalValue;


    private float mTotalUnconsumed;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private int mNestedYOffset;
    public TouchEventWatcher(NestedScrollLayout parent) {
        this.mParent = parent;
        init(mParent.getContext());
    }

    private void init(Context context) {
        mScroller = ScrollerCompat.create(context, null);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity()*2;
        mMaximumVelocity = (int) (configuration.getScaledMaximumFlingVelocity()*0.5f);
        setupAnimators();
    }

    ValueAnimator mValueAnimator;
    ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private boolean isNeedCheckHorizontal=false;
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
                    if (oldY != y) {
                        mParent.scrollTo(0,y);
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

    public void onDispatchTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
        * Shortcut the most recurring case: the user is in the dragging
        * state and he is moving his finger.  We want to intercept this
        * motion.
        */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
                    mActivePointerId);
            if (activePointerIndex == -1) {
                Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                return;
            }
            final int y = (int) MotionEventCompat.getY(ev, activePointerIndex);
            int deltaY = mLastMotionY - y;
            if (deltaY * mOneDrectionDeltaY > 0) {
                mOneDrectionDeltaY = deltaY + mOneDrectionDeltaY;
            } else {
                mOneDrectionDeltaY = deltaY;
            }
            doScroll(deltaY);
            mLastMotionY = y;
            initVelocityTrackerIfNotExists();
            mVelocityTracker.addMovement(ev);
            return;
        }

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have TOP valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId
                            + " in onInterceptTouchEvent");
                    break;
                }

                final int y = (int) MotionEventCompat.getY(ev, pointerIndex);
                final int x = (int) MotionEventCompat.getX(ev, pointerIndex);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop
                        && (Math.abs(x - mLastMotionX) <= y)) {
                    mIsBeingDragged = true;
                    mOneDrectionDeltaY = y - mLastMotionY;
                    mLastMotionY = y;
                    mLastMotionX = x;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int y = (int) ev.getY();
//                if(mValueAnimator.isRunning()){
//                    stopScroll();
//                }
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionY = y;
                mLastMotionX = (int) ev.getX();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                mOneDrectionDeltaY = 0;
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged. We need to call computeScrollOffset() first so that
                 * isFinished() is correct.
                */
//                mScroller.computeScrollOffset();
//                mIsBeingDragged = !mScroller.isFinished();
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN:
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = (int) MotionEventCompat.getY(ev, index);
                mLastMotionX = (int) MotionEventCompat.getX(ev,index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    smoothScrollToFinal();
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(velocityTracker,
                            mActivePointerId);

                    smoothScrollToFinal();
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

    }

    public boolean onInterceptTouchEvent(MotionEvent ev){

        if (!mParent.isEnabled()|| mParent.isNestedScrollInProgress()) {
            // Fail fast if we're not in TOP state where TOP swipe is possible
            return false;
        }
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have TOP valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId
                            + " in onInterceptTouchEvent");
                    break;
                }

                final int y = (int) MotionEventCompat.getY(ev, pointerIndex);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop
                        && (mParent.getNestedScrollAxes() & ViewCompat.SCROLL_AXIS_VERTICAL) == 0) {
                    Log.e(getClass().getName(),"onInterceptTouchEvent");
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    mNestedYOffset = 0;
                    final ViewParent parent = mParent.getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int y = (int) ev.getY();

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionY = y;
                mLastMotionX = (int) ev.getX();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged. We need to call computeScrollOffset() first so that
                 * isFinished() is correct.
                */
                mScroller.computeScrollOffset();
//                mIsBeingDragged = !mScroller.isFinished();
                mIsBeingDragged = false;
                Log.e("onInterceptTouchEvent","onInterceptTouchEvent" +mParent.isNestedScrollInProgress());
                mParent.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /* Release the drag */
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                //TODO anim
                mParent.stopNestedScroll();
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        return mIsBeingDragged;
    }
    public boolean onTouchEvent(MotionEvent ev) {

        if (!mParent.isEnabled()|| mParent.isNestedScrollInProgress()) {
            // Fail fast if we're not in TOP state where TOP swipe is possible
            return false;
        }

        initVelocityTrackerIfNotExists();

        MotionEvent vtev = MotionEvent.obtain(ev);

        final int actionMasked = MotionEventCompat.getActionMasked(ev);

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                if(mParent.getChildCount()==0){
                    return false;
                }
                if ((mIsBeingDragged = !mScroller.isFinished())) {
                    final ViewParent parent = mParent.getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stopScroll the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    stopScroll();
                }

                // Remember where the motion event started
                mLastMotionY = (int) ev.getY();
                mLastMotionX = (int) ev.getX();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mParent.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
                        mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int y = (int) MotionEventCompat.getY(ev, activePointerIndex);
                int deltaY = mLastMotionY - y;
                if (mParent.dispatchNestedPreScroll(0, deltaY, mParentScrollConsumed, mParentOffsetInWindow)) {
                    deltaY -= mParentScrollConsumed[1];
                    vtev.offsetLocation(0, mParentOffsetInWindow[1]);
                    mNestedYOffset += mParentOffsetInWindow[1];
                }
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    int x = (int)MotionEventCompat.getX(ev, activePointerIndex);
                    int deltaX= mLastMotionX -x;
                    if(!isNeedCheckHorizontal||(isNeedCheckHorizontal && Math.abs(deltaX) < Math.abs(deltaY) )){
                        final ViewParent parent = mParent.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                        mIsBeingDragged = true;
                        if (deltaY > 0) {
                            deltaY -= mTouchSlop;
                        } else {
                            deltaY += mTouchSlop;
                        }
                        mParent.scrollBy(0,deltaY);
                    }
                }
                if (mIsBeingDragged) {
                    Log.e(getClass().getName(),mIsBeingDragged + "isNestedScrollInProgress" +mParent.isNestedScrollInProgress());
                    // Scroll to follow the motion event
                    mLastMotionY = y - mParentOffsetInWindow[1];

                    final int oldY = getScrollY();
                    mParent.scrollBy(0,deltaY);
                    final int scrolledDeltaY = getScrollY() - oldY;
                    final int unconsumedY = deltaY - scrolledDeltaY;
                    if (mParent.dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mParentOffsetInWindow)) {
                        mLastMotionY -= mParentOffsetInWindow[1];
                        vtev.offsetLocation(0, mParentOffsetInWindow[1]);
                        mNestedYOffset += mParentOffsetInWindow[1];
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {

                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(velocityTracker,
                            mActivePointerId);
                    Log.e(TouchEventWatcher.class.getName(),initialVelocity + "initialVelocity");
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        flingWithNestedDispatch(-initialVelocity);
                    } else if (mScroller.springBack(0, getScrollY(), 0, 0, mParent.getMinScrollY(),
                            mParent.getMaxScrollY())) {
                        startAnim();
                    }
                }else{
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(velocityTracker,
                            mActivePointerId);
                    Log.e(getClass().getName(),initialVelocity + "mIsBeingDragged"+ mIsBeingDragged);
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        if (!mParent.dispatchNestedPreFling(0, -initialVelocity)) {
                            mParent.dispatchNestedFling(0, -initialVelocity, false);
                        }
                    }
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && mParent.getChildCount() > 0) {
                    if (mScroller.springBack(0, getScrollY(), 0, 0, mParent.getMinScrollY(),
                            mParent.getMaxScrollY())) {
                        ViewCompat.postInvalidateOnAnimation(mParent);
                    }
                }
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
        vtev.recycle();
        return true;
    }
    /**
     * Smooth scroll by TOP Y delta
     *
     * @param delta the number of pixels to scroll by on the Y axis
     */
    private void doScrollY(int delta,boolean mSmoothScrollingEnabled) {
        if (delta != 0) {
            if (mSmoothScrollingEnabled) {
                smoothScrollBy(0, delta);
            } else {
                mParent.scrollBy(0, delta);
            }
        }
    }

    static int constrain(int value, int min, int max){
        if(min == max){
            return min;
        }
        if (min > max) {
            min = min ^ max;
            max = min ^ max;
            min = min ^ max;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
    private long mLastScroll;
    static final int ANIMATED_SCROLL_GAP = 250;
    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    public final void smoothScrollBy(int dx, int dy) {
        if (mParent.getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            final int maxScrollY= mParent.getMaxScrollY();
            final int minScrollY = mParent.getMinScrollY();
            final int scrollY = getScrollY();
            dy = constrain(scrollY +dy,maxScrollY,minScrollY) - scrollY;
            mScroller.startScroll(0, scrollY, 0, dy);
            startAnim();
        } else {
            if (!mScroller.isFinished()) {
               stopScroll();
            }
            mParent.scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }


    public boolean flingWithNestedDispatch(int velocityY) {
        final int scrollY = getScrollY();
        boolean canFling = false;
        if(velocityY != 0 && scrollY < mParent.getMaxScrollY()&&scrollY>mParent.getMinScrollY() ){
            canFling =true;
        }
        if (!mParent.dispatchNestedPreFling(0, velocityY)) {
            mParent.dispatchNestedFling(0, velocityY, canFling);
            if (canFling) {
                fling(velocityY);
                return true;
            }else{
                return  false;
            }
        }else{
            return true;
        }
    }


    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityY) {
        fling(velocityY,mParent.getMinScrollY(),mParent.getMaxScrollY());

    }

    /**
     * velocityY >0 向上滚动
     * @param velocityY
     * @param minY
     * @param maxY
     */
    public void fling(int velocityY, final int minY, final int maxY) {
        Log.e("watcher", minY+"minY"+velocityY +"velocityY" + maxY);
        stopScroll();
        if(velocityY < 0){
            if(minY == getScrollY()){
                return;
            }
//            if(getScrollY()-minY <= mTouchSlop){
//                ViewCompat.postOnAnimationDelayed(mParent, new Runnable() {
//                    @Override
//                    public void run() {
//                        mParent.scrollTo(0,minY);
//                    }
//                },16);
//                return;
//            }
        }
        if(velocityY > 0){
            if(maxY == getScrollY()){
                return;
            }
//            if(maxY - getScrollY() <= mTouchSlop){
//                ViewCompat.postOnAnimationDelayed(mParent, new Runnable() {
//                    @Override
//                    public void run() {
//                        mParent.scrollTo(0,maxY);
//                    }
//                },16);
//                return;
//            }
        }
        if(Math.abs(velocityY)<mMinimumVelocity){
            velocityY = velocityY>0?mMinimumVelocity:-mMinimumVelocity;
        }
        if(Math.abs(velocityY) >mMaximumVelocity){
            velocityY = velocityY>0?mMaximumVelocity:-mMaximumVelocity;
        }
        if (mParent.getChildCount() > 0) {
            if(velocityY>0){
                mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, Math.min(minY, maxY),
                        Math.max(minY, maxY));
            }else{
                mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0,  Math.min(minY,maxY),
                        Math.max(minY,maxY));
            }
            startAnim();
        }
    }

    /**
     * 大于0向上滚动顶部，小于0向下滚动底部
     * @param derection
     */
    private void smoothScroll(int derection){
        if(derection >0){
            smoothScrollBy(0,mParent.getMinScrollY()-getScrollY());
        }else if(derection <0){
            smoothScrollBy(0,  mParent.getMaxScrollY() -getScrollY());
        }
    }

    private void smoothScrollToFinal() {
//        if ((mOneDrectionDeltaY == 0 && (mTarget.getTranslationY() == 0 ||mTarget.getTranslationY() ==getScrollRange())) || !isFollowMove) {
//            return;
//        }
        if (mOneDrectionDeltaY > 0) {
            startScrollDown();
        } else {
            startScrollUp();
        }
        mOneDrectionDeltaY = 0;
    }

    private void doScroll(int deltaY) {
        boolean isFollowMove = false;
        if (isFollowMove) {
            final int curScrollY = getScrollY();

            int finalScrollY = curScrollY + deltaY;
//            mTarget.setTranslationY(finalScrollY);
        } else {
            if (Math.abs(mOneDrectionDeltaY) > 3 * mTouchSlop) {
                if (mOneDrectionDeltaY > 0) {
                    mOneDrectionDeltaY = 0;
                    startScrollDown();
                } else {
                    mOneDrectionDeltaY = 0;
                    startScrollUp();
                }
            }
        }
    }

    private void startScrollUp() {
        startAnim();
    }

    private void startScrollDown() {
        startAnim();
    }

    private int getScrollY() {
        return  mParent.getScrollY();
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

    private void endDrag() {
        mIsBeingDragged = false;
        mOneDrectionDeltaY = 0;
        recycleVelocityTracker();
        mParent.stopNestedScroll();
    }

    void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    public static final  int ANIMATE_OFFSET_DIPS_PER_SECOND = 300;
    ValueAnimator mAnimator;
    private void animateOffsetTo(final View parent, final int currentOffset , final int offset) {
        if (currentOffset == offset) {
            if (mAnimator != null && mAnimator.isRunning()) {
                mAnimator.cancel();
            }
            return;
        }

        if (mAnimator == null) {
            mAnimator = new ValueAnimator();
            mAnimator.setInterpolator(new DecelerateInterpolator());
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    int value = (int) animator.getAnimatedValue();
                }
            });
        } else {
            mAnimator.cancel();
        }

        // Set the duration based on the amount of dips we're travelling in
        final float distanceDp = Math.abs(currentOffset - offset) /
                mParent.getResources().getDisplayMetrics().density;
        mAnimator.setDuration(Math.round(distanceDp * 1000 / ANIMATE_OFFSET_DIPS_PER_SECOND));

        mAnimator.setIntValues(currentOffset, offset);
        mAnimator.start();
    }




    public interface OnScrollListener {
        void onTriger(boolean isScrollUp);

        void onScroll(float dy);
    }

}
