package com.ytjojo.viewlib.nestedsrolllayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.Pools;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.ScrollerCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Administrator on 2016/12/22 0022.
 */
@SuppressWarnings("unchecked")
public class NestedScrollLayout extends FrameLayout implements NestedScrollingChild, NestedScrollingParent, ScrollingView {
    private View mBehaviorTouchView;
    private View mNestedScrollingDirectChild;
    private View mNestedScrollingTarget;
    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];

    private Paint mScrimPaint;
    private boolean mNestedScrollInProgress;
    private int mTopViewIndex = -1;
    TouchEventWatcher mEventWatcher;
    private ScrollerCompat mScroller;
    private static final Pools.Pool<Rect> sRectPool = new Pools.SynchronizedPool<>(12);

    @NonNull
    private static Rect acquireTempRect() {
        Rect rect = sRectPool.acquire();
        if (rect == null) {
            rect = new Rect();
        }
        return rect;
    }

    private static void releaseTempRect(@NonNull Rect rect) {
        rect.setEmpty();
        sRectPool.release(rect);
    }

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
        super(context, attrs, defStyleAttr);
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        mScroller = ScrollerCompat.create(getContext(), null);
        mLayoutManager = new LayoutManager(this);
        setupForWindowInsets();
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SuperNestedScrollLayout,
                defStyleAttr, R.style.Widget_Design_SuperNestedLayout);
        mStatusBarBackground = a.getDrawable(R.styleable.SuperNestedScrollLayout_statusBarBackground);
        a.recycle();
    }

    private boolean isLolipop = Build.VERSION.SDK_INT >= 21;

    LayoutManager mLayoutManager;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mLastInsets == null && ViewCompat.getFitsSystemWindows(this)) {
            // We're set to fitSystemWindows but we haven't had any insets yet...
            // We should request a new dispatch of window insets
            ViewCompat.requestApplyInsets(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() < 1) {
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mLayoutManager.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    public void measureChildFixed(View child,
                                        int parentWidthMeasureSpec, int widthUsed,
                                        int parentHeightMeasureSpec, int heightUsed){
        measureChildWithMargins(child,parentWidthMeasureSpec,widthUsed,parentHeightMeasureSpec,heightUsed);
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
        mLayoutManager.onLayout(changed, left, top, right, bottom);
        if (mEventWatcher == null) {
            mEventWatcher = new TouchEventWatcher(this);
//            scrollTo(0, getEndPosition());
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
        if(mEventWatcher.isBlockingInteractionBelow!=null&&mEventWatcher.isBlockingInteractionBelow){
            return;
        }
        if (b) {
            mEventWatcher.recycleVelocityTracker();
        }
        // if this is TOP List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
//        if (mNestedScrollingTarget != null && !ViewCompat.isNestedScrollingEnabled(mNestedScrollingTarget)) {
//            // Nope.
//        } else {
//        }
        super.requestDisallowInterceptTouchEvent(b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mEventWatcher.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mEventWatcher.onInterceptTouchEvent(event);
//        return false;
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
            if(view.getVisibility() ==GONE){
                continue;
            }
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
        handled = isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        return handled;
    }


    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mNestedScrollingDirectChild = child;
        mNestedScrollingTarget = target;
        mNestedScrollInProgress = true;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            if(view.getVisibility() ==GONE){
                continue;
            }
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
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        // Dispatch up our nested parent
        stopNestedScroll();
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            if(view.getVisibility() ==GONE){
                continue;
            }
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (!lp.isNestedScrollAccepted()) {
                continue;
            }
            final Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.onStopNestedScroll(this, view, mNestedScrollingDirectChild, target);
            }
            lp.resetNestedScroll();
            lp.resetChangedAfterNestedScroll();


        }

        mNestedScrollingDirectChild = null;
        mNestedScrollingTarget = null;
        mNestedScrollInProgress = false;
    }
    public void resetNestedScrollInProgress(){
        mNestedScrollInProgress = false;
    }
    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
        final int childCount = getChildCount();
        int dyAlreadyConsumed = dyConsumed;
        int dyUnConsumedSurplus = dyUnconsumed;
        if(dyUnconsumed > 0){
            for (int i = 0; i < childCount; i++) {
                final View view = getChildAt(i);
                if(view.getVisibility() ==GONE){
                    continue;
                }
                final LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (!lp.isNestedScrollAccepted()) {
                    continue;
                }

                final Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null) {
                    mTempIntPair[0] = dyConsumed;
                    mTempIntPair[1] = 0;
                    viewBehavior.onNestedScroll(this, view, mNestedScrollingDirectChild, target, dxConsumed, dyAlreadyConsumed,
                            dxUnconsumed, dyUnConsumedSurplus, mTempIntPair);
                    dyAlreadyConsumed += mTempIntPair[1];
                    dyUnConsumedSurplus -=mTempIntPair[1];
                    if (dyUnConsumedSurplus == 0) {
//                    break;
                    }
                }
            }
        }else{
            for (int i = childCount -1; i >= 0; i--) {
                final View view = getChildAt(i);
                if(view.getVisibility() ==GONE){
                    continue;
                }
                final LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (!lp.isNestedScrollAccepted()) {
                    continue;
                }

                final Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null) {
                    mTempIntPair[0] = dyAlreadyConsumed;
                    mTempIntPair[1] = 0;
                    viewBehavior.onNestedScroll(this, view, mNestedScrollingDirectChild, target, dxConsumed, dyAlreadyConsumed,
                            dxUnconsumed, dyUnConsumedSurplus, mTempIntPair);
                    dyAlreadyConsumed += mTempIntPair[1];
                    dyUnConsumedSurplus -=mTempIntPair[1];
                    if (dyUnConsumedSurplus == 0) {
//                    break;
                    }
                }
            }
        }


        //-----
        if (dyUnConsumedSurplus != 0) {
            // Dispatch up to the nested parent first
            dispatchNestedScroll(dxConsumed, dyAlreadyConsumed, dxUnconsumed, dyUnConsumedSurplus,
                    null);
        }
