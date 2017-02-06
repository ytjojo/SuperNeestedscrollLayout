package com.ytjojo.viewlib.nestedsrolllayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;

/**
 * Created by Administrator on 2016/12/22 0022.
 */
@SuppressWarnings("unchecked")
public class NestedScrollLayout extends FrameLayout implements NestedScrollingChild, NestedScrollingParent, ScrollingView {
//    private View mTarget; // the target of the gesture
//    private View mHeaderView;
//    private View mFooterView;
    private View mBehaviorTouchView;
    private View mNestedScrollingDirectChild;
    private View mNestedScrollingTarget;
    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;
    private int mTopViewIndex = -1;
    private boolean mUsingCustomStart;
    private int mHeaderMargin;
    TouchEventWatcher mEventWatcher;
    private int mState = 0;
    private ScrollerCompat mScroller;

    public NestedScrollLayout(Context context) {
        this(context, null);
    }

    public static boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    public NestedScrollLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, null, 0, 0);

    }

    private boolean isLolipop = Build.VERSION.SDK_INT >= 21;

    LayoutManager mLayoutManager;
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NestedScrollLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        mScroller = ScrollerCompat.create(getContext(), null);
        mLayoutManager = new LayoutManager();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() < 1) {
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mLayoutManager.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() < 1) {
            return;
        }
        mLayoutManager.onLayout(changed,left,top,right,bottom);
        if (mEventWatcher == null) {
            mEventWatcher = new TouchEventWatcher(this);
            setMaxScrollY(getEndPosition());
            scrollTo(0, getEndPosition());
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mTopViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mTopViewIndex;
        } else if (i >= mTopViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        if (b) {
            mEventWatcher.recycleVelocityTracker();
        }
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if (mNestedScrollingTarget != null &&((android.os.Build.VERSION.SDK_INT < 21 && mNestedScrollingTarget instanceof AbsListView)
                ||  !ViewCompat.isNestedScrollingEnabled(mNestedScrollingTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mEventWatcher.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mEventWatcher.onInterceptTouchEvent(event);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(true);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
                                        int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
//                Log.e(getClass().getName(),velocityY + "dispatchNestedFling");
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }


    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    //----------NestScrollingChild;
    //----------NestScrollingParent;

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {

        boolean handled = false;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            final Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                final boolean accepted = viewBehavior.onStartNestedScroll(this, view, child, target,
                        nestedScrollAxes);
                handled |= accepted;

                lp.acceptNestedScroll(accepted);
            } else {
                lp.acceptNestedScroll(false);
            }
        }
        return isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0 && handled;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mNestedScrollingDirectChild = child;
        mNestedScrollingTarget = target;
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
        mEventWatcher.stopScroll();

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (!lp.isNestedScrollAccepted()) {
                continue;
            }
            final Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.onNestedScrollAccepted(this, view, child, target, nestedScrollAxes);
            }
        }
    }

    public boolean isNestedScrollInProgress() {
        return mNestedScrollInProgress;
    }


    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
//            finishSpinner(mTotalUnconsumed);//TODO
            mTotalUnconsumed = 0;
        }
        mSkipNestedPreScroll = false;
        // Dispatch up our nested parent
        stopNestedScroll();
        Log.e(getClass().getName(), "onStopNestedScroll   mWasNestedFlung =" + mWasNestedFlung );

        flingCalculate();
        if (!mWasNestedFlung) {

        }
        mWasNestedFlung = false;


        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (!lp.isNestedScrollAccepted()) {
                continue;
            }
            final Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.onStopNestedScroll(this, view, target);
            }
            lp.resetNestedScroll();
            lp.resetChangedAfterNestedScroll();
        }

        mNestedScrollingDirectChild = null;
        mNestedScrollingTarget = null;
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
        final int oldScrollY =getScrollY();
        if (dyUnconsumed < 0) {
            // If the scrolling view is scrolling down but not consuming, it's probably be at
            // the top of it's content
            scrollBy(0, dyUnconsumed);
            // Set the expanding flag so that onNestedPreScroll doesn't handle any events
            mSkipNestedPreScroll = true;
        } else {
            // As we're no longer handling nested scrolls, reset the skip flag
            mSkipNestedPreScroll = false;
        }

        Log.e(getClass().getName(), "onNestedScroll   dyConsumed =" + dyConsumed + "dyUnconsumed" + dyUnconsumed);
