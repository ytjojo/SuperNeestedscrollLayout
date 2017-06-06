package com.ytjojo.viewlib.supernestedlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.FloatRange;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

import static android.view.animation.AnimationUtils.currentAnimationTimeMillis;

/**
 * Created by Administrator on 2017/3/13 0013.
 */

public class RefreshFooterBehavior <V extends View> extends Behavior<V> implements PtrUIHandler  {
    SuperNestedLayout mSuperNestedLayout;
    public static final byte PTR_STATUS_INIT = 1;
    private byte mStatus = PTR_STATUS_INIT;
    public static final byte PTR_STATUS_PREPARE = 2;
    //    public static final byte PTR_STATUS_SETTING = 3;
    public static final byte PTR_STATUS_LOADING = 4;
    public static final byte PTR_STATUS_COMPLETE = 5;
    ArrayList<View> mToTranslationYViews = new ArrayList<>();
    V mRefreshHeaderView;
    PtrIndicator mRefreshIndicator;
    private boolean canLoad = true;

    public RefreshFooterBehavior() {
        super();
    }

    public RefreshFooterBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRefreshIndicator = new PtrIndicator();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RefreshBehavior);
        isTriggerSensitive = a.getBoolean(R.styleable.RefreshBehavior_isTriggerSensitive,false);
        isKeepShowWhenLoading = a.getBoolean(R.styleable.RefreshBehavior_isKeepShowWhenLoading,false);
        mFrictionFactor = a.getFloat(R.styleable.RefreshBehavior_frictionFactor,1f);
        mRefreshIndicator.setMaxDistanceRatio(a.getFloat(R.styleable.RefreshBehavior_maxDistanceRatio,1.5f));
        mRefreshIndicator.setRatioOfHeaderHeightToRefresh(a.getFloat(R.styleable.RefreshBehavior_ratioOfHeaderHeightToRefresh,1.1f));
        mRefreshIndicator.setMaxContentOffsetY(a.getDimensionPixelOffset(R.styleable.RefreshBehavior_ratioOfHeaderHeightToRefresh,-1));
        a.recycle();
    }
    @FloatRange(from=0f,to =1f)
    private float  mFrictionFactor = 1f;
    private int mMaxHeaderNestedScrollY;
    private boolean isTriggerSensitive = false;
    ViewOffsetHelper mOffsetHelper;

    @Override
    public boolean onStartNestedScroll(SuperNestedLayout superNestedLayout, V header, View directTargetChild, View target, int nestedScrollAxes) {
        mRefreshHeaderView = header;
        if(ViewCompat.getFitsSystemWindows(header)){
            ViewCompat.setFitsSystemWindows(header,false);

        }
        if (!canLoad) {
            return false;
        }
        final int childCount = superNestedLayout.getChildCount();
        mToTranslationYViews.clear();
        SuperNestedLayout.LayoutParams headerLp = (SuperNestedLayout.LayoutParams) header.getLayoutParams();
        View mAnchorDirectChild = headerLp.mAnchorDirectChild;
        ArrayList<View> hasScrollViewBehaviorViews = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            View itemView = superNestedLayout.getChildAt(i);
            if(itemView.getVisibility() ==View.GONE){
                continue;
            }
            SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) itemView.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if(lp.isControlViewByBehavior(ScrollViewBehavior.sBehaviorName)){
                mToTranslationYViews.add(itemView);
            }
            if (viewBehavior != null&& viewBehavior instanceof ScrollViewBehavior) {
                if(hasScrollViewBehaviorViews.size() ==0){
                    mToTranslationYViews.clear();
                    mToTranslationYViews.add(itemView);
                }
                hasScrollViewBehaviorViews.add(itemView);
            }
            if (mAnchorDirectChild != null) {
                if (mAnchorDirectChild == itemView) {
                    mToTranslationYViews.remove(itemView);
                    break;
                }
            } else {
                if(header ==itemView){
                    break;
                }
            }

        }
        if (hasScrollViewBehaviorViews.size() == 1) {
            return hasScrollViewBehaviorViews.get(0) ==directTargetChild;
        }else{
            return hasScrollViewBehaviorViews.get(hasScrollViewBehaviorViews.size()-1) ==directTargetChild;
        }
    }

    @Override
    public void onNestedScrollAccepted(SuperNestedLayout superNestedLayout, V header, View directTargetChild, View target, int nestedScrollAxes) {
        super.onNestedScrollAccepted(superNestedLayout, header, directTargetChild, target, nestedScrollAxes);
        mSuperNestedLayout = superNestedLayout;
        if(mOffsetHelper== null){
            mOffsetHelper = ViewOffsetHelper.getViewOffsetHelper(header);
        }
        addUIHandler((PtrUIHandler) header);
        isIgnore = false;
        if (!canLoad) {
            isIgnore = true;
            return;
        }
        SuperNestedLayout.LayoutParams headerLp = (SuperNestedLayout.LayoutParams) header.getLayoutParams();
        if (mRefreshIndicator == null) {
            mRefreshIndicator = new PtrIndicator();
        }
        if(mRefreshIndicator.getStableRefreshOffset()<=0){
            int height = header.getMeasuredHeight() + headerLp.topMargin + headerLp.bottomMargin;
            mRefreshIndicator.setStableRefreshOffset(height);
        }
        if(isTriggerSensitive){
            if(mRefreshIndicator.getRatioOfHeaderToHeightToRefresh()>0.3f){
                mRefreshIndicator.setRatioOfHeaderHeightToRefresh(0.3f);
            }
        }
        mMaxHeaderNestedScrollY = (int)(mRefreshIndicator.getMaxContentOffsetY()/mFrictionFactor);
        if(mCheckTriggerRunnable !=null &&mCheckTriggerRunnable.isRunning()){
            mCheckTriggerRunnable.cancel();
        }

    }
    public void setTriggerSensitive(boolean triggerSensitive){
        this.isTriggerSensitive = triggerSensitive;
        if(isTriggerSensitive){
            if(mRefreshIndicator.getRatioOfHeaderToHeightToRefresh()>0.3f){
                mRefreshIndicator.setRatioOfHeaderHeightToRefresh(0.3f);
            }
        }
    }

    boolean isKeepShowWhenLoading = false;
    public void setKeepShowWhenLoading(boolean keepShowWhenLoading){
        isKeepShowWhenLoading = keepShowWhenLoading;
    }

    @Override
    public void onNestedPreScroll(SuperNestedLayout superNestedLayout, V header, View directTargetChild, View target, int dx, int dy, int[] consumed) {
        if ( isIgnore) {
            return;
        }
        if(isRunning()){
            return;
        }
        if (dy < 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_LOADING)) {
            final int childCount = mToTranslationYViews.size();
            if(isKeepShowWhenLoading && mStatus == PTR_STATUS_LOADING){
                float y =  ViewCompat.getTranslationY(mToTranslationYViews.get(0));
                if(y >=0){
                    return;
                }
                float contentNestedScrollDy =  (y/mFrictionFactor);
                float tempStart= contentNestedScrollDy;
                contentNestedScrollDy -=dy;
                if(contentNestedScrollDy >0){
                    contentNestedScrollDy = 0;
                }
                consumed[1] = (int) (tempStart- contentNestedScrollDy);
                float contentDy = (contentNestedScrollDy *mFrictionFactor);
                for (int i = 0; i < childCount; i++) {
                    View itemView = mToTranslationYViews.get(i);
                    ViewCompat.setTranslationY(itemView, contentDy);
                }
                mSuperNestedLayout.dispatchOnDependentViewChanged();
                float headerY = ViewCompat.getTranslationY(header);
                if(!isTriggerSensitive &&contentDy > -mRefreshIndicator.getStableRefreshOffset()){
                    contentDy = -mRefreshIndicator.getStableRefreshOffset();
                }
                if(headerY != contentDy){
                    ViewCompat.setTranslationY(header,contentDy);
                    this.onUIPositionChange(superNestedLayout, true, mStatus, mRefreshIndicator);
                }
            }else{
                float headerY = (int) ViewCompat.getTranslationY(header);
                if(headerY >=0){
                    return;
                }
                float headerNestedScrollDy =  (headerY /mFrictionFactor);
                float start = headerNestedScrollDy;
                headerNestedScrollDy -=dy;
                if(headerNestedScrollDy >=0){
                    headerNestedScrollDy = 0;
                }
                float finalY = (headerNestedScrollDy * mFrictionFactor);
                consumed[1] = (int) (start - headerNestedScrollDy);
                ViewCompat.setTranslationY(header, finalY);
                mRefreshIndicator.onMove(0, finalY);
                this.onUIPositionChange(superNestedLayout, true, mStatus, mRefreshIndicator);
                for (int i = 0; i < childCount; i++) {
                    View itemView = mToTranslationYViews.get(i);
                    if(headerY <finalY){
                        ViewCompat.setTranslationY(itemView, finalY);
                    }
                }
                mSuperNestedLayout.dispatchOnDependentViewChanged();
            }
        }
    }

    @Override
    public void onNestedScroll(SuperNestedLayout superNestedLayout, V header, View directTargetChild, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed) {
        if (isIgnore) {
            return;
        }
        if(isRunning()){
            return;
        }
        if (dyUnconsumed > 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_INIT || mStatus == PTR_STATUS_LOADING)) {
            mSuperNestedLayout.dispatchNestedScroll(dxConsumed,dyConsumed,dxUnconsumed,dyUnconsumed,consumed);
            dyUnconsumed-=consumed[1];

            if(dyUnconsumed ==0){
                return;
            }

            if (mStatus == PTR_STATUS_INIT) {
                mStatus = PTR_STATUS_PREPARE;
                this.onUIRefreshPrepare(superNestedLayout);
            }
            final int childCount = mToTranslationYViews.size();
            float consumedDy = 0;
            float y =  ViewCompat.getTranslationY(mToTranslationYViews.get(0));
            if(y<= -mRefreshIndicator.getMaxContentOffsetY()){
                return;
            }
            float contentNestedScrollDy = y/mFrictionFactor;
            float tempStart= contentNestedScrollDy;
            contentNestedScrollDy -=dyUnconsumed;
            if(contentNestedScrollDy <= - mMaxHeaderNestedScrollY){
                contentNestedScrollDy = -mMaxHeaderNestedScrollY;
            }
            consumedDy =tempStart - contentNestedScrollDy;
            consumed[1] +=  consumedDy;
            float finalY = (int) (contentNestedScrollDy *mFrictionFactor);
            for (int i = 0; i < childCount; i++) {
                View itemView = mToTranslationYViews.get(i);
                ViewCompat.setTranslationY(itemView, finalY);
            }
            if (ViewCompat.getTranslationY(header) > finalY) {
                ViewCompat.setTranslationY(header, finalY);
                mRefreshIndicator.onMove(0, finalY);
                this.onUIPositionChange(superNestedLayout, true, mStatus, mRefreshIndicator);
            }
            mSuperNestedLayout.dispatchOnDependentViewChanged();
            if(isTriggerSensitive){
                if (mStatus == PTR_STATUS_PREPARE&& ViewCompat.getTranslationY(header) < -mRefreshIndicator.getOffsetToRefresh()) {
                    mStatus = PTR_STATUS_LOADING;
                    RefreshFooterBehavior.this.onUIRefreshBegin(mSuperNestedLayout);
                }
            }

        }
    }
    public boolean isRunning() {
//        return isRuning;
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

    boolean isIgnore;

    public void setRefreshComplete() {
        if (mStatus == PTR_STATUS_LOADING) {
            mStatus = PTR_STATUS_COMPLETE;
            startValueAnimitor(mRefreshHeaderView, 0, PTR_STATUS_INIT);
            isIgnore = true;
            RefreshFooterBehavior.this.onUIRefreshComplete(mSuperNestedLayout);
        }
    }

    @Override
    public void onStopNestedScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target) {
        if (isIgnore) {
            isIgnore = false;
        }
        mRefreshIndicator.onRelease();
        if(isRunning()){
            return;
        }
        final int translationY = (int) ViewCompat.getTranslationY(child);
        if(ViewCompat.getTranslationY(child) ==0){
            if(mStatus == PTR_STATUS_INIT||mStatus == PTR_STATUS_COMPLETE){

            }else{
                if(!isTriggerSensitive){
                    mStatus = PTR_STATUS_INIT;
                    RefreshFooterBehavior.this.onUIReset(mSuperNestedLayout);
                }
            }
            return;
        }
        if (translationY <= -mRefreshIndicator.getOffsetToRefresh() && mStatus == PTR_STATUS_PREPARE) {
            if(isTriggerSensitive){
                mStatus = PTR_STATUS_LOADING;
                RefreshFooterBehavior.this.onUIRefreshBegin(mSuperNestedLayout);
            }else{
                startValueAnimitor(child, -mRefreshIndicator.getStableRefreshOffset(), PTR_STATUS_LOADING);
            }
        } else if (mStatus == PTR_STATUS_LOADING) {
            if(translationY < -mRefreshIndicator.getStableRefreshOffset()){
                startValueAnimitor(child, -mRefreshIndicator.getStableRefreshOffset(), PTR_STATUS_LOADING);
            }
        } else {
            if(!isTriggerSensitive){
                startValueAnimitor(child, 0, PTR_STATUS_INIT);
            }
        }


    }


    ValueAnimator mValueAnimator;

    private void startValueAnimitor(final V header, final int finalY, final byte endStatus) {
        if(isRunning()){
            mValueAnimator.cancel();
        }
        int y = (int) ViewCompat.getTranslationY(header);

        if(y==finalY){
            if (endStatus == PTR_STATUS_LOADING && mStatus != endStatus) {
                RefreshFooterBehavior.this.onUIRefreshBegin(mSuperNestedLayout);
            } else if ((endStatus == PTR_STATUS_INIT) && mRefreshIndicator.isInStartPosition()) {
                RefreshFooterBehavior.this.onUIReset(mSuperNestedLayout);
            }
            mStatus = endStatus;
            return;
        }
        int duration = 300;
        if (Math.abs(finalY - y) < header.getMeasuredHeight()) {
            duration = 200;
        }
        mValueAnimator = ValueAnimator.ofInt(y, finalY);
        mValueAnimator.setRepeatCount(0);
        mValueAnimator.setRepeatMode(ValueAnimator.RESTART);
        mValueAnimator.setDuration(duration);
        //fuck you! the default interpolator is AccelerateDecelerateInterpolator
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                if (mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_LOADING) {
                    int count = mToTranslationYViews.size();
                    mRefreshIndicator.onMove(0, value);
                    if(isTriggerSensitive ||value > ViewCompat.getTranslationY(mToTranslationYViews.get(0))){
                        for (int i = 0; i < count; i++) {
                            View view = mToTranslationYViews.get(i);
                            ViewCompat.setTranslationY(view, value);
                        }
                        mSuperNestedLayout.dispatchOnDependentViewChanged();
                    }

                    ViewCompat.setTranslationY(header, value);
                    mRefreshIndicator.onMove(0, value);

                } else if (mStatus == PTR_STATUS_PREPARE) {
                    int count = mToTranslationYViews.size();
                    for (int i = 0; i < count; i++) {
                        View view = mToTranslationYViews.get(i);
                        ViewCompat.setTranslationY(view, value);
                    }
                    ViewCompat.setTranslationY(header, value);
                    mRefreshIndicator.onMove(0, value);
                    mSuperNestedLayout.dispatchOnDependentViewChanged();
                }
                RefreshFooterBehavior.this.onUIPositionChange(mSuperNestedLayout, false, mStatus, mRefreshIndicator);
            }
        });
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (endStatus == PTR_STATUS_LOADING && mStatus != endStatus) {
                    RefreshFooterBehavior.this.onUIRefreshBegin(mSuperNestedLayout);
                } else if ((endStatus == PTR_STATUS_INIT) && mRefreshIndicator.isInStartPosition()) {
                    RefreshFooterBehavior.this.onUIReset(mSuperNestedLayout);
                }
                mStatus = endStatus;
