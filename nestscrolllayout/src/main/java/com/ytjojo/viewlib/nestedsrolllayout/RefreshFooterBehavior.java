package com.ytjojo.viewlib.nestedsrolllayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.FloatRange;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import com.orhanobut.logger.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

import static android.view.animation.AnimationUtils.currentAnimationTimeMillis;

/**
 * Created by Administrator on 2017/3/13 0013.
 */

public class RefreshFooterBehavior <V extends View> extends Behavior<V> implements PtrUIHandler  {
    NestedScrollLayout mNestedScrollLayout;
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
    }
    @FloatRange(from=0f,to =1f)
    private float  mFrictionFactor = 1f;
    private int mMaxHeaderNestedScrollY;
    private boolean mTriggerSensitive = false;
    ViewOffsetHelper mOffsetHelper;

    @Override
    public boolean onStartNestedScroll(NestedScrollLayout nestedScrollLayout, V header, View directTargetChild, View target, int nestedScrollAxes) {
        mRefreshHeaderView = header;
        if(ViewCompat.getFitsSystemWindows(header)){
            ViewCompat.setFitsSystemWindows(header,false);

        }
        if (!canLoad) {
            return false;
        }
        final int childCount = nestedScrollLayout.getChildCount();
        mToTranslationYViews.clear();
        NestedScrollLayout.LayoutParams headerLp = (NestedScrollLayout.LayoutParams) header.getLayoutParams();
        View mAnchorDirectChild = headerLp.mAnchorDirectChild;
        ArrayList<View> hasScrollViewBehaviorViews = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            View itemView = nestedScrollLayout.getChildAt(i);
            NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) itemView.getLayoutParams();
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
    public void onNestedScrollAccepted(NestedScrollLayout nestedScrollLayout, V header, View directTargetChild, View target, int nestedScrollAxes) {
        super.onNestedScrollAccepted(nestedScrollLayout, header, directTargetChild, target, nestedScrollAxes);
        mNestedScrollLayout = nestedScrollLayout;
        if(mOffsetHelper== null){
            mOffsetHelper = ViewOffsetHelper.getViewOffsetHelper(header);
        }
        addUIHandler((PtrUIHandler) header);
        isIgnore = false;
        if (!canLoad) {
            isIgnore = true;
            return;
        }
        NestedScrollLayout.LayoutParams headerLp = (NestedScrollLayout.LayoutParams) header.getLayoutParams();
        if (mRefreshIndicator == null) {
            mRefreshIndicator = new PtrIndicator();
            int height = header.getMeasuredHeight() + headerLp.topMargin + headerLp.bottomMargin;
            mRefreshIndicator.setHeaderHeight(height);
            if(mTriggerSensitive){
                if(mRefreshIndicator.getRatioOfHeaderToHeightRefresh()>0.3f){
                    mRefreshIndicator.setRatioOfHeaderHeightToRefresh(0.3f);
                }
            }
        }
        mMaxHeaderNestedScrollY = (int)(mRefreshIndicator.getMaxOffsetY()/mFrictionFactor);
        if(mCheckTriggerRunnable !=null &&mCheckTriggerRunnable.isRunning()){
            mCheckTriggerRunnable.cancel();
        }

    }
    public void setTriggerSensitive(boolean triggerSensitive){
        this.mTriggerSensitive = triggerSensitive;
        if(mTriggerSensitive){
            if(mRefreshIndicator.getRatioOfHeaderToHeightRefresh()>0.3f){
                mRefreshIndicator.setRatioOfHeaderHeightToRefresh(0.3f);
            }
        }
    }

    @Override
    public void onNestedPreScroll(NestedScrollLayout nestedScrollLayout, V header, View directTargetChild, View target, int dx, int dy, int[] consumed) {
        if ( isIgnore) {
            return;
        }
        if(isRunning()){
            return;
        }
        if (dy < 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_LOADING)) {
            final int childCount = mToTranslationYViews.size();
            if(mStatus == PTR_STATUS_LOADING){
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
                mNestedScrollLayout.dispatchOnDependentViewChanged();
                float headerY = ViewCompat.getTranslationY(header);
                if(!mTriggerSensitive &&contentDy > -mRefreshIndicator.getHeaderHeight()){
                    contentDy = -mRefreshIndicator.getHeaderHeight();
                }
                if(headerY != contentDy){
                    ViewCompat.setTranslationY(header,contentDy);
                    this.onUIPositionChange(nestedScrollLayout, true, mStatus, mRefreshIndicator);
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
                this.onUIPositionChange(nestedScrollLayout, true, mStatus, mRefreshIndicator);
                for (int i = 0; i < childCount; i++) {
                    View itemView = mToTranslationYViews.get(i);
                    if(headerY <finalY){
                        ViewCompat.setTranslationY(itemView, finalY);
                    }
                }
                mNestedScrollLayout.dispatchOnDependentViewChanged();
            }
        }
    }

    @Override
    public void onNestedScroll(NestedScrollLayout nestedScrollLayout, V header, View directTargetChild, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed) {
        if (isIgnore) {
            return;
        }
        if(isRunning()){
            return;
        }
        if (dyUnconsumed > 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_INIT || mStatus == PTR_STATUS_LOADING)) {
            mNestedScrollLayout.dispatchNestedScroll(dxConsumed,dyConsumed,dxUnconsumed,dyUnconsumed,consumed);
            dyUnconsumed-=consumed[1];

            if(dyUnconsumed ==0){
                return;
            }

            if (mStatus == PTR_STATUS_INIT) {
                mStatus = PTR_STATUS_PREPARE;
                this.onUIRefreshPrepare(nestedScrollLayout);
            }
            final int childCount = mToTranslationYViews.size();
            int consumedDy = 0;
            float y =  ViewCompat.getTranslationY(mToTranslationYViews.get(0));
            if(y<= -mRefreshIndicator.getMaxOffsetY()){
                return;
            }
            int contentNestedScrollDy = (int) (y/mFrictionFactor);
            int tempStart= contentNestedScrollDy;
            contentNestedScrollDy -=dyUnconsumed;
            if(contentNestedScrollDy <= - mMaxHeaderNestedScrollY){
                contentNestedScrollDy = -mMaxHeaderNestedScrollY;
            }
            consumedDy =tempStart - contentNestedScrollDy;
            consumed[1] +=  consumedDy;
            int finalY = (int) (contentNestedScrollDy *mFrictionFactor);
            int ssy = (int) ViewCompat.getTranslationY(header);
            Logger.e("getTranslationY" +ssy +"finlay"+finalY);
            for (int i = 0; i < childCount; i++) {
                View itemView = mToTranslationYViews.get(i);
                ViewCompat.setTranslationY(itemView, finalY);
            }
            if (ViewCompat.getTranslationY(header) > finalY) {
                ViewCompat.setTranslationY(header, finalY);
                mRefreshIndicator.onMove(0, finalY);
                this.onUIPositionChange(nestedScrollLayout, true, mStatus, mRefreshIndicator);
            }
            mNestedScrollLayout.dispatchOnDependentViewChanged();
            if(mTriggerSensitive){
                if (mStatus == PTR_STATUS_PREPARE&& ViewCompat.getTranslationY(header) < -mRefreshIndicator.getOffsetToRefresh()) {
                    mStatus = PTR_STATUS_LOADING;
                    RefreshFooterBehavior.this.onUIRefreshBegin(mNestedScrollLayout);
                }
            }

        }
    }
    public boolean isRunning() {
//        return isRuning;
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

    boolean isIgnore;

    public void setLoadComplete() {
        if (mStatus == PTR_STATUS_LOADING) {
            mStatus = PTR_STATUS_COMPLETE;
            startValueAnimitor(mRefreshHeaderView, 0, PTR_STATUS_INIT);
            isIgnore = true;
            RefreshFooterBehavior.this.onUIRefreshComplete(mNestedScrollLayout);
        }
    }

    @Override
    public void onStopNestedScroll(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target) {
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
                if(!mTriggerSensitive){
                    mStatus = PTR_STATUS_INIT;
                    RefreshFooterBehavior.this.onUIReset(mNestedScrollLayout);
                }
            }
            return;
        }
        if (translationY <= -mRefreshIndicator.getOffsetToRefresh() && mStatus == PTR_STATUS_PREPARE) {
            if(mTriggerSensitive){
                mStatus = PTR_STATUS_LOADING;
                RefreshFooterBehavior.this.onUIRefreshBegin(mNestedScrollLayout);
            }else{
                startValueAnimitor(child, -mRefreshIndicator.getHeaderHeight(), PTR_STATUS_LOADING);
            }
        } else if (mStatus == PTR_STATUS_LOADING) {
            if(translationY < -mRefreshIndicator.getHeaderHeight()){
                startValueAnimitor(child, -mRefreshIndicator.getHeaderHeight(), PTR_STATUS_LOADING);
            }
        } else {
            if(!mTriggerSensitive){
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
                RefreshFooterBehavior.this.onUIRefreshBegin(mNestedScrollLayout);
            } else if ((endStatus == PTR_STATUS_INIT) && mRefreshIndicator.isInStartPosition()) {
                RefreshFooterBehavior.this.onUIReset(mNestedScrollLayout);
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
                    if(mTriggerSensitive||value > ViewCompat.getTranslationY(mToTranslationYViews.get(0))){
                        for (int i = 0; i < count; i++) {
                            View view = mToTranslationYViews.get(i);
                            ViewCompat.setTranslationY(view, value);
                        }
                        mNestedScrollLayout.dispatchOnDependentViewChanged();
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
                    mNestedScrollLayout.dispatchOnDependentViewChanged();
                }
                RefreshFooterBehavior.this.onUIPositionChange(mNestedScrollLayout, false, mStatus, mRefreshIndicator);
            }
        });
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (endStatus == PTR_STATUS_LOADING && mStatus != endStatus) {
                    RefreshFooterBehavior.this.onUIRefreshBegin(mNestedScrollLayout);
                } else if ((endStatus == PTR_STATUS_INIT) && mRefreshIndicator.isInStartPosition()) {
                    RefreshFooterBehavior.this.onUIReset(mNestedScrollLayout);
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
    public boolean onNestedPreFling(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target, float velocityX, float velocityY) {
        return super.onNestedPreFling(nestedScrollLayout, child, directTargetChild, target, velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target, float velocityX, float velocityY, boolean consumed) {
        if(!mTriggerSensitive){
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
                    RefreshFooterBehavior.this.onUIRefreshPrepare(mNestedScrollLayout);
                }
                startValueAnimitor(child, -mRefreshIndicator.getHeaderHeight(), PTR_STATUS_LOADING);
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


        return super.onNestedFling(nestedScrollLayout, child, directTargetChild, target, velocityX, velocityY, consumed);
    }



    @Override
    public void onStopDrag(NestedScrollLayout nestedScrollLayout) {
        super.onStopDrag(nestedScrollLayout);
    }

    @Override
    public void onScrollBy(NestedScrollLayout nestedScrollLayout, V child, int dx, int dy, int[] consumed) {
        super.onScrollBy(nestedScrollLayout, child, dx, dy, consumed);
    }

    @Override
    public boolean onLayoutChild(NestedScrollLayout nestedScrollLayout, V child, int layoutDirection) {
        NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();

        if(lp.mAnchorView != null){
            View mAnchorView = lp.mAnchorView;
            int left = nestedScrollLayout.getPaddingLeft() + lp.leftMargin;
            NestedScrollLayout.LayoutParams anchorViewLp = (NestedScrollLayout.LayoutParams) mAnchorView.getLayoutParams();
            int top = mAnchorView.getTop()-anchorViewLp.topMargin+lp.topMargin;
            child.layout(left, top - child.getMeasuredHeight(),left + child.getMeasuredWidth(), top);

        }else{
            int left = nestedScrollLayout.getPaddingLeft() + lp.leftMargin;
            int top = lp.topMargin + nestedScrollLayout.getMeasuredHeight()-nestedScrollLayout.getPaddingBottom();
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
    public void onUIReset(NestedScrollLayout parent) {
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIReset(parent);
        }
    }

    @Override
    public void onUIRefreshPrepare(NestedScrollLayout parent) {
        mRefreshIndicator.onPressDown();
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIRefreshPrepare(parent);
        }
    }

    @Override
    public void onUIRefreshBegin(NestedScrollLayout parent) {
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
    public void onUIRefreshComplete(NestedScrollLayout parent) {
        mRefreshIndicator.onUIRefreshComplete();
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIRefreshComplete(parent);
        }
    }

    @Override
    public void onUIPositionChange(NestedScrollLayout parent, boolean isUnderTouch, byte status, PtrIndicator indicator) {
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
                                RefreshFooterBehavior.this.onUIRefreshPrepare(mNestedScrollLayout);
                            }
                            startValueAnimitor(mView, -mRefreshIndicator.getHeaderHeight(), PTR_STATUS_LOADING);
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

}
