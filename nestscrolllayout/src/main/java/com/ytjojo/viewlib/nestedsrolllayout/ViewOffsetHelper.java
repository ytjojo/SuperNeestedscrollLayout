package com.ytjojo.viewlib.nestedsrolllayout;

import android.animation.ValueAnimator;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/1/5 0005.
 */
public class ViewOffsetHelper {
    private final ScrollerCompat mScroller;
    private ValueAnimator mOffsetAnimator;
    private NestedScrollLayout mParent;

    public static ViewOffsetHelper getViewOffsetHelper(View view) {
        ViewOffsetHelper offsetHelper = (ViewOffsetHelper) view.getTag(com.ytjojo.viewlib.nestedsrolllayout.R.id.view_offset_helper);
        if (offsetHelper == null) {
            offsetHelper = new ViewOffsetHelper(view);
            view.setTag(com.ytjojo.viewlib.nestedsrolllayout.R.id.view_offset_helper, offsetHelper);
        }
        return offsetHelper;
    }
    public static void setViewOffsetHelper(View view,ViewOffsetHelper helper) {
        view.setTag(com.ytjojo.viewlib.nestedsrolllayout.R.id.view_offset_helper,helper);
    }
    private final View mView;
    private View mHeader;
    private int mMinHeaderTopOffset;
    public void setHeaderViewMinOffsetTop(int minHeaderTopOffset){
        this.mMinHeaderTopOffset = minHeaderTopOffset;
    }
    public void setHeaderView(View header){
        mHeader = header;
    }
    public ViewOffsetHelper(View view) {
        mParent = (NestedScrollLayout) view.getParent();
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
           NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) mView.getLayoutParams();
            mLayoutTop = lp.getLayoutTop();
            if(mHeader !=null){
                mHeaderHeight =  mHeader.getMeasuredHeight();
            }
        }
    }
    private void updateOffsets() {
        int offsetDy = mOffsetTop - (mView.getTop() -mLayoutTop);
        Logger.e("offsetDy =  " + offsetDy+ "   mOffsetTop = "+ mOffsetTop + " mLayoutTop = " +mLayoutTop);
        if(mHeader !=null &&mOffsetTop >= mMinHeaderTopOffset ){
            NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) mHeader.getLayoutParams();
            int headerDy = mOffsetTop - (mHeader.getTop() -lp.getLayoutTop());
            ViewCompat.offsetTopAndBottom(mHeader,headerDy);
            dispatchScrollChanged(mHeader,mOffsetTop - offsetDy,mOffsetTop,offsetDy,mMinHeaderTopOffset);
        }
        if(mScrollViews !=null){
            for(View v:mScrollViews){
                ViewCompat.offsetTopAndBottom(v,offsetDy);
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


    public int getTopAndBottomOffset() {
        return mOffsetTop;
    }


    public static final  int ANIMATE_OFFSET_DIPS_PER_SECOND = 300;
    ValueAnimator mOffsetToAnimator;
    public void animateOffsetTo( final int toOffsetTop) {
        if (mOffsetTop == toOffsetTop) {
            if (mOffsetToAnimator != null && mOffsetToAnimator.isRunning()) {
                mOffsetToAnimator.cancel();
            }
            return;
        }

        if (mOffsetToAnimator == null) {
            mOffsetToAnimator = new ValueAnimator();
            mOffsetToAnimator.setInterpolator(new DecelerateInterpolator());
            mOffsetToAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    int value = (int) animator.getAnimatedValue();
                    setTopAndBottomOffset(value);
                }
            });
        } else {
            mOffsetToAnimator.cancel();
        }

        // Set the duration based on the amount of dips we're travelling in
        final float distanceDp = Math.abs(mOffsetTop - toOffsetTop) /
                mView.getResources().getDisplayMetrics().density;
        mOffsetToAnimator.setDuration(Math.round(distanceDp * 1000 / ANIMATE_OFFSET_DIPS_PER_SECOND));

        mOffsetToAnimator.setIntValues(mOffsetTop, toOffsetTop);
        mOffsetToAnimator.start();
    }


    ValueAnimator mValueAnimator;
    ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private boolean isNeedCheckHorizontal=false;
    private void setupAnimators() {
        mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                if (mScroller.computeScrollOffset()) {
                    setTopAndBottomOffset(mScroller.getCurrY());

                } else {
                    stopScroll();
                }
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

    public void fling(int velocityY, final int minY, final int maxY) {
        Log.e("watcher", minY+"minY"+velocityY +"velocityY" + maxY);
        stopScroll();
        if(velocityY < 0){
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
        if(velocityY>0){
            mScroller.fling(0, mOffsetTop, 0, -velocityY, 0, 0, Math.min(minY, maxY),
                    Math.max(minY, maxY));
        }else{
            mScroller.fling(0, mOffsetTop, 0, -velocityY, 0, 0,  Math.min(minY,maxY),
                    Math.max(minY,maxY));
        }
        startAnim();
    }

    public void resetOffsetTop() {
        NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) mView.getLayoutParams();
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
    private int mHeaderHeight;
    private void dispatchScrollChanged(View child, int startOffsetTop, int endOffsetTop, int parentScrollDy, int rangeEnd){
        final NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
//        if(!lp.hasOffsetChangedListener()){
//            return;
//        }
        if(lp.hasScrollFlag()){
//            int rangeStart =lp.getLayoutTop() - lp.topMargin;
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

    }
}
