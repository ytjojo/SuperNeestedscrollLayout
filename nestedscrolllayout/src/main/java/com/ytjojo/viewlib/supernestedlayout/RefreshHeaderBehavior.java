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
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.LinkedList;


/**
 * Created by Administrator on 2017/1/11 0011.
 */

public class RefreshHeaderBehavior<V extends View> extends Behavior<V> implements PtrUIHandler {
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
    private boolean canRefresh = true;

    public RefreshHeaderBehavior() {
        super();
    }

    public RefreshHeaderBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRefreshIndicator = new PtrIndicator();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RefreshBehavior);
        isKeepShowWhenLoading = a.getBoolean(R.styleable.RefreshBehavior_isKeepShowWhenLoading,true);
        mParallaxMult = a.getFloat(R.styleable.RefreshBehavior_parallaxMult,0);
        mFrictionFactor = a.getFloat(R.styleable.RefreshBehavior_frictionFactor,0.6f);
        mRefreshIndicator.setMaxDistanceRatio(a.getFloat(R.styleable.RefreshBehavior_maxDistanceRatio,3.0f));
        mRefreshIndicator.setRatioOfHeaderHeightToRefresh(a.getFloat(R.styleable.RefreshBehavior_ratioOfHeaderHeightToRefresh,1.2f));
        mRefreshIndicator.setMaxContentOffsetY(a.getDimensionPixelOffset(R.styleable.RefreshBehavior_ratioOfHeaderHeightToRefresh,-1));

        a.recycle();
    }
    @FloatRange(from=0f,to =1f)
    private float  mFrictionFactor = 0.6f;
    private int mMaxHeaderNestedScrollY;
    ViewOffsetHelper mOffsetHelper;
    private float mParallaxMult=0f;
    private float mMinHeaderOffset;
    private float mTotalHeaderOffset;
    public void setParallaxMult(float parallaxMult){
        this.mParallaxMult = parallaxMult;

    }
    private void checkParallaxMultValue(){
        if(mRefreshIndicator !=null&& mParallaxMult !=0f){
            mMinHeaderOffset = mRefreshIndicator.getStableRefreshOffset()*mParallaxMult;
            mTotalHeaderOffset = mRefreshIndicator.getStableRefreshOffset() - mMinHeaderOffset;
        }
    }
    @Override
    public boolean onStartNestedScroll(SuperNestedLayout superNestedLayout, V header, View directTargetChild, View target, int nestedScrollAxes) {
        mRefreshHeaderView = header;
        if(ViewCompat.getFitsSystemWindows(header)){
            ViewCompat.setFitsSystemWindows(header,false);

        }
        if (!canRefresh) {
            return false;
        }
        final int childCount = superNestedLayout.getChildCount();
        boolean found = false;
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
            if (viewBehavior != null&& viewBehavior instanceof ScrollViewBehavior) {
                hasScrollViewBehaviorViews.add(itemView);
            }
            if (mAnchorDirectChild != null) {
                if (mAnchorDirectChild == itemView) {
                    found = true;
                    continue;
                }
            } else {
                if(header ==itemView){
                    found = true;
                    continue;
                }
            }

            if (found && lp.isControlViewByBehavior(ScrollViewBehavior.sBehaviorName)) {
                mToTranslationYViews.add(itemView);
            }
        }
        if (hasScrollViewBehaviorViews.size() >= 1) {
           return hasScrollViewBehaviorViews.get(0) ==directTargetChild;
        }

        return false;
    }
    private void addToTranslationYViews(SuperNestedLayout superNestedLayout, V header){
        mRefreshHeaderView = header;
        if(ViewCompat.getFitsSystemWindows(header)){
            ViewCompat.setFitsSystemWindows(header,false);

        }
        final int childCount = superNestedLayout.getChildCount();
        boolean found = false;
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
            if (viewBehavior != null&& viewBehavior instanceof ScrollViewBehavior) {
                hasScrollViewBehaviorViews.add(itemView);
            }
            if (mAnchorDirectChild != null) {
                if (mAnchorDirectChild == itemView) {
                    found = true;
                    continue;
                }
            } else {
                if(header ==itemView){
                    found = true;
                    continue;
                }
            }

            if (found && lp.isControlViewByBehavior(ScrollViewBehavior.sBehaviorName)) {
                mToTranslationYViews.add(itemView);
            }
        }

    }

    @Override
    public void onNestedScrollAccepted(SuperNestedLayout superNestedLayout, V header, View directTargetChild, View target, int nestedScrollAxes) {
        super.onNestedScrollAccepted(superNestedLayout, header, directTargetChild, target, nestedScrollAxes);

        reset(superNestedLayout,header);
    }
    private void reset(SuperNestedLayout superNestedLayout, V header){
        mSuperNestedLayout = superNestedLayout;
        if(mOffsetHelper== null){
            mOffsetHelper = ViewOffsetHelper.getViewOffsetHelper(header);
        }
        addUIHandler((PtrUIHandler) header);
        isIgnore = false;
        if (!canRefresh) {
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
        mMaxHeaderNestedScrollY = (int)(mRefreshIndicator.getMaxContentOffsetY()/mFrictionFactor);
        checkParallaxMultValue();
    }
    public void setStableRefreshOffset(int stableRefreshOffset){
        if(mRefreshIndicator == null){
            mRefreshIndicator = new PtrIndicator();
        }
        mRefreshIndicator.setStableRefreshOffset(stableRefreshOffset);
    }
    public void setMaxContentOffset(int maxOffset){
        mRefreshIndicator.setMaxContentOffsetY(maxOffset);
    }
    private boolean isKeepShowWhenLoading;

    public void setKeepShowWhenLoading(boolean keepShowWhenLoading){
        isKeepShowWhenLoading = keepShowWhenLoading;
    }
    @Override
    public void onNestedPreScroll(SuperNestedLayout superNestedLayout, V header, View directTargetChild, View target, int dx, int dy, int[] consumed) {
        doOnNestedPreScroll(superNestedLayout,header,dx,dy,consumed);
    }
    private void doOnNestedPreScroll(SuperNestedLayout superNestedLayout, V header, int dx, int dy, int[] consumed){
        if ( isIgnore) {
            return;
        }
        if(isRunning()){
            return;
        }
        if (dy > 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_LOADING)) {
            final int childCount = mToTranslationYViews.size();
            if(isKeepShowWhenLoading && mStatus == PTR_STATUS_LOADING){
                float y =  ViewCompat.getTranslationY(mToTranslationYViews.get(0));
                if(y <=0){
                    return;
                }
                float contentNestedScrollDy =  (y/mFrictionFactor);
                float tempStart= contentNestedScrollDy;
                contentNestedScrollDy -=dy;
                if(contentNestedScrollDy <0){
                    contentNestedScrollDy = 0;
                }
                consumed[1] = (int) (tempStart- contentNestedScrollDy);
                float contentDy = (contentNestedScrollDy *mFrictionFactor);
                for (int i = 0; i < childCount; i++) {
                    View itemView = mToTranslationYViews.get(i);
                    ViewCompat.setTranslationY(itemView, contentDy);
                }
                float headerY = ViewCompat.getTranslationY(header);
                if(contentDy < mRefreshIndicator.getStableRefreshOffset()){
                    contentDy =mRefreshIndicator.getStableRefreshOffset();
                }
                if(headerY != contentDy){
                    ViewCompat.setTranslationY(header,contentDy);
                    if(mRefreshIndicator.getCurrentPosY() !=contentDy){
                        mRefreshIndicator.onMove(0,contentDy);
                        this.onUIPositionChange(superNestedLayout, true, mStatus, mRefreshIndicator);
                    }
                }
                mSuperNestedLayout.dispatchOnDependentViewChanged();
            }else{
                float y = mRefreshIndicator.getCurrentPosY();
                if(y <=0){
                    return;
                }
                float headerNestedScrollDy =  (y/mFrictionFactor);
                float start = headerNestedScrollDy;
                headerNestedScrollDy -=dy;
                if(headerNestedScrollDy <=0){
                    headerNestedScrollDy = 0;
                }
                float finalY = (headerNestedScrollDy * mFrictionFactor);
                consumed[1] = (int) (start - headerNestedScrollDy);

                setHeaderTranslationY(header,finalY);
                mRefreshIndicator.onMove(0, finalY);
                this.onUIPositionChange(superNestedLayout, true, mStatus, mRefreshIndicator);
                for (int i = 0; i < childCount; i++) {
                    View itemView = mToTranslationYViews.get(i);
                    if(y>finalY){
                        ViewCompat.setTranslationY(itemView, finalY);
                    }
                }
                mSuperNestedLayout.dispatchOnDependentViewChanged();
            }
        }
    }


    private void setHeaderTranslationY(V header,float y){
        if(y >=0 && y< mRefreshIndicator.getStableRefreshOffset()){
            if(mParallaxMult ==1f){
                ViewCompat.setTranslationY(header,mMinHeaderOffset);
            }else if(mParallaxMult !=0f){
                float ratio = y/mRefreshIndicator.getStableRefreshOffset();
                ViewCompat.setTranslationY(header,mMinHeaderOffset+ mTotalHeaderOffset*ratio);
            }else{
                ViewCompat.setTranslationY(header,y);
            }
        }else{
            ViewCompat.setTranslationY(header,y);
        }
    }


    @Override
    public void onNestedScroll(SuperNestedLayout superNestedLayout, V header, View directTargetChild, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed) {
        doOnestedScroll(superNestedLayout,header,dxConsumed,dyConsumed,dxUnconsumed,dyUnconsumed,consumed);
    }
    private void doOnestedScroll(SuperNestedLayout superNestedLayout, V header, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed){
        if (isIgnore) {
            return;
        }
        if(isRunning()){
            return;
        }
        if (dyUnconsumed < 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_INIT || mStatus == PTR_STATUS_LOADING)) {
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
            float consumedDy = 0f;
            float y =  ViewCompat.getTranslationY(mToTranslationYViews.get(0));
            if(y>=mRefreshIndicator.getMaxContentOffsetY()){
                return;
            }
            float contentNestedScrollDy = y/mFrictionFactor;
            float tempStart= contentNestedScrollDy;
            contentNestedScrollDy -=dyUnconsumed;
            if(contentNestedScrollDy > mMaxHeaderNestedScrollY){
                contentNestedScrollDy = mMaxHeaderNestedScrollY;
            }
            consumedDy =tempStart - contentNestedScrollDy;
            consumed[1] +=  consumedDy;
            float finalY = contentNestedScrollDy *mFrictionFactor;
//            int ssy = (int) ViewCompat.getTranslationY(header);
//            Logger.e("getTranslationY" +ssy +"finlay"+finalY);
            for (int i = 0; i < childCount; i++) {
                View itemView = mToTranslationYViews.get(i);
                ViewCompat.setTranslationY(itemView, finalY);
            }
            if (mRefreshIndicator.getCurrentPosY() != finalY) {
                setHeaderTranslationY(header,finalY);
                mRefreshIndicator.onMove(0, finalY);
                this.onUIPositionChange(superNestedLayout, true, mStatus, mRefreshIndicator);
            }

            mSuperNestedLayout.dispatchOnDependentViewChanged();

        }
    }


    private boolean isRuning;
    public boolean isRunning() {
//        return isRuning;
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

    boolean isIgnore;

    public void setRefreshComplete() {
        if (mStatus == PTR_STATUS_LOADING) {
            mStatus = PTR_STATUS_COMPLETE;
            if(mRefreshIndicator.getDelayScrollInitail()>0){
                mRefreshHeaderView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startValueAnimitor(mRefreshHeaderView, 0, PTR_STATUS_INIT);
                    }
                },mRefreshIndicator.getDelayScrollInitail());
            }else{
                startValueAnimitor(mRefreshHeaderView, 0, PTR_STATUS_INIT);

            }
            isIgnore = true;
            RefreshHeaderBehavior.this.onUIRefreshComplete(mSuperNestedLayout);
        }
    }
    public void setRefreshComplete(long delayToScroll) {
        if (mStatus == PTR_STATUS_LOADING) {
            mStatus = PTR_STATUS_COMPLETE;
            mRefreshHeaderView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startValueAnimitor(mRefreshHeaderView, 0, PTR_STATUS_INIT);
                }
            },delayToScroll);
            isIgnore = true;
            RefreshHeaderBehavior.this.onUIRefreshComplete(mSuperNestedLayout);
        }
    }
    public void scrollInitialPosition(){
        if(mStatus != PTR_STATUS_INIT){
            isIgnore = true;
            startValueAnimitor(mRefreshHeaderView, 0, PTR_STATUS_INIT);
        }
    }
    public void callRefreshCompleteOnly() {
        if (mStatus == PTR_STATUS_LOADING) {
            mStatus = PTR_STATUS_COMPLETE;
            isIgnore = true;
            RefreshHeaderBehavior.this.onUIRefreshComplete(mSuperNestedLayout);
        }
    }

    @Override
    public void onStopNestedScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target) {

        onStopDrag(superNestedLayout, child);

    }

    ValueAnimator mValueAnimator;

    private void startValueAnimitor(final V header, final int finalY, final byte endStatus) {
        if(isRunning()){
            mValueAnimator.cancel();
        }
        int y = (int) ViewCompat.getTranslationY(header);

        if(y==finalY){
            if (endStatus == PTR_STATUS_LOADING && mStatus != endStatus) {
                RefreshHeaderBehavior.this.onUIRefreshBegin(mSuperNestedLayout);
            } else if ((endStatus == PTR_STATUS_INIT) && mRefreshIndicator.isInStartPosition()) {
                RefreshHeaderBehavior.this.onUIReset(mSuperNestedLayout);
            }
            mStatus = endStatus;
            return;
        }
        int duration = 300;
        if (Math.abs(finalY - y) < header.getMeasuredHeight()) {
            duration = 200;
        }
        isRuning = true;
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
                    if(value < ViewCompat.getTranslationY(mToTranslationYViews.get(0))){
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
                RefreshHeaderBehavior.this.onUIPositionChange(mSuperNestedLayout, false, mStatus, mRefreshIndicator);
            }
        });
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (endStatus == PTR_STATUS_LOADING && mStatus != endStatus) {
                    RefreshHeaderBehavior.this.onUIRefreshBegin(mSuperNestedLayout);
                } else if ((endStatus == PTR_STATUS_INIT) && mRefreshIndicator.isInStartPosition()) {
                    RefreshHeaderBehavior.this.onUIReset(mSuperNestedLayout);
                }
                mStatus = endStatus;
                isRuning = false;
