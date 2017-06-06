package com.ytjojo.viewlib.supernestedlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created by Administrator on 2017/6/5 0005.
 */

public class BottomQuickReturnBehavior<V extends View> extends Behavior<V> {


    private int mScrollDy;
    private  int mTatalOffset;
    public BottomQuickReturnBehavior() {
        super();
    }
    public BottomQuickReturnBehavior(Context context) {
        this(context,null);
    }
    public BottomQuickReturnBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    public boolean onLayoutChild(SuperNestedLayout parent, V child, int layoutDirection) {
        SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) child.getLayoutParams();
        int left = parent.getPaddingLeft() + lp.leftMargin;
        int top = parent.getMeasuredHeight();
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        mTatalOffset = child.getMeasuredHeight()+parent.getPaddingBottom()+lp.bottomMargin;
        return true;
    }

    @Override
    public boolean onStartNestedScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        return true;
    }

    @Override
    public void onNestedScrollAccepted(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        if (isRunning()) {
            mValueAnimator.cancel();
        }
        mScrollDy = 0;
    }

    @Override
    public void onStopNestedScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target) {
        onStop(child);
    }

    @Override
    public void onNestedPreScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, int dx, int dy, int[] consumed) {
        scroll(child,dy);
    }

    @Override
    public void onStartDrag(SuperNestedLayout superNestedLayout, V child, int mInitialTouchX, int mInitialTouchY, boolean acceptedByAnother, Behavior accepteBehavior) {
//        setCanAcceptedDrag(true);
    }

    @Override
    public void onStopDrag(SuperNestedLayout superNestedLayout, V child) {
        onStop(child);
    }

    @Override
    public void onScrollBy(SuperNestedLayout superNestedLayout, V child, int dx, int dy, int[] consumed) {

        scroll(child,dy);
    }
    private void scroll(V child ,int dy){
        if(dy ==0){

        }else if(dy > 0 && mScrollDy>=0){
            mScrollDy +=dy;
        }else if(dy < 0 && mScrollDy<=0){
            mScrollDy +=dy;
        }else{
            mScrollDy = dy;
        }
        int y = (int) ViewCompat.getTranslationY(child);
        y+=dy;
        if(dy <0){
            if(y <- mTatalOffset){
                y = -mTatalOffset;
            }
        }else{
            if(y > 0){
                y = 0;
            }
        }

        if(ViewCompat.getTranslationY(child) != y){
            ViewCompat.setTranslationY(child,y);
        }
    }
    private void onStop(V child){
        final  int y = (int) ViewCompat.getTranslationY(child);
        if(y!=0 && y!=-mTatalOffset){
            if(mScrollDy>=0){
                startValueAnimitor(child,0);
            }else{
                startValueAnimitor(child,-mTatalOffset);
            }
        }
        mScrollDy = 0;
    }


    ValueAnimator mValueAnimator;

    private void startValueAnimitor(final V header, final int finalY) {
        if (isRunning()) {
            mValueAnimator.cancel();
        }
        int y = (int) ViewCompat.getTranslationY(header);

        if (y == finalY) {
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
                ViewCompat.setTranslationY(header, value);

            }
        });
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

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

    public boolean isRunning() {
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

}