//        final int oldScrollY = getScrollY();
//        scrollBy(0,dyUnconsumed);


        //-----

        final int myConsumed = getScrollY() - oldScrollY;
        final int myUnconsumed = dyUnconsumed - myConsumed;
        if(myUnconsumed!=0){
            // Dispatch up to the nested parent first
            dispatchNestedScroll(dxConsumed, myConsumed, dxUnconsumed, myUnconsumed,
                    null);
        }

    }

    boolean isEnterAlways = false;
    boolean isSnap = false;
    boolean mCanScrollWhenChildScroll = false;
    private long mLastTime;
    boolean mSkipNestedPreScroll;
    private int mTotalDy = 0;
    private long mTotalDuration = 0;
    private boolean  mWasNestedFlung;

    private void saveVelocityYData(int dy, int derection) {

        if (derection > 0) {
            if (dy < 0 || mTotalDy < 0) {
                clearVelocityData();
                if (dy > 0) {
                    mLastTime = AnimationUtils.currentAnimationTimeMillis();
                    mTotalDy = 0;
                }
            } else {
                if (mLastTime != 0) {
                    long curTimeMillis = AnimationUtils.currentAnimationTimeMillis();
                    long duration = curTimeMillis - mLastTime;
                    mTotalDuration += duration;
                    mTotalDy += dy;

                } else {
                    mLastTime = AnimationUtils.currentAnimationTimeMillis();
                }

            }
        } else if (derection < 0) {
            if (dy >= 0 || mTotalDy > 0) {
                clearVelocityData();
                if (dy < 0) {
                    mLastTime = AnimationUtils.currentAnimationTimeMillis();
                    mTotalDy = 0;
                }
            } else {
                if (mLastTime != 0) {
                    long curTimeMillis = AnimationUtils.currentAnimationTimeMillis();
                    long duration = curTimeMillis - mLastTime;
                    mTotalDy += dy;
                    mTotalDuration += duration;
                    Log.e(getClass().getName(), " saveVelocityYData dy=" + dy);
                    mLastTime = curTimeMillis;
                } else {
                    mLastTime = AnimationUtils.currentAnimationTimeMillis();
                }
            }
        } else {
            clearVelocityData();
        }
    }

    private void clearVelocityData() {
        mLastTime = 0;
        mTotalDy = 0;
        mTotalDuration = 0;
    }

    private void flingCalculate() {
        if (mTotalDy == 0) {
            return;
        }
        LayoutParams lp = (LayoutParams) mBehaviorTouchView.getLayoutParams();
        Behavior behavior = lp.getBehavior();
        if (getScrollY() > getEndPosition() || getScrollY() < 0) {
            if (mTotalDy != 0 &&mTotalDuration !=0) {

                fling((int) (mTotalDy * 1000 / mTotalDuration));//增加速度
            } else {
//                Log.e(getClass().getName(), " flingCalculate velocity= isEmpty");
                fling(getScrollY() > getEndPosition() / 2 ? -1 : 1);
            }
        }
        clearVelocityData();

    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        consumed[1] =0;
        if (dy != 0 && !mSkipNestedPreScroll) {
            if (dy > 0) {

                int startScrollY = getScrollY();
                scrollBy(0, dy);
                int dyConsumed = getScrollY() - startScrollY;
                consumed[1] = dyConsumed;
                saveVelocityYData(dy, 1);

            } else {
                if (isEnterAlways) {
                    int startScrollY = getScrollY();
                    scrollBy(0, dy);
                    int dyConsumed = getScrollY() - startScrollY;
                    consumed[1] = 0;
                    if(!mCanScrollWhenChildScroll){
                        consumed[1] = dyConsumed;
                        saveVelocityYData(dy,-1);
                    }
                    Log.e(getClass().getName(), " onNestedPreScroll consumed[1] =" + consumed[1] + "dy=" + dy);
                }
            }
        }
        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if((dy -consumed[1]) != 0){
            if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
                consumed[0] += parentConsumed[0];
                consumed[1] += parentConsumed[1];
            }

        }

    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        Log.e(getClass().getName(), velocityY + "onNestedFling" + consumed);
        clearVelocityData();
        return mWasNestedFlung =mEventWatcher.flingWithNestedDispatch((int) velocityY);

