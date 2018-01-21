package com.github.ytjojo.supernestedlayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Created by Administrator on 2018/1/21 0021.
 */

public class DispatchTouchEventHandlar {
    private static final String TAG = "DispatchTouchEvent";
    SuperNestedLayout mParent;
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
    private float mOrginalValue;


    public DispatchTouchEventHandlar(SuperNestedLayout parent) {
        this.mParent = parent;
        init(mParent.getContext());
    }

    private void init(Context context) {
        mScroller = ScrollerCompat.create(context, null);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity() * 2;
        mMaximumVelocity = (int) (configuration.getScaledMaximumFlingVelocity() * 0.3f);
        setupAnimators();
    }

    ValueAnimator mValueAnimator;
    ValueAnimator.AnimatorUpdateListener mUpdateListener;

    private void setupAnimators() {
        mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                if ((value != 0f && value != 1f) && value == mOrginalValue) {
                    return;
                }
                if (mScroller.computeScrollOffset()) {

                    int oldY = (int) mOneDrectionDeltaY;
                    int y = mScroller.getCurrY();
                    if (oldY != y) {
                        dispatchScrollDy(y-oldY,y);
                    }
                    mOneDrectionDeltaY = y;
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
                if (mValueAnimator.isRunning()) {
                    stopScroll();
                }
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
                mScroller.computeScrollOffset();
                mIsBeingDragged = !mScroller.isFinished();
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN:
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = (int) MotionEventCompat.getY(ev, index);
                mLastMotionX = (int) MotionEventCompat.getX(ev, index);
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



    static int constrain(int value, int min, int max) {
        if (min == max) {
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
    public final void smoothScrollBy(int dx, int dy,int minY,int maxY,int curY) {
        if (mParent.getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            final int maxScrollY = maxY;
            final int minScrollY = minY;
            final int scrollY = curY;
            dy = constrain(scrollY + dy, maxScrollY, minScrollY) - scrollY;
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




    /**
     * velocityY >0 向上滚动
     *
     * @param velocityY
     * @param minY
     * @param maxY
     */
    public void fling(int velocityY, final int minY, final int maxY,int curY) {
        Log.e("watcher", minY + "minY" + velocityY + "velocityY" + maxY);
        stopScroll();
        if (Math.abs(velocityY) < mMinimumVelocity) {
            velocityY = velocityY > 0 ? mMinimumVelocity : -mMinimumVelocity;
        }
        if (Math.abs(velocityY) > mMaximumVelocity) {
            velocityY = velocityY > 0 ? mMaximumVelocity : -mMaximumVelocity;
        }
        if (velocityY > 0) {
            mScroller.fling(0, curY, 0, velocityY, 0, 0, Math.min(minY, maxY),
                    Math.max(minY, maxY));
        } else {
            mScroller.fling(0, curY, 0, velocityY, 0, 0, Math.min(minY, maxY),
                    Math.max(minY, maxY));
        }
        startAnim();
    }




    private void smoothScrollToFinal() {
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
            dispatchScrollDy(deltaY, mOneDrectionDeltaY);
        } else {
            if (Math.abs(mOneDrectionDeltaY) > 3 * mTouchSlop) {
                mOneDrectionDeltaY = 0;
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int yVelocity = (int) VelocityTrackerCompat.getYVelocity(velocityTracker,
                        mActivePointerId);
                if (yVelocity < mMinimumVelocity) {
                    yVelocity = 0;
                }
                if (mOneDrectionDeltaY > 0) {
                    dispatchFling(false, yVelocity);
                } else {
                    dispatchFling(true, yVelocity);
                }
            }
        }
    }

    private void dispatchFling(boolean isScrollUp, int velocityY) {

    }

    private void dispatchScrollDy(int deltaY, float oneDrectionDeltaY) {
    }
    private void dispatchAbortAnimation(){

    }
    private void startScrollUp() {
        startAnim();
    }

    private void startScrollDown() {
        startAnim();
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

    public static final int ANIMATE_OFFSET_DIPS_PER_SECOND = 300;
    ValueAnimator mAnimator;

    private void animateOffsetTo(final View parent, final int currentOffset, final int offset) {
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
        void onTriger(boolean isScrollUp, int velocityY);
        void onScroll(float dy, int totalOneDirectionDy);
        void abortAnimation();
        void onCancel();
    }

}
