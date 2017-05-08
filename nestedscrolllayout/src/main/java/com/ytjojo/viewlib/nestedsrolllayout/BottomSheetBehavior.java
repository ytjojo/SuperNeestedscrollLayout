package com.ytjojo.viewlib.nestedsrolllayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;

import com.orhanobut.logger.Logger;

/**
 * Created by Administrator on 2017/3/7 0007.
 */

public class BottomSheetBehavior<V extends View> extends Behavior<V> {
    public static final String sBehaviorName = "BottomSheetBehavior";
    public static final int STABLE_STATE_EXPANDED_FULLY = 0;
    public static int STABLE_STATE_EXPANDED = 3;
    public static int STABLE_STATE_COLLAPSED = 4;
    public static int STABLE_STATE_HIDDEN = 4;

    public static final int STATE_EXPANDED_FULLY = 0;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    public static final int STATE_EXPANDED = 3;
    public static final int STATE_COLLAPSED = 4;
    public static final int STATE_HIDDEN = 5;
    public static final int STATE_AUTHORPOINT = 6;
    public static final int STATE_UNKNOWN = -1;


    public static final int PEEK_HEIGHT_AUTO = -1;

    private static final float HIDE_THRESHOLD = 0.5f;

    private static final float HIDE_FRICTION = 0.1f;
    ViewOffsetHelper mViewOffsetHelper;
    int mStableState = STABLE_STATE_EXPANDED;
    int mState = STATE_COLLAPSED;
    boolean canExpandedFully = true;

    private int mPeekHeight;

    private boolean mPeekHeightAuto;

    private int mPeekHeightMin;

    int mMinOffset;

    int mMaxOffset;

    boolean mHideable = true;

    private boolean mSkipCollapsed;
    int mParentHeight;
    int mLastNestedScrollDy;
    View mBottomSheet;
    View mBottomSheetHeader;
    int mMaximumVelocity;
    int mMinimumVelocity;
    int mWindowBackgroundColor = 0xA0A0A0A0;
    @FloatRange(from = 0f, to = 1f)
    float mWindowBackgroundAlpha;

    public BottomSheetBehavior() {
        super();
    }

