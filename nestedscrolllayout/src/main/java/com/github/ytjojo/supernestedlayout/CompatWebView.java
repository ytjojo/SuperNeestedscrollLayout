package com.github.ytjojo.supernestedlayout;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CompatWebView extends WebView implements NestedScrollingChild {

    private static final int INVALID_POINTER = -1;
    private static String TAG = CompatWebView.class.getSimpleName();
    private int consumedY;
    private int contentHeight = -1;
    private float density;
    public int direction = 0;
    private DirectionDetector directionDetector;
    private NestedScrollingChildHelper mChildHelper;
    private View.OnLongClickListener longClickListenerFalse;
    private View.OnLongClickListener longClickListenerTrue;
    private boolean mIsBeingDragged = false;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    private int mNestedYOffset;
    private int mLastMotionY;
    private final int[] mScrollConsumed = new int[2];
    private final int[] mScrollOffset = new int[2];
    private int mActivePointerId = -1;
    private VelocityTracker mVelocityTracker;
    private OnScrollChangeListener onScrollChangeListener;
    private int originHeight;
    private ViewGroup parentView;
    public ScrollState position = ScrollState.TOP;
    int preContentHeight = 0;
    private float preY;
    private OnScrollChangeListener scrollStateChangedListener;
    private WebSettings settings;
    private int mTouchSlop;
    private int webviewHeight = -1;

    public CompatWebView(Context paramContext) {
        this(paramContext, null);
    }

    public CompatWebView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatWebView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init();
    }

    private void endTouch() {
        setJavaScriptEnable(true);
        this.mIsBeingDragged = false;
        this.mActivePointerId = -1;
        recycleVelocityTracker();
        stopNestedScroll();
        if(!ViewCompat.isNestedScrollingEnabled(this)){
            ViewCompat.setNestedScrollingEnabled(this,true);
        }
    }

    private void flingWithNestedDispatch(int velocityY) {
        final boolean canFling = (velocityY < 0 && ViewCompat.canScrollVertically(this, -1)) || (velocityY > 0 && ViewCompat.canScrollVertically(this, 1));
        if (!dispatchNestedPreFling(0.0F, velocityY)) {
            Log.i(TAG, "dispatchNestedPreFling : velocityY : " + velocityY);
            dispatchNestedFling(0, velocityY, canFling);
//            flingScroll(0,velocityY);
        }
    }

    private void getEmbeddedParent(View paramView) {
        ViewParent localViewParent = paramView.getParent();
        if (localViewParent != null) {
            if ((localViewParent instanceof OnScrollChangeListener)) {
                this.parentView = ((ViewGroup) localViewParent);
                setScrollStateChangedListener((OnScrollChangeListener) localViewParent);
            } else {
                if ((localViewParent instanceof ViewGroup)) {
                    getEmbeddedParent((ViewGroup) localViewParent);
                }
            }
        }
    }

    private void init() {
        this.mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.directionDetector = new DirectionDetector();
        this.density = getScale();
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        this.settings = getSettings();
        Log.i(TAG, "max -- min Velocity = " + this.mMaximumVelocity + " -- " + this.mMinimumVelocity + " touchSlop = " + this.mTouchSlop);
    }


    private void setJavaScriptEnable(boolean flag) {
        if (this.settings.getJavaScriptEnabled() != flag) {
            Log.i(TAG, "setJavaScriptEnable : " + this.settings.getJavaScriptEnabled() + " / " + flag);
            this.settings.setJavaScriptEnabled(flag);
        }
    }


    private void setScrollStateChangedListener(OnScrollChangeListener paramc) {
        this.scrollStateChangedListener = paramc;
    }

    @Override
    public void computeScroll() {
        if (this.position == ScrollState.MEDIUM) {
            super.computeScroll();
        }
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return this.mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return this.mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return this.mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return this.mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    public int getWebContentHeight() {
        return this.contentHeight;
    }

    public boolean hasNestedScrollingParent() {
        return this.mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.contentHeight = ((int) (getContentHeight() * getScale()));
        if (this.contentHeight != this.preContentHeight) {
            loadUrl("javascript:window.InjectedObject.getContentHeight(document.getElementsByTagName('div')[0].scrollHeight)");
            this.preContentHeight = this.contentHeight;
        }
    }

    public boolean isBeingDragged() {
        return this.mIsBeingDragged;
    }

    public boolean isNestedScrollingEnabled() {
        return this.mChildHelper.isNestedScrollingEnabled();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getEmbeddedParent(this);
    }


    private void setLongClickEnable(boolean longClickable) {
        if (longClickable) {
            Log.i(TAG, "111111 setLongClickEnable : " + isLongClickable());
            if (!isLongClickable()) {
                super.setOnLongClickListener(this.longClickListenerFalse);
                setLongClickable(true);
                setHapticFeedbackEnabled(true);
            }
            return;
        }
        Log.i(TAG, "22222 setLongClickEnable : " + isLongClickable());
        if (this.longClickListenerTrue == null) {
            this.longClickListenerTrue = new View.OnLongClickListener() {
                public boolean onLongClick(View view) {
                    return true;
                }
            };
        }
        super.setOnLongClickListener(this.longClickListenerTrue);
        setLongClickable(false);
        setHapticFeedbackEnabled(false);
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        this.consumedY = (y - oldy);
        Log.i(TAG, "consumedYconsumedYconsumedY====" + consumedY);
        if (y <= 0) {
            this.position = ScrollState.TOP;

        }else if (y + this.webviewHeight >= this.contentHeight) {
            if (this.contentHeight > 0) {
                this.position = ScrollState.BOTTOM;
            }
        } else {
            this.position = ScrollState.MEDIUM;
        }
        if (null != this.scrollStateChangedListener) {
            this.scrollStateChangedListener.onChildPositionChange(this.position);
        }


//            Log.i(TAG,"yy=="+y+"  webviewHeight=="+this.webviewHeight+"  contentHeight=="+this.contentHeight);

        if (this.onScrollChangeListener != null) {
            this.onScrollChangeListener.onScrollChanged(x, y, oldx, oldy, this.position);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.webviewHeight = h + 1;
        if (this.contentHeight < 1) {
            setContentHeight(this.webviewHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int actionMasked = MotionEventCompat.getActionMasked(ev);
        initVelocityTrackerIfNotExists();
        MotionEvent vtev = MotionEvent.obtain(ev);
        final int index = MotionEventCompat.getActionIndex(ev);
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);
        this.consumedY = 0;
        this.direction = 0;
        boolean onTouchEvent = false;
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                // Remember where the motion event started
                onTouchEvent = super.onTouchEvent(ev);
                mLastMotionY = (int) (ev.getY() + 0.5f);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                this.preY = vtev.getY();
                this.mIsBeingDragged = false;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                onTouchEvent = super.onTouchEvent(ev);
                mLastMotionY = (int) (MotionEventCompat.getY(ev, index) + 0.5f);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev,
                        mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }
                if (!mIsBeingDragged && Math.abs(vtev.getY() - this.preY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                }
//                if(!mIsBeingDragged){
//                    setLongClickEnable(true);
//                }
                final int y = (int) (MotionEventCompat.getY(ev, activePointerIndex) + 0.5f);
                Log.i(TAG, "mLastMotionY=====" + mLastMotionY);
                Log.i(TAG, "YYYYYYY=====" + y);
                int deltaY = mLastMotionY - y;

                if (deltaY != 0) {
                    this.direction = this.directionDetector.getDirection(deltaY, this.scrollStateChangedListener);
                }
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                    vtev.offsetLocation(0, mScrollOffset[1]);
                    mNestedYOffset += mScrollOffset[1];
                }
                if (mIsBeingDragged) {
//                    setJavaScriptEnable(true);
                    // Scroll to follow the motion event
                    mLastMotionY = y - mScrollOffset[1];
                    Log.i(TAG, "deltaY===" + deltaY);
                    Log.i(TAG, "this.consumedY===" + this.consumedY);
                    final int unconsumedY = deltaY - this.consumedY;

                    Log.i(TAG, " child consumed = " + this.mScrollConsumed[1] + " un_consumed = " + unconsumedY + " position = " + this.position + " direction = " + this.direction);
                    onTouchEvent = super.onTouchEvent(ev);
                    if (this.position == ScrollState.MEDIUM) {
                        return true;
                    }
                    switch (this.direction) {
                        case 1: {
                            if ((this.position != ScrollState.BOTTOM) && (this.contentHeight != this.webviewHeight)) {
                                scrollBy(0, unconsumedY);
                                break;
                            }
                            Log.i(TAG, "1111111consumedY===" + consumedY + "  unconsumedY==" + unconsumedY);
                            if (dispatchNestedScroll(0, this.consumedY, 0, unconsumedY, this.mScrollOffset)) {
                                vtev.offsetLocation(0.0F, this.mScrollOffset[1]);
                                this.mNestedYOffset += this.mScrollOffset[1];
                                this.mLastMotionY -= this.mScrollOffset[1];
                            }
                        }
                        break;
                        case 2:
                            if ((this.position == ScrollState.TOP) || (this.contentHeight == this.webviewHeight)) {
                                Log.i(TAG, "2222222consumedY===" + consumedY + "  unconsumedY==" + unconsumedY);
                                if (dispatchNestedScroll(0, this.consumedY, 0, unconsumedY, this.mScrollOffset)) {
                                    vtev.offsetLocation(0.0F, this.mScrollOffset[1]);
                                    this.mNestedYOffset += this.mScrollOffset[1];
                                    this.mLastMotionY -= this.mScrollOffset[1];
                                }
                            } else {
                                scrollBy(0, unconsumedY);
                            }
                            break;
                        default:
                            break;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                onTouchEvent = super.onTouchEvent(ev);
                break;
            case MotionEvent.ACTION_UP:
                onTouchEvent = super.onTouchEvent(ev);
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getYVelocity(velocityTracker, mActivePointerId);
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        flingWithNestedDispatch(-initialVelocity);
                    }
                }
                mActivePointerId = INVALID_POINTER;
                endTouch();
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onTouchEvent = super.onTouchEvent(ev);
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) (MotionEventCompat.getY(ev,
                        MotionEventCompat.findPointerIndex(ev, mActivePointerId)) + 0.5F);
                break;
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return onTouchEvent;
    }


    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >>
                MotionEventCompat.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose getDirection new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return super.canScrollVertically(direction);
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY,
                                   int scrollX, int scrollY,
                                   int scrollRangeX, int scrollRangeY,
                                   int maxOverScrollX, int maxOverScrollY,
                                   boolean isTouchEvent) {
        if (this.position != ScrollState.MEDIUM) {
            deltaY = 0;
        }
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
    }


    public void scrollToBottom() {
        scrollTo(getScrollX(), this.contentHeight - this.webviewHeight);
    }


    public void scrollToTop() {
        scrollTo(getScrollX(), 0);
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        Log.i(TAG, "contentHeight = " + contentHeight + " -  webviewHeight = " + this.webviewHeight + " = " + (contentHeight - this.webviewHeight));
    }

    @Override
    public void setNestedScrollingEnabled(boolean paramBoolean) {
        this.mChildHelper.setNestedScrollingEnabled(paramBoolean);
    }

    public void setOnLongClickListener(OnLongClickListener mOnLongClickListener) {
        this.longClickListenerFalse = mOnLongClickListener;
        super.setOnLongClickListener(mOnLongClickListener);
    }

    public void setOnScrollChangeListener(OnScrollChangeListener paramOnScrollChangeListener) {
        this.onScrollChangeListener = paramOnScrollChangeListener;
    }

    @Override
    public void setWebViewClient(WebViewClient paramWebViewClient) {
        if (!(paramWebViewClient instanceof WebViewClient))
            throw new IllegalArgumentException("WebViewClient should be instance of EmbeddedWebView$WebViewClient");
        super.setWebViewClient(paramWebViewClient);
    }

    @Override
    public boolean startNestedScroll(int paramInt) {
        return this.mChildHelper.startNestedScroll(paramInt);
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mChildHelper.onDetachedFromWindow();
        super.onDetachedFromWindow();
    }

    @Override
    public void stopNestedScroll() {
        this.mChildHelper.stopNestedScroll();
    }


    public static interface OnScrollChangeListener {
        public void onChildDirectionChange(int position);

        public void onChildPositionChange(ScrollState state);

        void onScrollChanged(int x, int y, int oldx, int oldy, ScrollState state);
    }

    public static enum ScrollState {TOP, BOTTOM, MEDIUM}

    public static class MyWebViewClient extends WebViewClient {
        public void onPageFinished(WebView paramWebView, String paramString) {
            super.onPageFinished(paramWebView, paramString);
        }

        public boolean shouldHandleUrlLoading(WebView paramWebView, String paramString) {
            return false;
        }

        @Deprecated
        public final boolean shouldOverrideUrlLoading(WebView paramWebView, String paramString) {
            if ((paramWebView instanceof CompatWebView)) {
                Log.i(TAG, "shouldOverrideUrlLoading : " + paramString);
                if ((TextUtils.isEmpty(paramString)) || (URLUtil.isAboutUrl(paramString))) {
                    Log.i(TAG, "Invalid url = " + paramString);
                    return true;
                }
                if (!((CompatWebView) paramWebView).isBeingDragged())
                    return shouldHandleUrlLoading(paramWebView, paramString);
            }
            return super.shouldOverrideUrlLoading(paramWebView, paramString);
        }
    }
}