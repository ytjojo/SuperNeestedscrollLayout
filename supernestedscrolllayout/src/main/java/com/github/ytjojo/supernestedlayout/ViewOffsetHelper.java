package com.github.ytjojo.supernestedlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Administrator on 2017/1/5 0005.
 */
public class ViewOffsetHelper {
    private final ScrollerCompat mScroller;
    private ValueAnimator mOffsetAnimator;
    private SuperNestedLayout mParent;

    public static ViewOffsetHelper getViewOffsetHelper(View view) {
        ViewOffsetHelper offsetHelper = (ViewOffsetHelper) view.getTag(com.github.ytjojo.supernestedlayout.R.id.view_offset_helper);
        if (offsetHelper == null) {
            offsetHelper = new ViewOffsetHelper(view);
            view.setTag(com.github.ytjojo.supernestedlayout.R.id.view_offset_helper, offsetHelper);
        }
        return offsetHelper;
    }
    public static void setViewOffsetHelper(View view,ViewOffsetHelper helper) {
        view.setTag(com.github.ytjojo.supernestedlayout.R.id.view_offset_helper,helper);
    }
    private final View mView;
    private View mHeader;
    private int mMinHeaderTopOffset;
    public void setHeaderViewMinOffsetTop(int minHeaderTopOffset){
        this.mMinHeaderTopOffset = minHeaderTopOffset;
    }
    public int getMinHeaderTopOffset(){
        return mMinHeaderTopOffset;
    }
    public void setHeaderView(View header){
        mHeader = header;
    }
    public ViewOffsetHelper(View view) {
        mParent = (SuperNestedLayout) view.getParent();
        mView = view;
        mScroller = ScrollerCompat.create(view.getContext(), null);
        setupAnimators();
    }
    private int mLayoutTop = Integer.MIN_VALUE;
    private int mOffsetTop;
    private ArrayList<View> mScrollViews;
    public void addScrollViews(View v){
        if(mScrollViews ==null){
            mScrollViews = new ArrayList<>();
        }
        if(!mScrollViews.contains(v)){
            mScrollViews.add(v);
        }
    }
    public void initLayoutTop(){
        if(mLayoutTop == Integer.MIN_VALUE){
           SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) mView.getLayoutParams();
            mLayoutTop = lp.getLayoutTop();
        }
    }
    public int mHeaderOffsetValue;
    private void updateOffsets() {
        int offsetDy = mOffsetTop - (mView.getTop() -mLayoutTop);

//        if(mHeader !=null &&mOffsetTop <0 &&mOffsetTop >= mMinHeaderTopOffset){
//            int top = mHeader.getTop()+offsetDy;
//            SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) mHeader.getLayoutParams();
//            if(top>lp.mLayoutTop){
//                top = lp.mLayoutTop;
//            }else if(top<mMinHeaderTopOffset+mHeaderOffsetValue+lp.mTopInset){
//                top = mMinHeaderTopOffset+mHeaderOffsetValue+lp.mTopInset;
//            }
//
//            int headerDy = top - mHeader.getTop();
//            if(mOffsetTop ==mMinHeaderTopOffset && headerDy>0){
//                headerDy = 0;
//            }
//            if(offsetDy>0){
//                if(mOffsetTop-offsetDy < mMinHeaderTopOffset ){
//                    headerDy =offsetDy -( mMinHeaderTopOffset - (mOffsetTop-offsetDy));
//                }
//            }else{
//
//            }
//            if(headerDy!=0){
//                ViewCompat.offsetTopAndBottom(mHeader,headerDy);
//                dispatchScrollChanged(mHeader,mHeader.getTop(),mHeader.getTop()+headerDy,headerDy,mMinHeaderTopOffset);
//            }
//
//        }else if(mHeader !=null && mOffsetTop >= 0){
//            SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) mHeader.getLayoutParams();
//            int headerDy = mOffsetTop - (mHeader.getTop() -lp.getLayoutTop());
//            ViewCompat.offsetTopAndBottom(mHeader,headerDy);
//            dispatchScrollChanged(mHeader,mOffsetTop - offsetDy,mOffsetTop,offsetDy,mMinHeaderTopOffset);
//        }else{
//            if(mOffsetTop-offsetDy > mMinHeaderTopOffset ){
//               int headerDy =mMinHeaderTopOffset - (mOffsetTop-offsetDy);
//                ViewCompat.offsetTopAndBottom(mHeader,headerDy);
//                dispatchScrollChanged(mHeader,mHeader.getTop(),mHeader.getTop()+headerDy,headerDy,mMinHeaderTopOffset);
//            }
//
//
//        }
        if(mHeader !=null){
            SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) mHeader.getLayoutParams();
            int minOffset = lp.getDownNestedPreScrollRange();
            if(lp.isEnterAlwaysFlag()&&ViewCompat.getMinimumHeight(mHeader)>0){
                boolean handler = mOffsetTop-mHeaderOffsetValue -minOffset< mMinHeaderTopOffset;
                if(handler){
                    int top = mHeader.getTop()+offsetDy;
                    if(top>mMinHeaderTopOffset+mHeaderOffsetValue+lp.mTopInset+minOffset){
                        top = mMinHeaderTopOffset+mHeaderOffsetValue+lp.mTopInset+minOffset;
                    }else if(top<mMinHeaderTopOffset+mHeaderOffsetValue+lp.mTopInset){
                        top = mMinHeaderTopOffset+mHeaderOffsetValue+lp.mTopInset;
                    }
                    int headerDy = top - mHeader.getTop();
                    if(headerDy!=0){
                        ViewCompat.offsetTopAndBottom(mHeader,headerDy);
                        dispatchScrollChanged(mHeader,mHeader.getTop()-lp.mTopInset,mHeader.getTop()-lp.mTopInset+headerDy,headerDy,mMinHeaderTopOffset);
                    }
                }else{
                    if(mOffsetTop>=mMinHeaderTopOffset){
                        int headerDy = mOffsetTop - (mHeader.getTop() -lp.getLayoutTop());
                        ViewCompat.offsetTopAndBottom(mHeader,headerDy);
                        dispatchScrollChanged(mHeader,mOffsetTop - offsetDy,mOffsetTop,offsetDy,mMinHeaderTopOffset);

                    }
                }
            }else{
//                SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) mHeader.getLayoutParams();
                if(mOffsetTop>=mMinHeaderTopOffset){
                    int headerDy = mOffsetTop - (mHeader.getTop() -lp.getLayoutTop());
                    ViewCompat.offsetTopAndBottom(mHeader,headerDy);
                    dispatchScrollChanged(mHeader,mOffsetTop - offsetDy,mOffsetTop,offsetDy,mMinHeaderTopOffset);

                }

            }
        }else if(mHeader!=null &&mOffsetTop>=0){
            SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) mHeader.getLayoutParams();
            int headerDy = mOffsetTop - (mHeader.getTop() -lp.getLayoutTop());
            ViewCompat.offsetTopAndBottom(mHeader,headerDy);
            dispatchScrollChanged(mHeader,mOffsetTop - offsetDy,mOffsetTop,offsetDy,mMinHeaderTopOffset);
        }

        if(mScrollViews !=null){
            for(View v:mScrollViews){
                ViewCompat.offsetTopAndBottom(v,offsetDy);
            }
        }
        if(mOnOffsetChangedListeners !=null){
            for (OffsetTopChangeCallback l:mOnOffsetChangedListeners){
                l.onOffsetTopChanged(mOffsetTop,offsetDy);
            }
        }
        mParent.dispatchOnDependentViewChanged();

    }

    /**
     * Set the top and bottom offset for this {@link ViewOffsetHelper}'s view.
     *
     * @param offset the offset in px.
     * @return true if the offset has changed
     */
    public boolean setTopAndBottomOffset(int offset) {
        initLayoutTop();
        resetOffsetTop();
        if (mOffsetTop != offset) {
            mOffsetTop = offset;
            updateOffsets();
            return true;
        }
        return false;
    }
    public void restoreTopAndBottomOffset(){
        int offsetDy = mOffsetTop - (mView.getTop() -mLayoutTop);
        initLayoutTop();
        if(mScrollViews !=null){
            for(View v:mScrollViews){
                ViewCompat.offsetTopAndBottom(v,offsetDy);
            }
        }
        if(mOnOffsetChangedListeners !=null){
            for (OffsetTopChangeCallback l:mOnOffsetChangedListeners){
                l.onOffsetTopChanged(mOffsetTop,offsetDy);
            }
        }
        mParent.dispatchOnDependentViewChanged();

    }
    public void restoreTopAndBottomOffset(int topAndBottomOffset){
        this.mOffsetTop = topAndBottomOffset;
        restoreTopAndBottomOffset();
    }

    private int mHeaderOffsetTop;
    private void saveHeaderOffsetTop(){
        if(mHeader != null){
            mHeaderOffsetTop =  mHeader.getTop();
        }else{

        }
    }
    public int getHeaderOffsetTop(){
        return mHeaderOffsetTop;
    }
    public void restoreHeaderTop(int top){
        if(mHeader != null){
            SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) mHeader.getLayoutParams();
            int value = top - mHeader.getTop();
            if(value !=0){
                mHeaderOffsetTop = top;
                if(value!=0){
                    ViewCompat.offsetTopAndBottom(mHeader,value);
                    dispatchScrollChanged(mHeader,mHeader.getTop()-value,mHeader.getTop(),value,mMinHeaderTopOffset);
                }
            }
        }
    }


    public int getTopAndBottomOffset() {
        return mOffsetTop;
    }

    ValueAnimator.AnimatorUpdateListener mOverScrollUpdateListener;
    public static final  int ANIMATE_OFFSET_DIPS_PER_SECOND = 300;
    public boolean animateOffsetTo( final int toOffsetTop,final AnimCallback callback) {
        if (mOffsetTop == toOffsetTop) {
            if (mValueAnimator != null && mValueAnimator.isRunning()) {
                mValueAnimator.removeAllUpdateListeners();
                mValueAnimator.cancel();
            }
            return false;
        }
        if (mOverScrollUpdateListener == null) {
            mOverScrollUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    int value = (int) animator.getAnimatedValue();
                    setTopAndBottomOffset(value);
                    if(callback !=null){
                        callback.onAnimationUpdate(value);
                    }
                }
            };

        }
        mValueAnimator.removeAllUpdateListeners();
        mValueAnimator.cancel();
        mValueAnimator.setInterpolator(mDecelerateInterpolator);
        mValueAnimator.addUpdateListener(mOverScrollUpdateListener);
        // Set the duration based on the amount of dips we're travelling in
        final float distanceDp = Math.abs(mOffsetTop - toOffsetTop) /
                mView.getResources().getDisplayMetrics().density;
        int duration = Math.round(distanceDp * 1000 / ANIMATE_OFFSET_DIPS_PER_SECOND);

        mValueAnimator.setDuration(Math.min(duration,500));

        mValueAnimator.setIntValues(mOffsetTop, toOffsetTop);
        mValueAnimator.removeAllListeners();
        if(mAnimatorListener ==null){
            mAnimatorListener = new AnimatorListener(callback);
        }
        mAnimatorListener.setCallback(callback);
        mValueAnimator.addListener(new AnimatorListener(callback));
        mValueAnimator.start();
        return true;
    }
    AnimatorListener mAnimatorListener;
    public boolean animateOffsetTo( final int toOffsetTop) {

        return animateOffsetTo(toOffsetTop,null);
    }


    ValueAnimator mValueAnimator;
    ValueAnimator.AnimatorUpdateListener mUpdateListener;
    AnimCallback mAnimcallback;
    public void setAnimtionCallback(AnimCallback callback){
        this.mAnimcallback = callback;
    }
    private void setupAnimators() {
        mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mScroller.computeScrollOffset()) {
                    int value = mScroller.getCurrY();
                    setTopAndBottomOffset(value);
                    if(mAnimcallback != null){
                        mAnimcallback.onAnimationUpdate(value);
                    }

                } else {
                    stopScroll();
                    if(mAnimcallback != null){
                        mAnimcallback.onAnimationEnd();
                    }
                }
            }
        };
        mValueAnimator =  new ValueAnimator();
        mValueAnimator.setIntValues(0, 10000);
        mValueAnimator.setRepeatCount(0);

    }
    LinearInterpolator mLinearInterpolator =new LinearInterpolator();
    DecelerateInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    public void startAnim() {
        mValueAnimator.removeAllListeners();
        mValueAnimator.removeAllUpdateListeners();
        mValueAnimator.setDuration(2500);
        mValueAnimator.setIntValues(0, 10000);
        mValueAnimator.setInterpolator(mLinearInterpolator);
        mValueAnimator.start();
        mValueAnimator.addUpdateListener(mUpdateListener);
        mValueAnimator.addListener(mAnimatorListener);
    }

    public void stopScroll() {
        mValueAnimator.removeAllListeners();
        if(mValueAnimator.isRunning()){
            mValueAnimator.cancel();
        }
        mValueAnimator.removeAllUpdateListeners();
        mValueAnimator.setDuration(0);
        mScroller.abortAnimation();
    }

    boolean isSnapScroll =false;
    public boolean fling(int velocityY, final int minY, final int maxY) {
        stopScroll();
        if(velocityY>0){
            if(mOffsetTop==Math.max(minY,maxY)){
                return false;
            }
            mScroller.fling(0, mOffsetTop, 0, -velocityY, 0, 0,Math.min(minY, maxY),
                    isSnapScroll ?Math.min(minY, maxY):Math.max(minY,maxY));
        }else{
            if(mOffsetTop==Math.min(minY,maxY)){
                return false;
            }
            mScroller.fling(0, mOffsetTop, 0, -velocityY, 0, 0,  isSnapScroll ?Math.max(minY,maxY):Math.min(minY,maxY),
                   Math.max(minY,maxY));
        }

        startAnim();
        return true;
    }

    public void resetOffsetTop() {
        SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) mView.getLayoutParams();
        mOffsetTop = mView.getTop()- lp.getLayoutTop();
    }

    public static final int MAX_OFFSET_ANIMATION_DURATION = 600;
    private void animateOffsetTo(  final int offset, float velocity) {
        resetOffsetTop();
        final int distance = offset - getTopAndBottomOffset();
        final int duration ;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 3 * Math.round(1000 * (distance / velocity));
        } else {
            final float distanceRatio = (float) distance /mHeader.getMeasuredHeight();
            duration = (int) ((distanceRatio + 1) * 150);
        }

        animateOffsetWithDuration( offset, duration);
    }

    private void animateOffsetWithDuration( final int offset, final int duration) {
        final int currentOffset = getTopAndBottomOffset();
        if (currentOffset == offset) {
            if (mOffsetAnimator != null && mOffsetAnimator.isRunning()) {
                mOffsetAnimator.cancel();
            }
            return;
        }

        if (mOffsetAnimator == null) {
            mOffsetAnimator = ValueAnimator.ofInt(0,1);
            mOffsetAnimator.setInterpolator(new DecelerateInterpolator());
            mOffsetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                }
            });
        } else {
            mOffsetAnimator.cancel();
        }

        mOffsetAnimator.setDuration(Math.min(duration, MAX_OFFSET_ANIMATION_DURATION));
        mOffsetAnimator.setIntValues(currentOffset, offset);
        mOffsetAnimator.start();
    }
    private void dispatchScrollChanged(View child, int startOffsetTop, int endOffsetTop, int parentScrollDy, int rangeEnd){
        saveHeaderOffsetTop();
        final SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) child.getLayoutParams();
        if(!lp.hasOffsetChangedListener()){
            return;
        }
        final int min = Math.min(rangeEnd,lp.mOverScrollDistance);
        final int  max = Math.max(rangeEnd,lp.mOverScrollDistance);
        int range = 0 - min;
        int top = child.getTop();
        int maxTop = (lp.isApplyInsets?lp.mTopInset:0)+lp.mOverScrollDistance;
        int minTop =maxTop - lp.mOverScrollDistance-child.getMeasuredHeight() -lp.topMargin+lp.bottomMargin  + lp.getUpNestedPreScrollRange() ;
        if(top<=maxTop &&top>=minTop){
            int offsetDy = 0;
            float rate = .0f;
            int offsetPix = 0;
            int endScrollYModify = endOffsetTop;
            if(endScrollYModify < min){
                endScrollYModify = min ;
            }
            if(endScrollYModify > max){
                endScrollYModify = max;
            }
            offsetPix = endScrollYModify;
            rate =(offsetPix)/((float) range);
            offsetDy = endScrollYModify - startOffsetTop;
            lp.onScrollChanged(rate,offsetDy,offsetPix,range,parentScrollDy);
        }