//        Log.e("NestedScrollLayout"," dyUnconsumed " +dyUnconsumed +"scrollDy"+ (scrollEnd -scrollStart));
    }


    private final int[] mTempIntPair = new int[2];

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        int xConsumed = 0;
        int yConsumed = 0;
        int xUnConsumed = dx;
        int yUnConsumed = dy;
        final int childCount = getChildCount();
        if (dy > 0) {
            for (int i = 0; i < childCount; i++) {
                final View view = getChildAt(i);
                if(view.getVisibility() ==GONE){
                    continue;
                }
                final LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (!lp.isNestedScrollAccepted()) {
                    continue;
                }

                final Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null) {
                    mTempIntPair[0] = mTempIntPair[1] = 0;
                    viewBehavior.onNestedPreScroll(this, view, mNestedScrollingDirectChild, target, xUnConsumed, yUnConsumed, mTempIntPair);
                    xUnConsumed -= mTempIntPair[0];
                    yUnConsumed -= mTempIntPair[1];
                    xConsumed += mTempIntPair[0];
                    yConsumed += mTempIntPair[1];
                    if (yConsumed == dy) {
//                        break;
                    }
                }
            }
        } else {
            for (int i = childCount - 1; i >= 0; i--) {
                final View view = getChildAt(i);
                if(view.getVisibility() ==GONE){
                    continue;
                }
                final LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (!lp.isNestedScrollAccepted()) {
                    continue;
                }
                final Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null) {
                    mTempIntPair[0] = mTempIntPair[1] = 0;
                    viewBehavior.onNestedPreScroll(this, view, mNestedScrollingDirectChild, target, xUnConsumed, yUnConsumed, mTempIntPair);
                    xUnConsumed -= mTempIntPair[0];
                    yUnConsumed -= mTempIntPair[1];
                    xConsumed += mTempIntPair[0];
                    yConsumed += mTempIntPair[1];
                    if (yConsumed == dy) {
                    }
                }
            }
        }
        consumed[0] = xConsumed;
        consumed[1] = yConsumed;

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if ((dy - consumed[1]) != 0) {
            parentConsumed[0] = parentConsumed[1] = 0;
            if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
                consumed[0] += parentConsumed[0];
                consumed[1] += parentConsumed[1];
            }

        }
