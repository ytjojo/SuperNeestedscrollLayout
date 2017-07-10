package com.ytjojo.viewlib.supernestedlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;

import static android.support.v4.view.ViewCompat.getTranslationY;

/**
 * Created by Administrator on 2017/6/5 0005.
 */

public class BottomQuickReturnBehavior<V extends View> extends Behavior<V> implements ViewOffsetHelper.OffsetTopChangeCallback{


    private int mScrollDy;
    private  int mTatalOffset;
    Rect mTouchRect = new Rect();
    Rect mTempRect = new Rect();
    V mView;
    ArrayList<View> mToTranslationYViews = new ArrayList<>();
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
    public void onAllChildLayouted(SuperNestedLayout parent, V child) {
        mView = child;
        if(child.getVisibility() != View.VISIBLE){
            return;
        }
        final int childCount = parent.getChildCount();
        mToTranslationYViews.clear();
        ArrayList<View> hasScrollViewBehaviorViews = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            View itemView = parent.getChildAt(i);
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
                    ViewOffsetHelper.getViewOffsetHelper(itemView).addOnOffsetChangedListener(this);
                }
                hasScrollViewBehaviorViews.add(itemView);
            }

        }
        if(mToTranslationYViews.size()>1){
            int touchRectHeight =0;
            int size = mToTranslationYViews.size();
            for (int i = 0; i < size; i++) {
                if(i != 0){
                    View view = mToTranslationYViews.get(i);
                    SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) view.getLayoutParams();
                    touchRectHeight += view.getMeasuredHeight()+lp.topMargin+lp.bottomMargin;
                }
            }
            mTouchRect.set(0,0,parent.getWidth(),touchRectHeight);
        }else{
            mTouchRect.setEmpty();
        }

    }

    @Override
    public boolean onStartNestedScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        return mToTranslationYViews.size() > 0 ;
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
        if(dy < 0){
            float translatlationY = getTranslationY(mToTranslationYViews.get(0));
            if(translatlationY >=0){
                return;
            }
            final float start = translatlationY;
            translatlationY -=dy;
            if(translatlationY >=0){
                translatlationY = 0;
            }
            final float finalY = translatlationY;
            consumed[1] = (int) (start - finalY);
            if(finalY!=start){
                int childCount = mToTranslationYViews.size();
                for (int i = 0; i < childCount; i++) {
                    View itemView = mToTranslationYViews.get(i);
                    ViewCompat.setTranslationY(itemView, finalY);
                }
            }

        }
    }

    @Override
    public void onNestedScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed) {
        if(dyUnconsumed > 0){
            final  int y = (int) getTranslationY(child);
            final int translatingY = (int) getTranslationY(mToTranslationYViews.get(0));
            int finalY = translatingY -dyUnconsumed;
            if(finalY < - mTatalOffset){
                finalY = - mTatalOffset;
            }
            final int startY = translatingY;
            consumed[1] = (startY - finalY);
            if (getTranslationY(child) > finalY) {
                if (isRunning()) {
                    mValueAnimator.cancel();
                }
                ViewCompat.setTranslationY(child, finalY);
            }
            if(finalY != translatingY){
                int count = mToTranslationYViews.size();
                for (int i = 0; i < count; i++) {
                    View itemView = mToTranslationYViews.get(i);
                    ViewCompat.setTranslationY(itemView, finalY);
                }
            }

        }
        scroll(child,dyUnconsumed+dyConsumed);


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
        int y = (int) getTranslationY(child);
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
        float translatlationY = getTranslationY(mToTranslationYViews.get(0));
        if(translatlationY >=0){
            if(getTranslationY(child) != y){
                ViewCompat.setTranslationY(child,y);
            }
        }
        Logger.e(dy+"dy");

    }
    private void onStop(V child){

        int cury = (int) ViewCompat.getTranslationY(mToTranslationYViews.get(0));
        if(cury != -mTatalOffset && cury <0){
            if(mScrollDy> 0){
                startValueAnimitor(child,cury ,-mTatalOffset);
            }else if(mScrollDy < 0){
                startValueAnimitor(child , cury ,0);
            }else{
                if(cury< -mTatalOffset/2){
                    startValueAnimitor(child,cury ,-mTatalOffset);
                }else{
                    startValueAnimitor(child , cury ,0);
                }
            }
        }else{
            final  int y = (int) getTranslationY(child);
            if(y!=0 && y!=-mTatalOffset){
                if(mScrollDy>=0){
                    startValueAnimitor(child, y ,0);
                }else{
                    startValueAnimitor(child, y ,-mTatalOffset);
                }
            }
        }
        mScrollDy = 0;
    }

    @Override
    public void onStartDrag(SuperNestedLayout superNestedLayout, V child, int mInitialTouchX, int mInitialTouchY, boolean acceptedByAnother, Behavior accepteBehavior) {
        View view =  mToTranslationYViews.get(0);
        mTempRect.set(mTouchRect);
        mTempRect.offset(0, (int) view.getY()+view.getMeasuredHeight());
        if(mTempRect.contains(mInitialTouchX,mInitialTouchY)){
            setCanAcceptedDrag(true);
        }
    }

    @Override
    public void onScrollBy(SuperNestedLayout superNestedLayout, V child, int dx, int dy, int[] consumed) {

        if(dy >0){
            onNestedScroll(superNestedLayout,child,null,null,0,0,0,dy,consumed);
        }else{
            scroll(child,dy);
            onNestedPreScroll(superNestedLayout,child,null,null,0,dy,consumed);
        }

    }

    @Override
    public void onStopDrag(SuperNestedLayout superNestedLayout, V child) {
        onStop(child);
    }

    ValueAnimator mValueAnimator;

    private void startValueAnimitor(final V child,int startY, final int finalY) {
        if (isRunning()) {
            mValueAnimator.cancel();
        }
        if(startY == finalY){
            return;
        }
        int duration = 200;
        mValueAnimator = ValueAnimator.ofInt(startY, finalY);
        mValueAnimator.setRepeatCount(0);
        mValueAnimator.setRepeatMode(ValueAnimator.RESTART);
        mValueAnimator.setDuration(duration);
        //fuck you! the default interpolator is AccelerateDecelerateInterpolator
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();

                if(getTranslationY(mToTranslationYViews.get(0)) < 0){
                    for( View v :mToTranslationYViews){
                        ViewCompat.setTranslationY(v, value);
                    }
                   int y = (int) ViewCompat.getTranslationY(child);
                    if(y> -mTatalOffset){
                        if(value<y){
                            ViewCompat.setTranslationY(child, value);
                        }
                    }
                }else{
                    ViewCompat.setTranslationY(child, value);
                }

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

    @Override
    public void onOffsetTopChanged(int offsetTop, int dv) {
        if(mView !=null && mView.getVisibility() == View.VISIBLE){
            scroll(mView,-dv);

        }
    }
}