//                animation.end();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isRuning = false;
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
        return super.onNestedFling(superNestedLayout, child, directTargetChild, target, velocityX, velocityY, consumed);
    }

    @Override
    public void onStartDrag(SuperNestedLayout superNestedLayout, V child, int mInitialTouchX, int mInitialTouchY, boolean acceptedByAnother, Behavior accepteBehavior) {

        if(superNestedLayout.isPointBellowChildBounds(child,mInitialTouchX,mInitialTouchY)){

            addToTranslationYViews(superNestedLayout,child);
            if(mToTranslationYViews.size()>0){
                View view =  mToTranslationYViews.get(0);
                SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) view.getLayoutParams();
                if(view.getTop()== lp.getLayoutTop()){
                    setCanAcceptedDrag(true);
                    reset(superNestedLayout,child);
                }
            }
        }
    }


    @Override
    public void onStopDrag(SuperNestedLayout superNestedLayout, V child) {
        if (isIgnore) {
            isIgnore = false;
        }
        mRefreshIndicator.onRelease();
        if(isRunning()){
            return;
        }
        final int translationY = (int) ViewCompat.getTranslationY(mRefreshHeaderView);
        if(ViewCompat.getTranslationY(mRefreshHeaderView) ==0){
            if(mStatus == PTR_STATUS_INIT||mStatus == PTR_STATUS_COMPLETE){

            }else{
                mStatus = PTR_STATUS_INIT;
                RefreshHeaderBehavior.this.onUIReset(mSuperNestedLayout);
            }
            return;
        }
        if (translationY >= mRefreshIndicator.getOffsetToRefresh() && mStatus == PTR_STATUS_PREPARE) {
            startValueAnimitor(child, mRefreshIndicator.getStableRefreshOffset(), PTR_STATUS_LOADING);
        } else if (mStatus == PTR_STATUS_LOADING) {
            if(isKeepShowWhenLoading){
                startValueAnimitor(child, mRefreshIndicator.getStableRefreshOffset(), PTR_STATUS_LOADING);
            }else if(mRefreshIndicator.getCurrentPosY()> mRefreshIndicator.getStableRefreshOffset()){
                startValueAnimitor(child, mRefreshIndicator.getStableRefreshOffset(), PTR_STATUS_LOADING);
            }
        } else {
            startValueAnimitor(child, 0, PTR_STATUS_INIT);
        }
    }

    @Override
    public void onScrollBy(SuperNestedLayout superNestedLayout, V child, int dx, int dy, int[] consumed) {
        if(dy<0 ){
            this.doOnestedScroll(superNestedLayout,child,0,0,dx,dy,consumed);
        }else if(dy >0){
            this.doOnNestedPreScroll(superNestedLayout,child,dx,dy,consumed);
        }
//        if (isIgnore) {
//            return;
//        }
//        if(isRunning()){
//            return;
//        }
//        int dyUnconsumed = dy;
//        if(dyUnconsumed ==0){
//            return;
//        }
//        if (dyUnconsumed < 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_INIT || mStatus == PTR_STATUS_LOADING)) {
//
//
//            if (mStatus == PTR_STATUS_INIT) {
//                mStatus = PTR_STATUS_PREPARE;
//                this.onUIRefreshPrepare(nestedScrollLayout);
//            }
//            final int childCount = mToTranslationYViews.size();
//            int consumedDy = 0;
//            float y =  ViewCompat.getTranslationY(mToTranslationYViews.get(0));
//            if(y>=mRefreshIndicator.getMaxContentOffsetY()){
//                return;
//            }
//            int contentNestedScrollDy = (int) (y/mFrictionFactor);
//            int tempStart= contentNestedScrollDy;
//            contentNestedScrollDy -=dyUnconsumed;
//            if(contentNestedScrollDy > mMaxHeaderNestedScrollY){
//                contentNestedScrollDy = mMaxHeaderNestedScrollY;
//            }
//            consumedDy =tempStart - contentNestedScrollDy;
//            consumed[1] +=  consumedDy;
//            int finalY = (int) (contentNestedScrollDy *mFrictionFactor);
//            for (int i = 0; i < childCount; i++) {
//                View itemView = mToTranslationYViews.get(i);
//                ViewCompat.setTranslationY(itemView, finalY);
//            }
//            if (mRefreshIndicator.getCurrentPosY() != finalY) {
//                setHeaderTranslationY(child,finalY);
//                mRefreshIndicator.onMove(0, finalY);
//                this.onUIPositionChange(nestedScrollLayout, true, mStatus, mRefreshIndicator);
//            }
//            mNestedScrollLayout.dispatchOnDependentViewChanged();
//
//        }
//
//        if (dy > 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_LOADING)) {
//            final int childCount = mToTranslationYViews.size();
//            if(isKeepShowWhenLoading && mStatus == PTR_STATUS_LOADING){
//                float y =  ViewCompat.getTranslationY(mToTranslationYViews.get(0));
//                if(y <=0){
//                    return;
//                }
//                float contentNestedScrollDy =  (y/mFrictionFactor);
//                float tempStart= contentNestedScrollDy;
//                contentNestedScrollDy -=dy;
//                if(contentNestedScrollDy <0){
//                    contentNestedScrollDy = 0;
//                }
//                consumed[1] = (int) (tempStart- contentNestedScrollDy);
//                float contentDy = (contentNestedScrollDy *mFrictionFactor);
//                for (int i = 0; i < childCount; i++) {
//                    View itemView = mToTranslationYViews.get(i);
//                    ViewCompat.setTranslationY(itemView, contentDy);
//                }
//                mNestedScrollLayout.dispatchOnDependentViewChanged();
//                float headerY = ViewCompat.getTranslationY(child);
//                if(contentDy < mRefreshIndicator.getStableRefreshOffset()){
//                    contentDy =mRefreshIndicator.getStableRefreshOffset();
//                }
//                if(headerY != contentDy){
//                    ViewCompat.setTranslationY(child,contentDy);
//                    if(mRefreshIndicator.getCurrentPosY() !=contentDy){
//                        mRefreshIndicator.onMove(0,contentDy);
//                        this.onUIPositionChange(nestedScrollLayout, true, mStatus, mRefreshIndicator);
//                    }
//                }
//            }else{
//                float y =mRefreshIndicator.getCurrentPosY();
//                if(y <=0){
//                    return;
//                }
//                float headerNestedScrollDy =  (y/mFrictionFactor);
//                float start = headerNestedScrollDy;
//                headerNestedScrollDy -=dy;
//                if(headerNestedScrollDy <=0){
//                    headerNestedScrollDy = 0;
//                }
//                float finalY = (headerNestedScrollDy * mFrictionFactor);
//                consumed[1] = (int) (start - headerNestedScrollDy);
//                setHeaderTranslationY(child,finalY);
//                mRefreshIndicator.onMove(0, finalY);
//                this.onUIPositionChange(nestedScrollLayout, true, mStatus, mRefreshIndicator);
//                for (int i = 0; i < childCount; i++) {
//                    View itemView = mToTranslationYViews.get(i);
//                    if(y>finalY){
//                        ViewCompat.setTranslationY(itemView, finalY);
//                    }
//                }
//                mNestedScrollLayout.dispatchOnDependentViewChanged();
//            }
//        }
    }

    @Override
    public boolean onLayoutChild(SuperNestedLayout superNestedLayout, V child, int layoutDirection) {
        SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) child.getLayoutParams();

        if(lp.mAnchorView != null){
            View mAnchorView = lp.mAnchorView;
            int left = superNestedLayout.getPaddingLeft() + lp.leftMargin;
            SuperNestedLayout.LayoutParams anchorViewLp = (SuperNestedLayout.LayoutParams) mAnchorView.getLayoutParams();
            int bottom = mAnchorView.getBottom()+anchorViewLp.bottomMargin-lp.bottomMargin;
            child.layout(left,bottom- child.getMeasuredHeight(),left + child.getMeasuredWidth(),bottom);

        }else{
            int left = superNestedLayout.getPaddingLeft() + lp.leftMargin;
            int top = -lp.bottomMargin - child.getMeasuredHeight();
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }

        return true;
    }

    public void setCanRefresh(boolean canRefresh) {
        this.canRefresh = canRefresh;
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

    public void setOnLoadListener(OnLoadListener l) {
        this.mOnLoadListener = l;
    }

    /**
     * A utility function to get the {@link RefreshHeaderBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link RefreshHeaderBehavior}.
     * @return The {@link RefreshHeaderBehavior} associated with the {@code view}.
     */
    @SuppressWarnings("unchecked")
    public static <V extends View> RefreshHeaderBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof SuperNestedLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of SuperNestedLayout");
        }
        Behavior behavior = ((SuperNestedLayout.LayoutParams) params)
                .getBehavior();
        if (!(behavior instanceof RefreshHeaderBehavior)) {
            throw new IllegalArgumentException(
                    "The view is not associated with RefreshHeaderBehavior");
        }
        return (RefreshHeaderBehavior<V>) behavior;
    }

}