//        Log.e("NestedScrollLayout"," consumed[1] " +consumed[1] +"scrollDy"+ (scrollEnd -scrollStart));

    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {

        boolean handled = false;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            if(view.getVisibility() ==GONE){
                continue;
            }
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (!lp.isNestedScrollAccepted()) {
                continue;
            }

            final Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                handled |= viewBehavior.onNestedFling(this, view, mNestedScrollingDirectChild, target, velocityX, velocityY,
                        consumed);
            }
        }
        if(handled){
            dispatchNestedFling(velocityX, velocityY, handled != consumed);
            return handled;
        }else{
            return dispatchNestedFling(velocityX, velocityY, handled != consumed);
        }

    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {

        boolean handled = false;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if(view.getVisibility() ==GONE){
                continue;
            }
            if (!lp.isNestedScrollAccepted()) {
                continue;
            }

            final Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                handled |= viewBehavior.onNestedPreFling(this, view, mNestedScrollingDirectChild, target, velocityX, velocityY);
            }
        }
        if (handled) {
            return true;
        } else {
            return dispatchNestedPreFling(velocityX, velocityY);
        }
    }


    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is TOP custom view.
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



    /**
     * <p>The scroll range of TOP scroll view is the overall height of all of its
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
        return super.computeVerticalScrollRange();
    }
    public void dispatchOnDependentViewChanged() {
        mLayoutManager.dispatchOnDependentViewChanged();
    }


    public int dragedScrollBy(int dx, int dy) {
        mTempIntPair[0] = mTempIntPair[1] = 0;
        int childCount = getChildCount();
        int consumedDy = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if(child.getVisibility() ==GONE){
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null &&viewBehavior.isAcceptedDrag()) {

                viewBehavior.onScrollBy(this, child, dx, dy - consumedDy, mTempIntPair);
                consumedDy += mTempIntPair[1];
                if (consumedDy == dy) {
                    break;
                }
            }
        }
        return consumedDy;
    }

    public void onStartDrag(int mInitialTouchX,int mInitialTouchY) {
        int childCount = getChildCount();
        boolean isAccepted =false;
        Behavior accepteBehavior = null;
        for (int i = childCount -1; i >=0; i--) {
            View child = getChildAt(i);
            if(child.getVisibility() ==GONE){
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.onStartDrag(this,child,mInitialTouchX,mInitialTouchY,isAccepted,accepteBehavior);
                boolean behaviorAccepted = viewBehavior.isAcceptedDrag;
                if(behaviorAccepted){
                    accepteBehavior = viewBehavior;
                }
                isAccepted |=behaviorAccepted;

            }
        }
    }
    boolean isBlockingInteractionBelow(int mInitialTouchX, int mInitialTouchY){
        int childCount = getChildCount();
        for (int i = childCount -1; i >=0; i--) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final Behavior b = lp.getBehavior();
            if(b !=null ){
                if(b.blocksInteractionBelow(this,child,mInitialTouchX,mInitialTouchY)){
                    return true;
                }

            }
        }
        return false;
    }

    public void onStopDrag() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if(child.getVisibility() ==GONE){
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                if( viewBehavior.isAcceptedDrag()){
                    viewBehavior.onStopDrag(this);
                }
                viewBehavior.setCanAcceptedDrag(false);
            }
        }
    }

    public void dispatchDragedFling(int velocityY) {
        int childCount = getChildCount();
        boolean handled = false;
        if (!dispatchNestedPreFling(0, velocityY)) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if(child.getVisibility() ==GONE){
                    continue;
                }
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null&&viewBehavior.isAcceptedDrag()) {
                    handled = viewBehavior.onFling(this, child, 0, velocityY);
                    if (handled) {
                        break;
                    }
                }
            }
            dispatchNestedFling(0, velocityY, handled);
        }
        if (handled) {

        }
    }



    private int clamp(int value, int min, int max) {
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

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    static final String WIDGET_PACKAGE_NAME;

    static {
        final Package pkg = NestedScrollLayout.class.getPackage();
        WIDGET_PACKAGE_NAME = pkg != null ? pkg.getName() : null;
    }

    static final Class<?>[] CONSTRUCTOR_PARAMS = new Class<?>[]{
            Context.class,
            AttributeSet.class
    };

    static final ThreadLocal<Map<String, Constructor<Behavior>>> sConstructors =
            new ThreadLocal<>();

    public static class LayoutParams extends FrameLayout.LayoutParams {
        /**
         * @hide
         */
        @IntDef(flag = true, value = {
                SCROLL_FLAG_SCROLL,
                SCROLL_FLAG_EXIT_UNTIL_COLLAPSED,
                SCROLL_FLAG_ENTER_ALWAYS,
                SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED,
                SCROLL_FLAG_SNAP
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface ScrollFlags {
        }

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
//        /**
//         * 用于header 当 onNestedScroll dy<0 没有被其他view 消耗
//         */
//        public static final int SCROLL_FLAG_ENTER_DOWN = 0x32;
//        /**
//         * 用于footer 当 onNestedScroll dy>0 没有被其他view 消耗
//         */
//        public static final int SCROLL_FLAG_ENTER_UP = 0x33;

//        public static final int SCROLL_FLAG_SCROLL_INTERUPTED = 0x128;

        /**
         * Upon TOP scroll ending, if the view is only partially visible then it will be snapped
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
//        public static final int LAYOUT_FLAG_ANTHOR = 0x4;

        public int anchorGravity = Gravity.NO_GRAVITY;
        public int gravity = Gravity.NO_GRAVITY;
        int mAnchorId = View.NO_ID;

        View mAnchorView;
        View mAnchorDirectChild;
        int mLayoutFlags = LAYOUT_FLAG_FRAMLAYOUT;
        int mScrollFlags = SCROLL_FLAG_SCROLL;
        int mLayoutTop = Integer.MIN_VALUE;
        private static final int INVALID_SCROLL_RANGE = -1;
        private View mAttachedView;

        boolean isApplyInsets;
        int mTopInset;
        String mControlBehaviorName;
        private boolean mDidAcceptNestedScroll = true;
        private boolean mDidChangeAfterNestedScroll;
        private int mTotalScrollRange = INVALID_SCROLL_RANGE;
        private int mTotalUnResolvedScrollRange = INVALID_SCROLL_RANGE;
        private int mDownPreScrollRange = INVALID_SCROLL_RANGE;
        private int mDownScrollRange = INVALID_SCROLL_RANGE;
        private int mUpPreScrollRange = INVALID_SCROLL_RANGE;
        private int mUpScrollRange = INVALID_SCROLL_RANGE;

        private int mRelativeOffsetRange = INVALID_SCROLL_RANGE;
        float mRelativeOffsetRangeRate;

        boolean mBehaviorResolved;
        int mOverScrollDistance;
        protected int mBaseLine;
        @FloatRange(from = 0, to = 1)
        float mScrimOpacity;
        @ColorInt
        int mScrimColor;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            final TypedArray a = c.obtainStyledAttributes(attrs, com.ytjojo.viewlib.nestedsrolllayout.R.styleable.NestedScrollLayout_LayoutParams);
            this.gravity = a.getInteger(
                    R.styleable.NestedScrollLayout_LayoutParams_android_layout_gravity,
                    Gravity.NO_GRAVITY);
            mAnchorId = a.getResourceId(R.styleable.NestedScrollLayout_LayoutParams_anchor,
                    View.NO_ID);
            this.anchorGravity = a.getInteger(
                    R.styleable.NestedScrollLayout_LayoutParams_anchorGravity,
                    Gravity.NO_GRAVITY);
            mControlBehaviorName = a.getString(R.styleable.NestedScrollLayout_LayoutParams_controlBehaviorName);
            mScrollFlags = a.getInt(R.styleable.NestedScrollLayout_LayoutParams_scrollFlags, 0);
            mLayoutFlags = a.getInt(R.styleable.NestedScrollLayout_LayoutParams_layoutFlags, LAYOUT_FLAG_LINEARVERTICAL);
            if (mLayoutFlags == LAYOUT_FLAG_FRAMLAYOUT) {
                mScrollFlags = 0;
                mControlBehaviorName = null;
            }
            if(TextUtils.isEmpty(mControlBehaviorName)){
                mLayoutFlags = LAYOUT_FLAG_FRAMLAYOUT;
            }
            mBehaviorResolved = a.hasValue(
                    R.styleable.NestedScrollLayout_LayoutParams_behavior);
            if (mBehaviorResolved) {
                mBehavior = parseBehavior(c, attrs, a.getString(
                        R.styleable.NestedScrollLayout_LayoutParams_behavior));
            }
            if((mScrollFlags & SCROLL_FLAG_SCROLL) != 0){
                mBaseLine=a.getDimensionPixelSize(R.styleable.NestedScrollLayout_LayoutParams_baseLine,Integer.MAX_VALUE);
                mOverScrollDistance=a.getDimensionPixelSize(R.styleable.NestedScrollLayout_LayoutParams_overScrollDistance,0);

            }

            a.recycle();
        }

        static Behavior parseBehavior(Context context, AttributeSet attrs, String name) {
            if (TextUtils.isEmpty(name)) {
                return null;
            }

            final String fullName;
            if (name.startsWith(".")) {
                // Relative to the app package. Prepend the app package name.
                fullName = context.getPackageName() + name;
            } else if (name.indexOf('.') >= 0) {
                // Fully qualified package name.
                fullName = name;
            } else {
                // Assume stock behavior in this package (if we have one)
                fullName = !TextUtils.isEmpty(WIDGET_PACKAGE_NAME)
                        ? (WIDGET_PACKAGE_NAME + '.' + name)
                        : name;
            }

            try {
                Map<String, Constructor<Behavior>> constructors = sConstructors.get();
                if (constructors == null) {
                    constructors = new HashMap<>();
                    sConstructors.set(constructors);
                }
                Constructor<Behavior> c = constructors.get(fullName);
                if (c == null) {
                    final Class<Behavior> clazz = (Class<Behavior>) Class.forName(fullName, true,
                            context.getClassLoader());
                    c = clazz.getConstructor(CONSTRUCTOR_PARAMS);
                    c.setAccessible(true);
                    constructors.put(fullName, c);
                }
                return c.newInstance(context, attrs);
            } catch (Exception e) {
                throw new RuntimeException("Could not inflate Behavior subclass " + fullName, e);
            }
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

        public void setAttactchedView(View v) {
            this.mAttachedView = v;
        }

        int mMeasuredHeight;

        int getMeasuredHeight() {
            if (mMeasuredHeight > 0) {
                return mMeasuredHeight;
            }
            return mMeasuredHeight = mAttachedView.getMeasuredHeight();

        }




        public void acceptNestedScroll(boolean accept) {
            mDidAcceptNestedScroll = accept;
        }

        void resetNestedScroll() {
            mDidAcceptNestedScroll = false;
        }

        void resetChangedAfterNestedScroll() {
            mDidChangeAfterNestedScroll = false;
        }
        void setChangedAfterNestedScroll(boolean changed) {
            mDidChangeAfterNestedScroll = changed;
        }
        boolean getChangedAfterNestedScroll() {
            return mDidChangeAfterNestedScroll;
        }

        public void setLayoutTop(int top) {
            this.mLayoutTop = top;
            invalidateScrollRanges();
        }

        public int getLayoutTop() {
            if (mLayoutTop == Integer.MIN_VALUE) {
                throw new IllegalStateException("Could not find layoutTop" + mAttachedView
                        + " after onLayout() setLayoutTop(int) not be called ");
            }
            return mLayoutTop;
        }

        public boolean isNestedScrollAccepted() {
            return mDidAcceptNestedScroll;
        }

        public final int getTotalResolvedScrollRange() {
            if (mTotalScrollRange != INVALID_SCROLL_RANGE) {
                return mTotalScrollRange;
            }
            int range = 0;
            if ((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                // We're set to scroll so add the child's height
                range += getMeasuredHeight() + this.topMargin + this.bottomMargin;

                if ((mScrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                    // For TOP collapsing scroll, we to take the collapsed height into account.
                    // We also break straight away since later views can't scroll beneath
                    // us
                    range -= ViewCompat.getMinimumHeight(mAttachedView);
                }
            }
            NestedScrollLayout parent = (NestedScrollLayout) mAttachedView.getParent();
            int topInsets =isApplyInsets?mTopInset : 0;
            return mTotalScrollRange = Math.max(0,range-topInsets);
        }

        private void invalidateScrollRanges() {
            // Invalidate the scroll ranges
            mTotalScrollRange = INVALID_SCROLL_RANGE;
            mTotalUnResolvedScrollRange = INVALID_SCROLL_RANGE;
            mDownPreScrollRange = INVALID_SCROLL_RANGE;
            mDownScrollRange = INVALID_SCROLL_RANGE;
            mUpPreScrollRange = INVALID_SCROLL_RANGE;
            mUpScrollRange = INVALID_SCROLL_RANGE;
        }

        public final int getTotalUnResolvedScrollRange() {
            if (mTotalUnResolvedScrollRange != INVALID_SCROLL_RANGE) {
                return mTotalUnResolvedScrollRange;
            }
            int range = 0;
            if ((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                // We're set to scroll so add the child's height
                range += getMeasuredHeight() + this.topMargin + this.bottomMargin;
            }
            return mTotalUnResolvedScrollRange = range;
        }

        public boolean isEitUntilCollapsed() {
            if ((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                if ((mScrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean isControlViewByBehavior(String behavoirName) {
            if (behavoirName.equals(mControlBehaviorName)) {
                return true;
            }
            return false;
        }


        public int getDownNestedPreScrollRange() {
            if (mDownPreScrollRange != INVALID_SCROLL_RANGE) {
                // If we already have TOP valid value, return it
                return mDownPreScrollRange;
            }
            int range = 0;
            if ((mScrollFlags & LayoutParams.FLAG_QUICK_RETURN) == LayoutParams.FLAG_QUICK_RETURN) {
                // First take the margin into account
                range += this.topMargin + this.bottomMargin;
                // The view has the quick return flag combination...
                if ((mScrollFlags & LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED) != 0) {
                    // If they're set to enter collapsed, use the minimum height
                    range += ViewCompat.getMinimumHeight(mAttachedView);
                } else if ((mScrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                    // Only enter by the amount of the collapsed height
                    range = ViewCompat.getMinimumHeight(mAttachedView);
                } else {
                    // Else use the full height
                    range += getMeasuredHeight();
                }
            }
            return mDownPreScrollRange = range;
        }

        public int getDownNestedScrollRange() {
            if (mDownScrollRange != INVALID_SCROLL_RANGE) {
                // If we already have TOP valid value, return it
                return mDownScrollRange;
            }

            int range = 0;
            int childHeight = getMeasuredHeight();
            childHeight += this.topMargin + this.bottomMargin;
            if ((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                // We're set to scroll so add the child's height
                range += childHeight;

                if ((mScrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                    // For TOP collapsing exit scroll, we to take the collapsed height into account.
                    // We also break the range straight away since later views can't scroll
                    // beneath us
                    range -= ViewCompat.getMinimumHeight(mAttachedView);
                }
            } else {
                // As soon as TOP view doesn't have the scroll flag, we end the range calculation.
                // This is because views below can not scroll under TOP fixed view.
            }
            return mDownScrollRange = Math.max(0, range);
        }

        public int getUpNestedPreScrollRange() {
            if (mUpPreScrollRange != INVALID_SCROLL_RANGE) {
                // If we already have TOP valid value, return it
                return mUpPreScrollRange;
            }
            int range = 0;
            if ((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0) {


                if ((mScrollFlags & LayoutParams.FLAG_QUICK_RETURN) == LayoutParams.FLAG_QUICK_RETURN) {
                    if ((mScrollFlags & LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                        range = ViewCompat.getMinimumHeight(mAttachedView)+((NestedScrollLayout)mAttachedView.getParent()).getTopInset();

                    }
                }
            }
            return mUpPreScrollRange = range;
        }

        public int getUpNestedScrollRange() {
            if (mUpScrollRange != INVALID_SCROLL_RANGE) {
                // If we already have TOP valid value, return it
                return mUpScrollRange;
            }
            if ((mScrollFlags & LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                return mUpScrollRange = getTotalResolvedScrollRange();
            }
            return mUpScrollRange = 0;
        }

        private int getRelativeOffsetRange() {
            if (mRelativeOffsetRange != INVALID_SCROLL_RANGE)
                return mRelativeOffsetRange;
            return mRelativeOffsetRange = (int) (getMeasuredHeight() * mRelativeOffsetRangeRate);
        }

        private void setelativeOffsetRangeRate(float rateToHeight) {
            mRelativeOffsetRangeRate = rateToHeight;
            if (mAttachedView.getMeasuredHeight() > 0) {
                mRelativeOffsetRange = (int) (getMeasuredHeight() * rateToHeight);
            }
        }

        /**
         * Set the id of this view's anchor.
         * <p>
         * <p>The view with this id must be TOP descendant of the CoordinatorLayout containing
         * the child view this LayoutParams belongs to. It may not be the child view with
         * this LayoutParams or TOP descendant of it.</p>
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
            return dependency == mAnchorDirectChild || (mBehavior != null && mBehavior.layoutDependsOn(parent, child, dependency));
        }

        /**
         * Locate the appropriate anchor view by the current {@link #setAnchorId(int) anchor id}
         * or return the cached anchor view if already known.
         *
         * @param parent   the parent CoordinatorLayout
         * @param forChild the child this LayoutParams is associated with
         * @return the located descendant anchor view, or null if the anchor id is
         * {@link View#NO_ID}.
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
         * TOP descendant of the expected parent view, it is not the child this LayoutParams
         * is assigned to or TOP descendant of it, and it has the expected id.
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
                                "Anchor must not be TOP descendant of the anchored view");
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

        public void addOnOffsetChangedListener(OnOffsetChangedListener l) {
            if (mOnOffsetChangedListeners == null) {
                mOnOffsetChangedListeners = new LinkedList<>();
            }
            if(!mOnOffsetChangedListeners.contains(l))
            mOnOffsetChangedListeners.add(l);
        }

        public void removeOnOffsetChangedListener(OnOffsetChangedListener l) {
            if (mOnOffsetChangedListeners != null) {
                mOnOffsetChangedListeners.remove(l);
            }

        }

        public boolean hasOffsetChangedListener() {
            if (mOnOffsetChangedListeners != null && !mOnOffsetChangedListeners.isEmpty()) {
                return true;
            }
            return false;
        }

        public void onScrollChanged(float ratio, int dy, int offsetPix, int totalRange, int parentScrollDy) {

            if(mOnOffsetChangedListeners !=null){
              for (OnOffsetChangedListener l:mOnOffsetChangedListeners){
                  l.onOffsetChanged(mAttachedView,ratio,dy,offsetPix,totalRange,parentScrollDy);

              }
            }
            Log.e("onScrollChanged", "getMeasuredHeight"+ mAttachedView.getMeasuredHeight()+"ratio =" + ratio + "dy =" + dy + " offsetPix=" + offsetPix + "totalRange =" + totalRange);
        }

        public boolean isVisable() {
            NestedScrollLayout mParent = (NestedScrollLayout) mAttachedView.getParent();
            int visiableTop = (int) (mAttachedView.getY() - mParent.getScrollY());
            return visiableTop >= mParent.getPaddingTop() + this.topMargin && visiableTop < mParent.getHeight() - mParent.getPaddingBottom();
        }

        public boolean isOutOfTop() {
            NestedScrollLayout mParent = (NestedScrollLayout) mAttachedView.getParent();
            int visiableTop = (int) (mAttachedView.getTop() + mAttachedView.getY() - mParent.getScrollY());
            return visiableTop < mParent.getPaddingTop() + this.topMargin;
        }


        public void setScrimOpacity(@FloatRange(from = 0, to = 1) float scrimOpacity){
            this.mScrimOpacity = scrimOpacity;
        }
        public void setScrimColor( @ColorInt int scrimColor){
            this.mScrimColor = scrimColor;
        }
        public float getScrimOpacity(){
            return mScrimOpacity;
        }
        public int getScrimColor(){
            return mScrimColor;
        }

        public int mScrollYStart;
        public int mScrollYEnd;

        Behavior mBehavior;

        public Behavior getBehavior() {
            return mBehavior;
        }
    }

    public View findDirectTargetChild(View target) {
        ViewGroup parent = (ViewGroup) target.getParent();
        View directTargetChild = null;
        if (parent == NestedScrollLayout.this) {
            directTargetChild = target;
        } else {
            while (parent != NestedScrollLayout.this) {
                directTargetChild = parent;
                parent = (ViewGroup) parent.getParent();
                if (parent == NestedScrollLayout.this) {
                    break;
                }
            }
        }
        return directTargetChild;
    }

    private WindowInsetsCompat mLastInsets;
    private boolean mDrawStatusBarBackground;
    private Drawable mStatusBarBackground;


    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (mDrawStatusBarBackground && mStatusBarBackground != null) {
            final int inset = mLastInsets != null ? mLastInsets.getSystemWindowInsetTop() : 0;
            if (inset > 0) {
                mStatusBarBackground.setBounds(0, 0, getWidth(), inset);
                mStatusBarBackground.draw(c);
            }
        }
    }
    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final float scrimAlpha = lp.mBehavior!=null? lp.mBehavior.getScrimOpacity(this, child):lp.getScrimOpacity();
        if (scrimAlpha > 0f) {
            if (mScrimPaint == null) {
                mScrimPaint = new Paint();
            }
            mScrimPaint.setColor(lp.mBehavior!=null?lp.mBehavior.getScrimColor(this, child):lp.getScrimColor());
            mScrimPaint.setAlpha(CollapsingTextHelper.constrain(Math.round(255 * scrimAlpha), 0, 200));

            final int saved = canvas.save();
            if (child.isOpaque()&&lp.mBehavior != null) {
                // If the child is opaque, there is no need to draw behind it so we'll inverse
                // clip the canvas
                canvas.clipRect(child.getLeft(), child.getTop(), child.getRight(),
                        child.getBottom(), Region.Op.DIFFERENCE);
            }
            // Now draw the rectangle for the scrim
            canvas.drawRect(getPaddingLeft(), getPaddingTop(),
                    getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(),
                    mScrimPaint);
            canvas.restoreToCount(saved);
        }
        return super.drawChild(canvas, child, drawingTime);
    }
    /**
     * Set a drawable to draw in the insets area for the status bar.
     * Note that this will only be activated if this DrawerLayout fitsSystemWindows.
     *
     * @param bg Background drawable to draw behind the status bar
     */
    public void setStatusBarBackground(@Nullable final Drawable bg) {
        if (mStatusBarBackground != bg) {
            if (mStatusBarBackground != null) {
                mStatusBarBackground.setCallback(null);
            }
            mStatusBarBackground = bg != null ? bg.mutate() : null;
            if (mStatusBarBackground != null) {
                if (mStatusBarBackground.isStateful()) {
                    mStatusBarBackground.setState(getDrawableState());
                }
                DrawableCompat.setLayoutDirection(mStatusBarBackground,
                        ViewCompat.getLayoutDirection(this));
                mStatusBarBackground.setVisible(getVisibility() == VISIBLE, false);
                mStatusBarBackground.setCallback(this);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Gets the drawable used to draw in the insets area for the status bar.
     *
     * @return The status bar background drawable, or null if none set
     */
    @Nullable
    public Drawable getStatusBarBackground() {
        return mStatusBarBackground;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final int[] state = getDrawableState();
        boolean changed = false;

        Drawable d = mStatusBarBackground;
        if (d != null && d.isStateful()) {
            changed |= d.setState(state);
        }

        if (changed) {
            invalidate();
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mStatusBarBackground;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        final boolean visible = visibility == VISIBLE;
        if (mStatusBarBackground != null && mStatusBarBackground.isVisible() != visible) {
            mStatusBarBackground.setVisible(visible, false);
        }
    }

    /**
     * Set a drawable to draw in the insets area for the status bar.
     * Note that this will only be activated if this DrawerLayout fitsSystemWindows.
     *
     * @param resId Resource id of a background drawable to draw behind the status bar
     */
    public void setStatusBarBackgroundResource(@DrawableRes int resId) {
        setStatusBarBackground(resId != 0 ? ContextCompat.getDrawable(getContext(), resId) : null);
    }

    /**
     * Set a drawable to draw in the insets area for the status bar.
     * Note that this will only be activated if this DrawerLayout fitsSystemWindows.
     *
     * @param color Color to use as a background drawable to draw behind the status bar
     *              in 0xAARRGGBB format.
     */
    public void setStatusBarBackgroundColor(@ColorInt int color) {
        setStatusBarBackground(new ColorDrawable(color));
    }
    private WindowInsetsCompat setWindowInsets(WindowInsetsCompat insets) {
        if (mLastInsets != insets) {
            mLastInsets = insets;
            mDrawStatusBarBackground = insets != null && insets.getSystemWindowInsetTop() > 0;
            setWillNotDraw(!mDrawStatusBarBackground && getBackground() == null);

            // Now dispatch to the Behaviors
            requestLayout();
        }
        return insets;
    }
    private class ApplyInsetsListener
            implements android.support.v4.view.OnApplyWindowInsetsListener {
        @Override
        public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
            return setWindowInsets(insets);
        }
    }

    private void setupForWindowInsets(){
        if (Build.VERSION.SDK_INT >= 21) {
            InsetsHelperLollipop.setupForWindowInsets(this,new ApplyInsetsListener());
        } else {
        }
    }
    public WindowInsetsCompat getLastInsets(){
        return mLastInsets;
    }
    public int getTopInset() {
        return mLastInsets != null ? mLastInsets.getSystemWindowInsetTop() : 0;
    }
    public boolean doViewsOverlap(View first, View second) {
        return mLayoutManager.doViewsOverlap(first,second);
    }
    public boolean isPointInChildBounds(View child, int x, int y) {
        return mLayoutManager.isPointInChildBounds(child,x,y);
    }
}