    public BottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.BottomSheetBehavior);
        TypedValue value = a.peekValue(R.styleable.BottomSheetBehavior_BottomSheet_peekHeight);
        TypedValue minOffsetValue = a.peekValue(R.styleable.BottomSheetBehavior_BottomSheet_minOffset);
        if (minOffsetValue != null && minOffsetValue.data == PEEK_HEIGHT_AUTO) {
            mMinOffset = PEEK_HEIGHT_AUTO;
        } else {
            mMinOffset = a.getDimensionPixelSize(
                    R.styleable.BottomSheetBehavior_BottomSheet_minOffset, PEEK_HEIGHT_AUTO);
        }
        canExpandedFully = a.getBoolean(R.styleable.BottomSheetBehavior_behavior_canExpandedFully, true);
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            setPeekHeight(value.data);
        } else {
            setPeekHeight(a.getDimensionPixelSize(
                    R.styleable.BottomSheetBehavior_BottomSheet_peekHeight, PEEK_HEIGHT_AUTO));
        }
        setHideable(a.getBoolean(R.styleable.BottomSheetBehavior_behavior_hideable, false));
        setSkipCollapsed(a.getBoolean(R.styleable.BottomSheetBehavior_behavior_skipCollapsed,
                false));
        a.recycle();

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMaximumVelocity = (int) (configuration.getScaledMaximumFlingVelocity()*0.5f);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
    }

    @Override
    public boolean hasNestedScrollChild() {
        return true;
    }

    @Override
    public void onAllChildLayouted(NestedScrollLayout nestedScrollLayout, V child) {
        calculateScrollRange(nestedScrollLayout, child);
        if (ViewCompat.getFitsSystemWindows(nestedScrollLayout) && !ViewCompat.getFitsSystemWindows(child)) {
            ViewCompat.setFitsSystemWindows(child, true);
        }
        int peekHeight;
        mParentHeight = nestedScrollLayout.getHeight() -  nestedScrollLayout.getTopInset();

        if (mPeekHeightAuto) {
            if (mPeekHeightMin == 0) {
                mPeekHeightMin = nestedScrollLayout.getResources().getDimensionPixelSize(
                        R.dimen.bottom_sheet_peek_height_min);
            }
            peekHeight = Math.max(mPeekHeightMin, nestedScrollLayout.getWidth() * 9 / 16);
        } else {
            peekHeight = mPeekHeight;
        }
        if (mMinOffset == PEEK_HEIGHT_AUTO) {
            if (mPeekHeightMin == 0) {
                mPeekHeightMin = nestedScrollLayout.getResources().getDimensionPixelSize(
                        R.dimen.bottom_sheet_peek_height_min);
            }
            mMinOffset = Math.max(mPeekHeightMin, nestedScrollLayout.getWidth() * 9 / 16);
        }
        if (mBottomSheetHeader == null) {

            int childHeight = child.getHeight();
            if (childHeight == mParentHeight) {
                canExpandedFully = true;
            } else {
                mMinOffset = Math.max(0, mParentHeight - child.getHeight());
                canExpandedFully = false;
            }
            NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) mBottomSheet.getLayoutParams();
            lp.setScrimColor(mWindowBackgroundColor);
        } else {
            canExpandedFully = true;
            NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) mBottomSheetHeader.getLayoutParams();
            lp.setScrimColor(mWindowBackgroundColor);
        }
        mMaxOffset = Math.max(mParentHeight - peekHeight, mMinOffset);
        if (mState == STATE_EXPANDED) {
            mViewOffsetHelper.setTopAndBottomOffset(mMinOffset);
            dispatchOnSlide(-mMinOffset);
        } else if (mHideable && mState == STATE_HIDDEN) {
            mViewOffsetHelper.setTopAndBottomOffset(mParentHeight);
            dispatchOnSlide(-mParentHeight);
        } else if (mState == STATE_COLLAPSED) {
            mViewOffsetHelper.setTopAndBottomOffset(mMaxOffset);
            dispatchOnSlide(-mMaxOffset);
        } else if (mState == STATE_DRAGGING || mState == STATE_SETTLING) {
            mState = STATE_EXPANDED;
            mViewOffsetHelper.setTopAndBottomOffset(mMinOffset);
            dispatchOnSlide(-mMinOffset);
        }
        if (mBottomSheet != null && mCallback != null) {
            mCallback.onStateChanged(mBottomSheet, mBottomSheetHeader, mState);
        }

    }

    @Override
    public boolean onMeasureChild(NestedScrollLayout nestedScrollLayout, V child, int parentWidthMeasureSpec, int widthUsed,
                                  int parentHeightMeasureSpec, int heightUsed) {
        if (!canExpandedFully) {
            NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
            nestedScrollLayout.measureChildFixed(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed + mMinOffset);
            return true;
        }
        return false;
    }

    public void calculateScrollRange(NestedScrollLayout nestedScrollLayout, V v) {
        initValue(nestedScrollLayout, v);
        mMinScrollY = mDownScrollRange;
        mMaxScrollY = mUpScrollRange;

    }

    boolean mNestedScrollInProgress;

    @Override
    public void onNestedScrollAccepted(NestedScrollLayout nestedScrollLayout, V v,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        super.onNestedScrollAccepted(nestedScrollLayout, v, directTargetChild, target, nestedScrollAxes);
        mWasNestedFlung = false;
        mNestedPreScrollCalled = false;
        isBlocksInteractionBelow = false;
        mNestedScrollInProgress = true;
        resetVelocityData();
        mYVelocity = 0;
        mLastNestedScrollDy = 0;
        initValue(nestedScrollLayout, v);
        mViewOffsetHelper.stopScroll();
        mViewOffsetHelper.resetOffsetTop();

    }

    private void initValue(NestedScrollLayout nestedScrollLayout, V v) {
        mViewOffsetHelper = ViewOffsetHelper.getViewOffsetHelper(v);
        mViewOffsetHelper.setAnimtionCallback(mAnimtionCallback);
        mUpPreScrollRange = getViewRangeEnd(nestedScrollLayout, v);
        mUpScrollRange = mUpPreScrollRange;
        mDownPreScrollRange = mUpPreScrollRange;
        mDownScrollRange = mUpPreScrollRange;
        final int childCount = nestedScrollLayout.getChildCount();
        View scrollHeader = null;
        for (int i = 0; i < childCount; i++) {
            final View child = nestedScrollLayout.getChildAt(i);
            final NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if (lp.isControlViewByBehavior(sBehaviorName)) {
                ViewOffsetHelper.setViewOffsetHelper(child, mViewOffsetHelper);
                if (scrollHeader == null && child != v) {
                    scrollHeader = child;
                    mBottomSheetHeader = scrollHeader;
//                    if (ViewCompat.getFitsSystemWindows(nestedScrollLayout) && !ViewCompat.getFitsSystemWindows(mBottomSheetHeader)) {
//                        ViewCompat.setFitsSystemWindows(mBottomSheetHeader, true);
//                    }
                }
                if (child != scrollHeader) {
                    mViewOffsetHelper.addScrollViews(child);
                }
                if (child == v) {
                    break;
                }
            }
        }
        mBottomSheet = v;
        if (scrollHeader != null) {
            final WindowInsetsCompat lastInsets = nestedScrollLayout.getLastInsets();
            final boolean applyInsets = lastInsets != null && ViewCompat.getFitsSystemWindows(nestedScrollLayout)
                    && !ViewCompat.getFitsSystemWindows(scrollHeader);
            NestedScrollLayout.LayoutParams headerLp = (NestedScrollLayout.LayoutParams) scrollHeader.getLayoutParams();
            NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) v.getLayoutParams();
            int keyValue = lp.getLayoutTop() - lp.topMargin;
            mDownPreScrollRange = keyValue - headerLp.getDownNestedPreScrollRange() - nestedScrollLayout.getTopInset();
            mUpPreScrollRange = keyValue - headerLp.getUpNestedPreScrollRange() - (!headerLp.isEitUntilCollapsed() && isApllyInsets(v, nestedScrollLayout) ? nestedScrollLayout.getTopInset() : 0);
//            mDownScrollRange =keyValue - headerLp.getTotalUnResolvedScrollRange()-(applyInsets?nestedScrollLayout.getTopInset():0);
            mDownScrollRange = -nestedScrollLayout.getPaddingTop() + headerLp.getLayoutTop() - headerLp.topMargin - (applyInsets ? nestedScrollLayout.getTopInset() : 0);
            mUpScrollRange = Math.max(mUpPreScrollRange, mUpScrollRange);
            mViewOffsetHelper.setHeaderView(scrollHeader);
            mViewOffsetHelper.setHeaderViewMinOffsetTop(-mUpPreScrollRange);
            mOverScrollDistance = headerLp.mOverScrollDistance;
        } else {
        }


    }

    public boolean isApllyInsets(View child, NestedScrollLayout nestedScrollLayout) {
        return nestedScrollLayout.getLastInsets() != null && ViewCompat.getFitsSystemWindows(nestedScrollLayout)
                && !ViewCompat.getFitsSystemWindows(child);
    }

    private int getViewRangeEnd(NestedScrollLayout nestedScrollLayout, View v) {
        NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) v.getLayoutParams();
        int parentH = nestedScrollLayout.getMeasuredHeight() - nestedScrollLayout.getPaddingTop() - nestedScrollLayout.getPaddingBottom();
        return lp.getLayoutTop() - lp.topMargin - parentH + v.getMeasuredHeight();
    }

    private int mDownPreScrollRange;
    private int mDownScrollRange;
    private int mUpPreScrollRange;
    private int mUpScrollRange;
    private boolean mSkipNestedPreScrollFling;
    private boolean mNestedPreScrollCalled = false;
    private int mOverScrollDistance;

    @Override
    public void onNestedPreScroll(NestedScrollLayout nestedScrollLayout, V v, View directTargetChild, View target,
                                  int dx, int dy, int[] consumed) {
        if (directTargetChild != v) {
            return;
        }
        mNestedPreScrollCalled = true;
        final int startScrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        int endScrollY = startScrollY;
        int parentScrollDy = 0;

        if (dy != 0) {
            if (dy > 0) {
                if (startScrollY < mUpPreScrollRange) {
                    endScrollY += dy;
                    if (endScrollY > mUpPreScrollRange) {
                        endScrollY = mUpPreScrollRange;
                    }
                    if (endScrollY < 0 && endScrollY >= -mMinOffset) {
                        setStateInternal(STATE_EXPANDED);
                        dispatchOnSlide(endScrollY);
                    } else if (endScrollY >= 0) {
                        if (mState != STATE_EXPANDED_FULLY) {
                            setStateInternal(STATE_EXPANDED_FULLY);
                            dispatchOnSlide(0);
                        }

                    } else {
                        setStateInternal(STATE_DRAGGING);
                        dispatchOnSlide(endScrollY);
                    }
                }
            } else if (dy < 0 && canChildScrollUp(target)) {
                if (startScrollY >= mDownPreScrollRange) {
                    endScrollY += dy;
                    if (endScrollY < mDownPreScrollRange) {
                        endScrollY = mDownPreScrollRange;
                    }
                }
            }
            parentScrollDy = endScrollY - startScrollY;
//            Logger.e( "parentScrollDy =  "+ parentScrollDy+ " start = "+ startScrollY + " end = "+ endScrollY + " dy = "+dy);
            mViewOffsetHelper.setTopAndBottomOffset(-endScrollY);
            consumed[1] = parentScrollDy;
//            Log.e("onNestedPreScroll",startScrollY+ " dy"+ dy + "parentScrollDy" +parentScrollDy+ "mDownPreScrollRange" +mDownPreScrollRange);
//            Logger.e(v.getTop()+"top  y " +v.getY()+" scrollY "+nestedScrollLayout.getScrollY() +"height" +nestedScrollLayout.getMeasuredHeight());
        }
        checkNeedSaveVelocityData(target, dy);
        mLastNestedScrollDy = dy;


    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is TOP custom view.
     */
    public boolean canChildScrollUp(View scrollingTarget) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (scrollingTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) scrollingTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(scrollingTarget, -1) || scrollingTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(scrollingTarget, -1);
        }
    }

    private boolean isOverScrollMode(int scrollY) {
        if (mOverScrollDistance > 0 && scrollY < mDownScrollRange) {
            return true;
        }
        return false;
    }

    @Override
    public void onNestedScroll(NestedScrollLayout nestedScrollLayout, V v, View directTargetChild, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed) {
        if (directTargetChild != v) {
            return;
        }
        final int startScrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        int endScrollY = startScrollY + dyUnconsumed;
        if (dyUnconsumed < 0) {
            mSkipNestedPreScrollFling = true;
            if (mHideable) {
                if (endScrollY < -mParentHeight) {
                    endScrollY = -mParentHeight;
                }
            } else {
                if (endScrollY < -mMaxOffset) {
                    endScrollY = -mMaxOffset;
                }
            }
            if (endScrollY == -mParentHeight) {
                setStateInternal(STATE_HIDDEN);
                dispatchOnSlide(endScrollY);
            } else if (endScrollY <= -mMaxOffset) {
                setStateInternal(STATE_COLLAPSED);
                dispatchOnSlide(endScrollY);
            } else if (endScrollY <= -mMinOffset) {
                setStateInternal(STATE_EXPANDED);
                dispatchOnSlide(endScrollY);
            } else if (endScrollY < 0) {
                setStateInternal(STATE_DRAGGING);
                dispatchOnSlide(endScrollY);
            }
        } else {
            mSkipNestedPreScrollFling = false;
            if (endScrollY > mUpScrollRange) {
                endScrollY = mUpScrollRange;
            }

        }
        final int parentScrollDy = endScrollY - startScrollY;
        mViewOffsetHelper.setTopAndBottomOffset(-endScrollY);
        consumed[1] = parentScrollDy;
    }

    boolean shouldHide(View child, float yvel) {
        if (mSkipCollapsed) {
            return true;
        }
        if (child.getTop() < mMaxOffset) {
            // It should not hide, but collapse.
            return false;
        }
        final float newTop = child.getTop() + yvel * HIDE_FRICTION;
        return Math.abs(newTop - mMaxOffset) / (float) mPeekHeight > HIDE_THRESHOLD;
    }
    boolean shouldExpandFully(View child) {
        if(!canExpandedFully){
            return false;
        }
        if (child.getTop() < mMinOffset) {
            // It should not hide, but collapse.
            return true;
        }
        return false;
    }

    @Override
    public void onStopNestedScroll(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target) {
        if (directTargetChild != child) {
            return;
        }
        mNestedScrollInProgress = false;
        if (!mNestedPreScrollCalled) {
            return;
        }
        int scrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        if (scrollY > mDownScrollRange) {
            preScrollFlingCalculate(nestedScrollLayout, child, target,true);
        } else {
            if(isAuthorPoint&& -scrollY>mMinOffset && -scrollY<mMaxOffset){
                if(canUserVelocity()){
                    mAnimtionCallback.setEndState(STATE_AUTHORPOINT);
                    setStateInternal(STATE_SETTLING);
                    mViewOffsetHelper.fling((int) mYVelocity,mMinOffset,mMaxOffset);
                }else{
                    preScrollFlingCalculate(nestedScrollLayout, child, target,true);
                }
            }else{
                resetVelocityData();
                smoothSlideViewAndSetState(child, scrollY);
            }
        }

        mNestedPreScrollCalled = false;
        mSkipNestedPreScrollFling = false;
        mYVelocity = 0;
    }
    private boolean canUserVelocity(){
        if(Math.abs(mYVelocity) >= mMinimumVelocity ){
            int offsetTop = mViewOffsetHelper.getTopAndBottomOffset();
            if(offsetTop>mMinOffset && offsetTop<mMaxOffset){
                return true;
            }
        }
        return false;
    }
    boolean isAuthorPoint = true;
    private void smoothSlideViewAndSetState(View child, int scrollY) {
        int targetState;
        int top;

        if (mLastNestedScrollDy > 0) {
            if(shouldExpandFully(mBottomSheetHeader!=null?mBottomSheetHeader:mBottomSheet)){
                top = 0;
                targetState = STATE_EXPANDED_FULLY;
            } else {
                top = mMinOffset;
                targetState = STATE_EXPANDED;
            }
        } else if (mHideable && shouldHide(mBottomSheetHeader!=null?mBottomSheetHeader:mBottomSheet, mYVelocity)) {
            top = mParentHeight;
            targetState = STATE_HIDDEN;
        } else if (mLastNestedScrollDy == 0) {
            if (Math.abs(scrollY - mMinOffset) < Math.abs(scrollY - mMaxOffset)) {
                if (canExpandedFully) {
                    top = 0;
                    targetState = STATE_EXPANDED_FULLY;
                } else {
                    top = mMinOffset;
                    targetState = STATE_EXPANDED;
                }
            } else {
                top = mMaxOffset;
                targetState = STATE_COLLAPSED;
            }
        } else {
            top = mMaxOffset;
            targetState = STATE_COLLAPSED;
        }
        mAnimtionCallback.setEndState(targetState);
        if (mViewOffsetHelper.animateOffsetTo(top, mAnimtionCallback)) {
            setStateInternal(STATE_SETTLING);
        } else {
            setStateInternal(targetState);
        }
    }

    AnimtionCallback mAnimtionCallback = new AnimtionCallback(this);

    public static class AnimtionCallback implements ViewOffsetHelper.AnimCallback {
        int endState = STATE_UNKNOWN;
        BottomSheetBehavior behavior;

        AnimtionCallback(BottomSheetBehavior behavior) {
            this.behavior = behavior;
        }

        public void setEndState(int state) {
            this.endState = state;
        }

        @Override
        public void onAnimationUpdate(int value) {
            behavior.dispatchOnSlide(-value);
        }

        @Override
        public void onAnimationEnd() {
            if(endState != STATE_UNKNOWN){
                behavior.setStateInternal(endState);

            }
            endState = STATE_UNKNOWN;
        }
    }

    void setStateInternal(int state) {
        if(state == STATE_AUTHORPOINT){
            if(mViewOffsetHelper.getTopAndBottomOffset() ==mMinOffset){
                state = STATE_EXPANDED;
            }
            if(mViewOffsetHelper.getTopAndBottomOffset() ==mMaxOffset){
                state = STABLE_STATE_COLLAPSED;
            }
        }
        if (mState == state) {
            return;
        }
        mState = state;
        if (mBottomSheet != null && mCallback != null) {
            mCallback.onStateChanged(mBottomSheet, mBottomSheetHeader, state);
        }
    }

    void dispatchOnSlide(int offsetY) {
        Logger.e(offsetY + "offsetY" +offsetY + "mMaxOffset" + mMaxOffset + "mMinOffset" + mMinOffset);
        if (mBottomSheet != null && mCallback != null) {

            mCallback.onSlide(mBottomSheet, mBottomSheetHeader,
                    offsetY, mMinOffset, mMaxOffset);
        }
        if (mWindowBackgroundColor != Color.TRANSPARENT) {
            if (offsetY >= -mMinOffset) {
                mWindowBackgroundAlpha = 1f;
            } else {
                if (offsetY <= -mMaxOffset) {
                    mWindowBackgroundAlpha = 0f;
                } else if (offsetY <= -mMinOffset && offsetY >= -mMaxOffset) {
                    mWindowBackgroundAlpha = 1 + ((float) offsetY + mMinOffset) / ((float) (mMaxOffset - mMinOffset));
                }
            }
            if (mBottomSheetHeader != null) {
                NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) mBottomSheetHeader.getLayoutParams();
                lp.setScrimOpacity(mWindowBackgroundAlpha);
//                mBottomSheetHeader.postInvalidate();
            } else {
                NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) mBottomSheet.getLayoutParams();
                lp.setScrimOpacity(mWindowBackgroundAlpha);

            }
            ((ViewGroup) mBottomSheet.getParent()).postInvalidate();

        }

    }

    private BottomSheetCallback mCallback;

    public void setBottomSheetCallback(BottomSheetCallback callback) {
        mCallback = callback;
    }

    @Override
    public boolean onStartNestedScroll(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        if (directTargetChild != child) {
            return false;
        }
//        Logger.e("onStartNestedScroll" +(child == directTargetChild));
        return true;
    }

    private float mYVelocity;

    @Override
    public boolean onNestedFling(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target, float velocityX, float velocityY, boolean consumed) {
//        Log.e(getClass().getName(), "onNestedFling velocityY ="  + velocityY+ consumed);

        if (directTargetChild != child) {
            return false;
        }
        if (Math.abs(velocityY) > mMaximumVelocity) {
            if (velocityY > 0) {
                velocityY = mMaximumVelocity;
            } else {
                velocityY = -mMaximumVelocity;
            }
        }
        mYVelocity = velocityY;
        int scrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        if (scrollY < mDownScrollRange) {
            return true;
        }
        if (!consumed) {
            if (scrollY > mMinScrollY && scrollY < mMaxScrollY) {
                mViewOffsetHelper.fling((int) velocityY, -mMinScrollY, -mMaxScrollY);
                return true;
            }
        } else {
            if (velocityY > 0) {
                if (scrollY > mUpPreScrollRange) {
                    mViewOffsetHelper.fling((int) velocityY, -mMinScrollY, -mMaxScrollY);
                    return true;
                }
            } else {
                if (scrollY < mUpPreScrollRange) {
                    mViewOffsetHelper.fling((int) velocityY, -mMinScrollY, -mMaxScrollY);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onNestedPreFling(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target, float velocityX, float velocityY) {
//        Log.e(getClass().getName(),   "onNestedPreFling  velocityY =" + velocityY);
        if (directTargetChild != child) {
            return false;
        }
        if(preScrollFlingCalculate(nestedScrollLayout,child,target,false)){
            return true;
        }
        return false;
    }

    boolean isBlocksInteractionBelow;

    @Override
    public void onStartDrag(NestedScrollLayout nestedScrollLayout, V child, int mInitialTouchX, int mInitialTouchY, boolean acceptedByAnother, Behavior accepteBehavior) {
        if (mNestedScrollInProgress) {
            return;
        }
        if (nestedScrollLayout.isPointInChildBounds(child, mInitialTouchX, mInitialTouchY) || (mBottomSheetHeader != null &&
                nestedScrollLayout.isPointInChildBounds(mBottomSheetHeader, mInitialTouchX, mInitialTouchY))) {
            nestedScrollLayout.resetNestedScrollInProgress();
            isBlocksInteractionBelow = true;
            mViewOffsetHelper.stopScroll();
            mViewOffsetHelper.resetOffsetTop();
            setCanAcceptedDrag(true);
            mLastNestedScrollDy = 0;
        }


    }

    @Override
    public boolean blocksInteractionBelow(NestedScrollLayout parent, V child, int mInitialTouchX, int mInitialTouchY) {

        if (mNestedScrollInProgress) {
            return false;
        }
        if (parent.isPointInChildBounds(child, mInitialTouchX, mInitialTouchY) || (mBottomSheetHeader != null &&
                parent.isPointInChildBounds(mBottomSheetHeader, mInitialTouchX, mInitialTouchY))) {
            parent.resetNestedScrollInProgress();
            isBlocksInteractionBelow = true;
            mViewOffsetHelper.stopScroll();
            mViewOffsetHelper.resetOffsetTop();
            setCanAcceptedDrag(true);
            mLastNestedScrollDy = 0;
        }
        return isBlocksInteractionBelow;
    }

    @Override
    public void onScrollBy(NestedScrollLayout nestedScrollLayout, V child, int dx, int dy, int[] consumed) {
        final int startScrollY = -mViewOffsetHelper.getTopAndBottomOffset();

        int endScrollY = startScrollY + dy;

        if (dy < 0) {
            if (startScrollY < -mParentHeight) {
                return;
            }
            if (mHideable) {
                if (endScrollY < -mParentHeight) {
                    endScrollY = -mParentHeight;
                }
            } else {
                if (endScrollY < -mMaxOffset) {
                    endScrollY = -mMaxOffset;
                }
            }
            if (endScrollY == -mParentHeight) {
                setStateInternal(STATE_HIDDEN);
                dispatchOnSlide(endScrollY);
            } else if (endScrollY <= -mMaxOffset) {
                setStateInternal(STATE_COLLAPSED);
                dispatchOnSlide(endScrollY);
            } else if (endScrollY <= -mMinOffset) {
                setStateInternal(STATE_EXPANDED);
                dispatchOnSlide(endScrollY);
            } else if (endScrollY < 0) {
                setStateInternal(STATE_DRAGGING);
                dispatchOnSlide(endScrollY);
            }
        } else {
            if (startScrollY > mMaxScrollY) {
                return;
            }
            if (endScrollY > mMaxScrollY) {
                endScrollY = mMaxScrollY;
            }
            if (endScrollY < 0 && endScrollY >= -mMinOffset) {
                setStateInternal(STATE_EXPANDED);
                dispatchOnSlide(endScrollY);
            } else if (endScrollY >= 0) {
                if (mState != STATE_EXPANDED_FULLY) {
                    setStateInternal(STATE_EXPANDED_FULLY);
                    dispatchOnSlide(0);
                }
            } else {
                setStateInternal(STATE_DRAGGING);
                dispatchOnSlide(endScrollY);
            }
        }
        final int parentScrollDy = endScrollY - startScrollY;
        mViewOffsetHelper.setTopAndBottomOffset(-endScrollY);
        consumed[1] = parentScrollDy;
        mLastNestedScrollDy = dy;

    }

    @Override
    public boolean onFling(NestedScrollLayout nestedScrollLayout, V child, float velocityX, float velocityY) {
        mViewOffsetHelper.resetOffsetTop();
        int scrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        if (scrollY > mDownScrollRange) {
            mViewOffsetHelper.fling((int) velocityY, -mDownScrollRange, -mUpScrollRange);
            return true;
        }
        return false;
    }

    @Override
    public void onStopDrag(NestedScrollLayout nestedScrollLayout) {
        int scrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        if (scrollY > mDownScrollRange) {

        } else {
            smoothSlideViewAndSetState(mBottomSheet, scrollY);
        }
        isBlocksInteractionBelow = false;
        mLastNestedScrollDy = 0;
    }

    @Override
    public int getScrimColor(NestedScrollLayout parent, V child) {
        return mWindowBackgroundColor;
    }

//    @Override
//    public float getScrimOpacity(NestedScrollLayout parent, V child) {
//       if(mWindowBackgroundColor == Color.TRANSPARENT){
//
//           return super.getScrimOpacity(parent, child);
//       }else{
//            return mWindowBackgroundAlpha;
//       }
//    }

    private boolean preScrollFlingCalculate(NestedScrollLayout nestedScrollLayout, V child, View target, boolean isDoFling) {
        if (!mNestedPreScrollCalled || mSkipNestedPreScrollFling || mTotalDy == 0) {
            return false;
        }
        long mTotalDuration = AnimationUtils.currentAnimationTimeMillis() - mBeginTime;
        int velocityY = (int) (mTotalDy * 1000 / mTotalDuration);
        if(Math.abs(velocityY)<mMinimumVelocity){
            return false;
        }
        if (mTotalDy > 0) {
            int offsetTop = mViewOffsetHelper.getTopAndBottomOffset();
            if(isAuthorPoint && offsetTop>mMinOffset && offsetTop<mMaxOffset){
                if(isDoFling){
                    mAnimtionCallback.setEndState(STATE_AUTHORPOINT);
                    setStateInternal(STATE_SETTLING);
                    mViewOffsetHelper.fling(velocityY, mMinScrollY,mMaxOffset);
                }
                return true;
            }else if(-offsetTop < mUpPreScrollRange){
                if(isDoFling){
                    mViewOffsetHelper.fling(velocityY, -mDownScrollRange, -mUpPreScrollRange);
                }
                return true;
            }
        } else {
            if (-mViewOffsetHelper.getTopAndBottomOffset() > mDownPreScrollRange) {
                if(isDoFling){
                    mViewOffsetHelper.fling(velocityY, -mDownPreScrollRange, -mUpPreScrollRange);
                }
                return true;
            }
        }
        return false;

    }

    private void checkNeedSaveVelocityData(View scrollTarget, int dy) {
        if (dy > 0 || dy < 0 && canChildScrollUp(scrollTarget)) {
            saveVelocityYData(dy);
        }
    }

    private void saveVelocityYData(int dy) {

        if (dy == 0 || (dy <= 0 && mTotalDy <= 0) || (dy >= 0 && mTotalDy >= 0)) {
            mTotalDy += dy;
        } else {
            mBeginTime = mLastPreScrollTime;
            mTotalDy = dy;
        }

        mLastPreScrollTime = AnimationUtils.currentAnimationTimeMillis();
//        Log.e(getClass().getName(),"mTotalDy" +mTotalDy+ "mTotalDuration"+mTotalDuration);
    }

    private void resetVelocityData() {
        mTotalDy = 0;
        mBeginTime = AnimationUtils.currentAnimationTimeMillis();
        mLastPreScrollTime = mBeginTime;

    }

    private long mBeginTime;
    private long mLastPreScrollTime;
    private int mTotalDy = 0;
    private boolean mWasNestedFlung;

    /**
     * Callback for monitoring events about bottom sheets.
     */
    public abstract static class BottomSheetCallback {

        /**
         * Called when the bottom sheet changes its state.
         *
         * @param bottomSheet The bottom sheet view.
         * @param newState    The new state. This will be one of {@link #STATE_DRAGGING},
         *                    {@link #STATE_SETTLING}, {@link #STATE_EXPANDED},
         *                    {@link #STATE_COLLAPSED}, or {@link #STATE_HIDDEN}.
         */
        public abstract void onStateChanged(@NonNull View bottomSheet, View header, int newState);

        /**
         * Called when the bottom sheet is being dragged.
         *
         * @param bottomSheet The bottom sheet view.
         * @param slideOffset The new offset of this bottom sheet within [-1,1] range. Offset
         *                    increases as this bottom sheet is moving upward. From 0 to 1 the sheet
         *                    is between collapsed and expanded states and from -1 to 0 it is
         *                    between hidden and collapsed states.
         */
        public abstract void onSlide(@NonNull View bottomSheet, View header, int slideOffset, int minOffset, int maxOffset);
    }


    public final void setPeekHeight(int peekHeight) {
        boolean layout = false;
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!mPeekHeightAuto) {
                mPeekHeightAuto = true;
                layout = true;
            }
        } else if (mPeekHeightAuto || mPeekHeight != peekHeight) {
            mPeekHeightAuto = false;
            mPeekHeight = Math.max(0, peekHeight);
            mMaxOffset = mParentHeight - peekHeight;
            layout = true;
        }
        if (layout && mState == STATE_COLLAPSED && mBottomSheet != null) {
            mBottomSheet.requestLayout();
        }
    }

    public final int getPeekHeight() {
        return mPeekHeightAuto ? PEEK_HEIGHT_AUTO : mPeekHeight;
    }

    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * @param hideable {@code true} to make this bottom sheet hideable.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    public void setHideable(boolean hideable) {
        mHideable = hideable;
    }

    /**
     * Gets whether this bottom sheet can hide when it is swiped down.
     *
     * @return {@code true} if this bottom sheet can hide.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_hideable
     */
    public boolean isHideable() {
        return mHideable;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    public void setSkipCollapsed(boolean skipCollapsed) {
        mSkipCollapsed = skipCollapsed;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once.
     *
     * @return Whether the bottom sheet should skip the collapsed state.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Layout_behavior_skipCollapsed
     */
    public boolean getSkipCollapsed() {
        return mSkipCollapsed;
    }

    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * @param state One of {@link #STATE_COLLAPSED}, {@link #STATE_EXPANDED}, or
     *              {@link #STATE_HIDDEN}.
     */
    public final void setState(final int state) {
        if (state == mState) {
            return;
        }
        if (mBottomSheet == null) {
            // The view is not laid out yet; modify mState and let onLayoutChild handle it later
            if (state == STATE_COLLAPSED || state == STATE_EXPANDED ||
                    (mHideable && state == STATE_HIDDEN)) {
                mState = state;
            }
            return;
        }
        final View child = mBottomSheet;
        // Start the animation; wait until a pending layout if there is one.
        ViewParent parent = child.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
            child.post(new Runnable() {
                @Override
                public void run() {
                    startSettlingAnimation(child, state);
                }
            });
        } else {
            startSettlingAnimation(child, state);
        }
    }

    private void startSettlingAnimation(View child, int state) {
        int top = 0;
        switch (state) {
            case STATE_COLLAPSED:
                top = mMaxOffset;
                break;
            case STATE_EXPANDED:
                top = mMinOffset;
                break;
            case STATE_EXPANDED_FULLY:
                if (canExpandedFully) {
                    top = 0;
                } else {
                    top = mMinOffset;
                    state = STATE_EXPANDED;
                }

                break;
            case STATE_HIDDEN:

                top = mParentHeight;
                break;
            default:
                throw new IllegalArgumentException("Illegal state argument: " + state);


        }
        mAnimtionCallback.setEndState(state);
        if (mViewOffsetHelper.animateOffsetTo(top, mAnimtionCallback)) {
            setStateInternal(STATE_SETTLING);
        } else {
            setStateInternal(state);
        }

    }

    /**
     * Gets the current state of the bottom sheet.
     *
     * @return One of {@link #STATE_EXPANDED}, {@link #STATE_COLLAPSED}, {@link #STATE_DRAGGING},
     * and {@link #STATE_SETTLING}.
     */
    public final int getState() {
        return mState;
    }
}