//        Log.e("endOffsetTop","endOffsetTop" +endOffsetTop);

    }
    private void dispatchScrollChanged(View child, int startOffsetTop, int endOffsetTop, int parentScrollDy){
        final SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) child.getLayoutParams();
        if(!lp.hasOffsetChangedListener()){
            return;
        }
        final int top = child.getTop();
        if(top<=0&&top>=-child.getMeasuredHeight()){

        }
        int rangeEnd = child.getMeasuredHeight();
        final int min = Math.min(rangeEnd,0);
        final int  max = Math.max(rangeEnd,0);
        int range = max - min;
        if(Math.max(startOffsetTop, endOffsetTop)>=min&& Math.min(startOffsetTop, endOffsetTop)<= max){
            int offsetDy = 0;
            float rate = .0f;
            int offsetPix = 0;
            int endScrollYModify = endOffsetTop;
            if(endScrollYModify < min){
                endScrollYModify = min ;
            }
            if(endScrollYModify > max){
                endScrollYModify = max;
            }
            offsetPix = endScrollYModify;
//
            rate =(offsetPix)/((float) range);
            offsetDy = endScrollYModify - startOffsetTop;
            lp.onScrollChanged(rate,offsetDy,offsetPix,range,parentScrollDy);
        }

    }
    public static class AnimatorListener implements  Animator.AnimatorListener{
        AnimCallback callback;
        public void setCallback(AnimCallback callback){
            this.callback = callback;
        }
        public AnimatorListener(AnimCallback callback){
            this.callback = callback;
        }
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if(callback !=null){
                callback.onAnimationEnd();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }
    public  interface AnimCallback{
        void onAnimationUpdate(int value);
        void onAnimationEnd();
    }

    LinkedList<OffsetTopChangeCallback> mOnOffsetChangedListeners;

    public void addOnOffsetChangedListener(OffsetTopChangeCallback l) {
        if (mOnOffsetChangedListeners == null) {
            mOnOffsetChangedListeners = new LinkedList<>();
        }
        if(!mOnOffsetChangedListeners.contains(l))
            mOnOffsetChangedListeners.add(l);
    }

    public void removeOnOffsetChangedListener(OffsetTopChangeCallback l) {
        if (mOnOffsetChangedListeners != null) {
            mOnOffsetChangedListeners.remove(l);
        }

    }
    public interface OffsetTopChangeCallback{
        void onOffsetTopChanged(int offsetTop,int dv);
    }
}