//        if(!consumed){
//            mEventWatcher.flingWithNestedDispatch((int) velocityY);
//            return true;
//        }
//        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        clearVelocityData();
        Log.e(getClass().getName(), "onNestedPreFling" + velocityY);
//        return mEventWatcher.flingWithNestedDispatch((int) velocityY);
        return dispatchNestedPreFling(velocityX, velocityY);
    }


    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mNestedScrollingTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mNestedScrollingTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mNestedScrollingTarget, -1) || mNestedScrollingTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mNestedScrollingTarget, -1);
        }
    }

    private void moveBy(int dy) {

    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityY) {
        mEventWatcher.fling(velocityY);
    }


    /**
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     *
     * @hide
     */
    @Override
    public int computeVerticalScrollRange() {
        final int count = getChildCount();
        if (count == 0) {
            return 0;
        }

//        int scrollRange = mTarget.getBottom();
//        final int scrollY = getScrollY();
//        final int overscrollBottom = Math.max(0, scrollRange);
//        if (scrollY < 0) {
//            scrollRange -= scrollY;
//        } else if (scrollY > overscrollBottom) {
//            scrollRange += scrollY - overscrollBottom;
//        }
//        return scrollRange;
        return super.computeVerticalScrollRange();
    }


    /**
     * {@inheritDoc}
     * <p>
     * <p>This version also clamps the scrolling to the bounds of our child.
     */
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (getChildCount() > 0) {
//            Log.e(getClass().getName(),getEndPosition() +"scrollY" +y);
//            x = clamp(x,0, getWidth());
            y = clamp(y, mMaxScrollY, mMinScrollY);
            if (x != getScrollX() || y != getScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }
    private  int mMinScrollY;
    private int mMaxScrollY;
    private void setMinScrollY(int minScrollY){
        this.mMinScrollY = minScrollY;
    }
    private void setMaxScrollY(int maxScrollY){
        this.mMaxScrollY = maxScrollY;
    }

    private int clamp(int value, int min, int max) {
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

    @Override
    public boolean canScrollVertically(int direction) {
        return super.canScrollVertically(direction);
    }

    /**
     * @hide
     */
    @Override
    public int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    /**
     * @hide
     */
    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    /**
     * @hide
     */
    @Override
    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    /**
     * @hide
     */
    @Override
    public int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset();
    }

    /**
     * @hide
     */
    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }
    public static class LayoutParams extends FrameLayout.LayoutParams {
        /** @hide */
        @IntDef(flag=true, value={
                SCROLL_FLAG_SCROLL,
                SCROLL_FLAG_EXIT_UNTIL_COLLAPSED,
                SCROLL_FLAG_ENTER_ALWAYS,
                SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED,
                SCROLL_FLAG_SNAP
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface ScrollFlags {}

        /**
         * The view will be scroll in direct relation to scroll events. This flag needs to be
         * set for any of the other flags to take effect. If any sibling views
         * before this one do not have this flag, then this value has no effect.
         */
        public static final int SCROLL_FLAG_SCROLL = 0x1;

        /**
         * When exiting (scrolling off screen) the view will be scrolled until it is
         * 'collapsed'. The collapsed height is defined by the view's minimum height.
         *
         * @see ViewCompat#getMinimumHeight(View)
         * @see View#setMinimumHeight(int)
         */
        public static final int SCROLL_FLAG_EXIT_UNTIL_COLLAPSED = 0x2;

        /**
         * When entering (scrolling on screen) the view will scroll on any downwards
         * scroll event, regardless of whether the scrolling view is also scrolling. This
         * is commonly referred to as the 'quick return' pattern.
         */
        public static final int SCROLL_FLAG_ENTER_ALWAYS = 0x4;

        /**
         * An additional flag for 'enterAlways' which modifies the returning view to
         * only initially scroll back to it's collapsed height. Once the scrolling view has
         * reached the end of it's scroll range, the remainder of this view will be scrolled
         * into view. The collapsed height is defined by the view's minimum height.
         *
         * @see ViewCompat#getMinimumHeight(View)
         * @see View#setMinimumHeight(int)
         */
        public static final int SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED = 0x8;
        /**
         * 用于header 当 onNestedScroll dy<0 没有被其他view 消耗
         */
        public static final int SCROLL_FLAG_PIN = 0x16;
        /**
         * 用于header 当 onNestedScroll dy<0 没有被其他view 消耗
         */
        public static final int SCROLL_FLAG_ENTER_EXPAND = 0x32;
        /**
         * 用于footer 当 onNestedScroll dy>0 没有被其他view 消耗
         */
        public static final int SCROLL_FLAG_ENTER_COLLAPSED = 0x64;

        public static final int SCROLL_FLAG_SCROLL_INTERUPTED = 0x128;

        /**
         * Upon a scroll ending, if the view is only partially visible then it will be snapped
         * and scrolled to it's closest edge. For example, if the view only has it's bottom 25%
         * displayed, it will be scrolled off screen completely. Conversely, if it's bottom 75%
         * is visible then it will be scrolled fully into view.
         */
        public static final int SCROLL_FLAG_SNAP = 0x10;

        /**
         * Internal flags which allows quick checking features
         */
        static final int FLAG_QUICK_RETURN = SCROLL_FLAG_SCROLL | SCROLL_FLAG_ENTER_ALWAYS;
        static final int FLAG_SNAP = SCROLL_FLAG_SCROLL | SCROLL_FLAG_SNAP;


        public static final int LAYOUT_FLAG_FRAMLAYOUT = 0x1;
        public static final int LAYOUT_FLAG_LINEARVERTICAL = 0x2;
        public static final int LAYOUT_FLAG_ANTHOR = 0x4;

        public int anchorGravity = Gravity.NO_GRAVITY;
        int mAnchorId = View.NO_ID;

        View mAnchorView;
        View mAnchorDirectChild;
        int mLayoutFlags = 0x1;
        int mScrollFlags = SCROLL_FLAG_SCROLL;
        int mLayoutTop = Integer.MIN_VALUE;
        private static final int INVALID_SCROLL_RANGE = -1;
        public static final int HEAD = 1;
        public static final int BODY = 2;
        public static final int FOOT = 3;
        private View mAttachedView;

        int viewType = BODY;
        private boolean mDidBlockInteraction;
        private boolean mDidAcceptNestedScroll = true;
        private boolean mDidChangeAfterNestedScroll;
        private int mTotalScrollRange = INVALID_SCROLL_RANGE;
        private int mDownPreScrollRange = INVALID_SCROLL_RANGE;
        private int mDownScrollRange = INVALID_SCROLL_RANGE;
        private int mUpPreScrollRange = INVALID_SCROLL_RANGE;
        private int mUpScrollRange = INVALID_SCROLL_RANGE;

        private int mRelativeOffsetRange = INVALID_SCROLL_RANGE;
        float mRelativeOffsetRangeRate;
//        boolean isNestedScrollChild;
        boolean canScrollDownOutOfScreen;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            final TypedArray a = c.obtainStyledAttributes(attrs, com.ytjojo.viewlib.nestedsrolllayout.R.styleable.NestedFramLayout_LayoutParams);
            viewType = a.getInt(com.ytjojo.viewlib.nestedsrolllayout.R.styleable.NestedFramLayout_LayoutParams_nested_viewType, BODY);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        public LayoutParams(LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams p) {
            super(p);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }
        public void setAttactchedView(View v){
            this.mAttachedView = v;
        }
        int mMeasuredHeight;
        int getMeasuredHeight(){
            if(mMeasuredHeight >0){
                return mMeasuredHeight;
            }
            return mMeasuredHeight= mAttachedView.getMeasuredHeight();

        }
        public void acceptNestedScroll(boolean accept) {
            mDidAcceptNestedScroll = accept;
        }
        public void setLayoutTop(int top){
            this.mLayoutTop = top;
        }
        public int getLayoutTop(){
            if(mLayoutTop == Integer.MIN_VALUE){
                throw new IllegalStateException("Could not find layoutTop"+mAttachedView
                        + " after onLayout() setLayoutTop(int) not be called " );
            }
            return mLayoutTop;
        }
        public boolean isNestedScrollAccepted() {
            return mDidAcceptNestedScroll;
        }
        public final int getTotalScrollRange() {
            if (mTotalScrollRange != INVALID_SCROLL_RANGE) {
                return mTotalScrollRange;
            }
            int range =0;
            if ((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                // We're set to scroll so add the child's height
                range += getMeasuredHeight() + this.topMargin + this.bottomMargin;

                if ((mScrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                    // For a collapsing scroll, we to take the collapsed height into account.
                    // We also break straight away since later views can't scroll beneath
                    // us
                    range -= ViewCompat.getMinimumHeight(mAttachedView);
                }
            }
            return mTotalScrollRange = range;
        }
        public int getDownNestedPreScrollRange() {
            if (mDownPreScrollRange != INVALID_SCROLL_RANGE) {
                // If we already have a valid value, return it
                return mDownPreScrollRange;
            }
            int range =0;
            if ((mScrollFlags & LayoutParams.FLAG_QUICK_RETURN) == LayoutParams.FLAG_QUICK_RETURN) {
                // First take the margin into account
                range += this.topMargin + this.bottomMargin;
                // The view has the quick return flag combination...
                if ((mScrollFlags & LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED) != 0) {
                    // If they're set to enter collapsed, use the minimum height
                    range += ViewCompat.getMinimumHeight(mAttachedView);
                } else if ((mScrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                    // Only enter by the amount of the collapsed height
                    range += getMeasuredHeight() - ViewCompat.getMinimumHeight(mAttachedView);
                } else if((mScrollFlags & LayoutParams.SCROLL_FLAG_ENTER_COLLAPSED) != 0){
                    range = 0;
                } else {
                    // Else use the full height
                    range += getMeasuredHeight();
                }
            }
            return mDownPreScrollRange = range;
        }
        public int getDownNestedScrollRange() {
            if (mDownScrollRange != INVALID_SCROLL_RANGE) {
                // If we already have a valid value, return it
                return mDownScrollRange;
            }

            int range = 0;
            int childHeight =getMeasuredHeight();
            childHeight += this.topMargin + this.bottomMargin;
            if ((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                // We're set to scroll so add the child's height
                range += childHeight;

                if ((mScrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                    // For a collapsing exit scroll, we to take the collapsed height into account.
                    // We also break the range straight away since later views can't scroll
                    // beneath us
                    range -= ViewCompat.getMinimumHeight(mAttachedView);
                }else if((mScrollFlags & LayoutParams.SCROLL_FLAG_ENTER_COLLAPSED) !=0){
                    range = 0;
                }else if((mScrollFlags & LayoutParams.SCROLL_FLAG_ENTER_EXPAND) !=0){

                }
            } else {
                // As soon as a view doesn't have the scroll flag, we end the range calculation.
                // This is because views below can not scroll under a fixed view.
            }
            return mDownScrollRange = Math.max(0, range);
        }
        public int getUpNestedPreScrollRange() {
            if (mUpPreScrollRange != INVALID_SCROLL_RANGE) {
                // If we already have a valid value, return it
                return mUpPreScrollRange;
            }
            if((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0){
                if((mScrollFlags & LayoutParams.SCROLL_FLAG_ENTER_EXPAND) !=0 ){
                    return mUpPreScrollRange = 0;
                }
            }
            return mUpPreScrollRange = getTotalScrollRange();
        }
        public int getUpNestedScrollRange() {
            if (mUpScrollRange != INVALID_SCROLL_RANGE) {
                // If we already have a valid value, return it
                return mUpScrollRange;
            }
            if((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0){
                if((mScrollFlags & LayoutParams.SCROLL_FLAG_ENTER_EXPAND) !=0){
                    return mUpScrollRange = 0;
                }
            }
            return mUpScrollRange = getTotalScrollRange();
        }

        private int getRelativeOffsetRange(){
            if(mRelativeOffsetRange != INVALID_SCROLL_RANGE)
            return mRelativeOffsetRange;
            return mRelativeOffsetRange = (int) (getMeasuredHeight()*mRelativeOffsetRangeRate);
        }
        private void setelativeOffsetRangeRate(float rateToHeight){
            mRelativeOffsetRangeRate = rateToHeight;
            if( mAttachedView.getMeasuredHeight() >0){
                mRelativeOffsetRange = (int) (getMeasuredHeight()*rateToHeight);
            }
        }
        /**
         * Set the id of this view's anchor.
         *
         * <p>The view with this id must be a descendant of the CoordinatorLayout containing
         * the child view this LayoutParams belongs to. It may not be the child view with
         * this LayoutParams or a descendant of it.</p>
         *
         * @param id The {@link View#getId() view id} of the anchor or
         *           {@link View#NO_ID} if there is no anchor
         */
        public void setAnchorId(int id) {
            invalidateAnchor();
            mAnchorId = id;
        }

        void invalidateAnchor() {
            mAnchorView = mAnchorDirectChild = null;
        }

        boolean dependsOn(NestedScrollLayout parent, View child, View dependency) {
            return dependency == mAnchorDirectChild;
        }

        /**
         * Locate the appropriate anchor view by the current {@link #setAnchorId(int) anchor id}
         * or return the cached anchor view if already known.
         *
         * @param parent the parent CoordinatorLayout
         * @param forChild the child this LayoutParams is associated with
         * @return the located descendant anchor view, or null if the anchor id is
         *         {@link View#NO_ID}.
         */
        View findAnchorView(NestedScrollLayout parent, View forChild) {
            if (mAnchorId == View.NO_ID) {
                mAnchorView = mAnchorDirectChild = null;
                return null;
            }

            if (mAnchorView == null || !verifyAnchorView(forChild, parent)) {
                resolveAnchorView(forChild, parent);
            }
            return mAnchorView;
        }

        /**
         * Verify that the previously resolved anchor view is still valid - that it is still
         * a descendant of the expected parent view, it is not the child this LayoutParams
         * is assigned to or a descendant of it, and it has the expected id.
         */
        private boolean verifyAnchorView(View forChild, NestedScrollLayout parent) {
            if (mAnchorView.getId() != mAnchorId) {
                return false;
            }

            View directChild = mAnchorView;
            for (ViewParent p = mAnchorView.getParent();
                 p != parent;
                 p = p.getParent()) {
                if (p == null || p == forChild) {
                    mAnchorView = mAnchorDirectChild = null;
                    return false;
                }
                if (p instanceof View) {
                    directChild = (View) p;
                }
            }
            mAnchorDirectChild = directChild;
            return true;
        }

        /**
         * Determine the anchor view for the child view this LayoutParams is assigned to.
         * Assumes mAnchorId is valid.
         */
        private void resolveAnchorView(final View forChild, final NestedScrollLayout parent) {
            mAnchorView = parent.findViewById(mAnchorId);
            if (mAnchorView != null) {
                if (mAnchorView == parent) {
                    if (parent.isInEditMode()) {
                        mAnchorView = mAnchorDirectChild = null;
                        return;
                    }
                    throw new IllegalStateException(
                            "View can not be anchored to the the parent CoordinatorLayout");
                }

                View directChild = mAnchorView;
                for (ViewParent p = mAnchorView.getParent();
                     p != parent && p != null;
                     p = p.getParent()) {
                    if (p == forChild) {
                        if (parent.isInEditMode()) {
                            mAnchorView = mAnchorDirectChild = null;
                            return;
                        }
                        throw new IllegalStateException(
                                "Anchor must not be a descendant of the anchored view");
                    }
                    if (p instanceof View) {
                        directChild = (View) p;
                    }
                }
                mAnchorDirectChild = directChild;
            } else {
                if (parent.isInEditMode()) {
                    mAnchorView = mAnchorDirectChild = null;
                    return;
                }
                throw new IllegalStateException("Could not find NestedFrameLayout descendant view"
                        + " with id " + parent.getResources().getResourceName(mAnchorId)
                        + " to anchor view " + forChild);
            }
        }
        /**
         * Returns true if the anchor id changed to another valid view id since the anchor view
         * was resolved.
         */
        boolean checkAnchorChanged() {
            return mAnchorView == null && mAnchorId != View.NO_ID;
        }

        LinkedList<OnOffsetChangedListener> mOnOffsetChangedListeners;
        public void addOffsetChangedListener(OnOffsetChangedListener l){
            if(mOnOffsetChangedListeners ==null){
                mOnOffsetChangedListeners = new LinkedList<>();
            }
            mOnOffsetChangedListeners.add(l);
        }
        public void removeOffsetChangedListener(OnOffsetChangedListener l){
            if(mOnOffsetChangedListeners !=null){
                mOnOffsetChangedListeners.remove(l);
            }

        }
        public boolean hasOffsetChangedListener(){
            if(mOnOffsetChangedListeners !=null&& !mOnOffsetChangedListeners.isEmpty()){
                return true;
            }
            return false;
        }
        public void onScrollChanged(float ratio,int dy,int offsetPix,int totalRange,int parentScrollDy){

            Log.e("onScrollChanged",mAttachedView +"rage ="+ratio+"dy =" +dy+" offsetPix="+offsetPix+"totalRange ="+totalRange);
        }
        public boolean isVisable(){
            NestedScrollLayout mParent = (NestedScrollLayout) mAttachedView.getParent();
            int visiableTop = (int) (mAttachedView.getTop()+mAttachedView.getY() -mParent.getScrollY());
            return visiableTop >= mParent.getPaddingTop()+ this.topMargin&& visiableTop < mParent.getHeight() - mParent.getPaddingBottom() ;
        }
        public boolean isOutOfTop(){
            NestedScrollLayout mParent = (NestedScrollLayout) mAttachedView.getParent();
            int visiableTop = (int) (mAttachedView.getTop()+mAttachedView.getY() -mParent.getScrollY());
            return visiableTop <mParent.getPaddingTop()+ this.topMargin ;
        }
        public boolean canPreScroll(){
           if(getDownNestedPreScrollRange() !=0){

               NestedScrollLayout mParent = (NestedScrollLayout) mAttachedView.getParent();
               int visiableTop = (int) (mAttachedView.getTop()+mAttachedView.getY() -mParent.getScrollY());
               return visiableTop>=-getTotalScrollRange() && visiableTop <-getTotalScrollRange()+getDownNestedPreScrollRange();
           }else{
               return false;
           }
        }
        public int mScrollYStart;
        public int mScrollYEnd;
//        public boolean isNestedScrollChild(){
//            return isNestedScrollChild;
//        }
        public View getShowAtBollowView(){
            return null;
        }
        Behavior mBehavior;
        public Behavior getBehavior() {
            return mBehavior;
        }
    }

    public View findDirectTargetChild(View target){
        ViewGroup parent = (ViewGroup) target.getParent();
        View directTargetChild = null;
        if(parent == NestedScrollLayout.this){
            directTargetChild = target;
        }else{
            while(parent !=  NestedScrollLayout.this){
                directTargetChild = parent;
                parent= (ViewGroup) parent.getParent();
                if(parent == NestedScrollLayout.this){
                    break;
                }
            }
        }
        return directTargetChild;
    }



}