//                animation.end();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mValueAnimator.start();
    }


    @Override
    public boolean onNestedPreFling(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, float velocityX, float velocityY) {
        return super.onNestedPreFling(superNestedLayout, child, directTargetChild, target, velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, float velocityX, float velocityY, boolean consumed) {
        if(!isTriggerSensitive){
            return false;
        }
        if (isIgnore) {
            return false;
        }
        if(isRunning()){
            return false;
        }
        if(velocityY>0&&(mStatus ==PTR_STATUS_PREPARE||mStatus == PTR_STATUS_INIT)){
            if(!ViewCompat.canScrollVertically(target,1)){
                if(mStatus ==PTR_STATUS_INIT){
                    mStatus = PTR_STATUS_PREPARE;
                    RefreshFooterBehavior.this.onUIRefreshPrepare(mSuperNestedLayout);
                }
                startValueAnimitor(child, -mRefreshIndicator.getStableRefreshOffset(), PTR_STATUS_LOADING);
                return true;
            }else{
                if(velocityY>=800){
                    if(mCheckTriggerRunnable ==null){
                        mCheckTriggerRunnable =new CheckTriggerRunnable(child);
                    }
                    if(mCheckTriggerRunnable.isRunning()){
                        mCheckTriggerRunnable.cancel();
                    }
                    mCheckTriggerRunnable.start(target,velocityY);

                }
            }

        }


        return super.onNestedFling(superNestedLayout, child, directTargetChild, target, velocityX, velocityY, consumed);
    }



    @Override
    public void onStopDrag(SuperNestedLayout superNestedLayout, V child) {
        super.onStopDrag(superNestedLayout,child);
    }

    @Override
    public void onScrollBy(SuperNestedLayout superNestedLayout, V child, int dx, int dy, int[] consumed) {
        super.onScrollBy(superNestedLayout, child, dx, dy, consumed);
    }

    @Override
    public boolean onLayoutChild(SuperNestedLayout superNestedLayout, V child, int layoutDirection) {
        SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) child.getLayoutParams();

        if(lp.mAnchorView != null){
            View mAnchorView = lp.mAnchorView;
            int left = superNestedLayout.getPaddingLeft() + lp.leftMargin;
            SuperNestedLayout.LayoutParams anchorViewLp = (SuperNestedLayout.LayoutParams) mAnchorView.getLayoutParams();
            int top = mAnchorView.getTop()-anchorViewLp.topMargin+lp.topMargin;
            child.layout(left, top - child.getMeasuredHeight(),left + child.getMeasuredWidth(), top);

        }else{
            int left = superNestedLayout.getPaddingLeft() + lp.leftMargin;
            int top = lp.topMargin + superNestedLayout.getMeasuredHeight()- superNestedLayout.getPaddingBottom();
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }

        return true;
    }

    public void setCanLoad(boolean canLoad) {
        this.canLoad = canLoad;
    }

    LinkedList<PtrUIHandler> mPtrUIHandlers;

    public void addUIHandler(PtrUIHandler handler) {
        if (mPtrUIHandlers == null) {
            mPtrUIHandlers = new LinkedList<>();
        }
        if(!mPtrUIHandlers.contains(handler))
            mPtrUIHandlers.add(handler);
    }

    public void remove(PtrUIHandler handler) {
        if (mPtrUIHandlers != null)
            mPtrUIHandlers.remove(handler);
    }

    @Override
    public void onUIReset(SuperNestedLayout parent) {
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIReset(parent);
        }
    }

    @Override
    public void onUIRefreshPrepare(SuperNestedLayout parent) {
        mRefreshIndicator.onPressDown();
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIRefreshPrepare(parent);
        }
    }

    @Override
    public void onUIRefreshBegin(SuperNestedLayout parent) {
        if (mOnLoadListener != null) {
            mOnLoadListener.onLoad();
        }
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIRefreshBegin(parent);
        }

    }

    @Override
    public void onUIRefreshComplete(SuperNestedLayout parent) {
        mRefreshIndicator.onUIRefreshComplete();
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIRefreshComplete(parent);
        }
    }

    @Override
    public void onUIPositionChange(SuperNestedLayout parent, boolean isUnderTouch, byte status, PtrIndicator indicator) {
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIPositionChange(parent, isUnderTouch, status, indicator);
        }
    }

    OnLoadListener mOnLoadListener;
    CheckTriggerRunnable mCheckTriggerRunnable;
    public void setOnLoadListener(OnLoadListener l) {
        this.mOnLoadListener = l;
    }
    private class CheckTriggerRunnable implements Runnable {

        private final V mView;
        private long mBeginTime;
        WeakReference<View> mTarget;
        private boolean isRunning;
        private boolean cancel;
        long mMaxCheckDuriation = 300;
        CheckTriggerRunnable(V view) {
            mView = view;


        }
        public void cancel(){
           cancel = true;
            isRunning = false;
            mView.removeCallbacks(this);
            mBeginTime =0;
        }
        public void start(View target,float velocityY){
            mTarget = new WeakReference<View>(target);
            mBeginTime =   currentAnimationTimeMillis();
            isRunning = true;
            mView.postDelayed(this,60);
            cancel = false;
            mMaxCheckDuriation = (long) (velocityY/10)+200;
            if(mMaxCheckDuriation >1000){
                mMaxCheckDuriation = 1000;
            }

        }

        @Override
        public void run() {
             long duration= (AnimationUtils.currentAnimationTimeMillis()-mBeginTime)/1000;
            if (!cancel&&duration < mMaxCheckDuriation) {
                if(mTarget !=null){
                    View scrollView = mTarget.get();
                    if(scrollView !=null){
                        if(!ViewCompat.canScrollVertically(scrollView,1)){
                            mBeginTime =0;
                            if(mStatus == PTR_STATUS_INIT){
                                mStatus = PTR_STATUS_PREPARE;
                                RefreshFooterBehavior.this.onUIRefreshPrepare(mSuperNestedLayout);
                            }
                            startValueAnimitor(mView, -mRefreshIndicator.getStableRefreshOffset(), PTR_STATUS_LOADING);
                        }else{
//                        ViewCompat.postOnAnimation(mView, this);
                            mView.postDelayed(this,60);
                        }
                        return;
                    }
                }
            }
            this.onStop();
        }
        private void onStop(){
            cancel = false;
            mBeginTime =0;
            isRunning = false;

        }

        public boolean isRunning() {
            return isRunning;
        }
    }

    /**
     * A utility function to get the {@link RefreshFooterBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link RefreshFooterBehavior}.
     * @return The {@link RefreshFooterBehavior} associated with the {@code view}.
     */
    @SuppressWarnings("unchecked")
    public static <V extends View> RefreshFooterBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof SuperNestedLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of SuperNestedLayout");
        }
        Behavior behavior = ((SuperNestedLayout.LayoutParams) params)
                .getBehavior();
        if (!(behavior instanceof RefreshFooterBehavior)) {
            throw new IllegalArgumentException(
                    "The view is not associated with RefreshFooterBehavior");
        }
        return (RefreshFooterBehavior<V>) behavior;
    }
}
