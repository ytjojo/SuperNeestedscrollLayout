package com.github.ytjojo.supernestedlayout.demo.behavior;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.util.AttributeSet;
import android.view.View;

import com.github.ytjojo.supernestedlayout.Behavior;
import com.github.ytjojo.supernestedlayout.ScrollViewBehavior;
import com.github.ytjojo.supernestedlayout.SuperNestedLayout;

/**
 * Created by Administrator on 2017/7/13 0013.
 */

public class TranslateUpDownBehavior extends Behavior<View> {


    private boolean isAnimating = false;
    private OnStateChangeListener listener;
    View mDependency;

    public TranslateUpDownBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public void onAllChildLayouted(SuperNestedLayout parent, View child) {
        super.onAllChildLayouted(parent, child);
        child.setTranslationY(-child.getMeasuredHeight()-parent.getTopInset());
    }

    @Override
    public boolean onDependentViewChanged(SuperNestedLayout parent, View child, View dependency) {
        int top = dependency.getTop();
        SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) dependency.getLayoutParams();
        int layoutTop = lp.getLayoutTop();
        int translationY = top - layoutTop;
        if(top <=layoutTop ){
            if(translationY <= -child.getMeasuredHeight()- parent.getTopInset()){
                translationY = -child.getMeasuredHeight()-parent.getTopInset();
            }
            ViewCompat.setTranslationY(child,-child.getMeasuredHeight() - parent.getTopInset() - translationY);
        }


        return super.onDependentViewChanged(parent, child, dependency);
    }

    @Override
    public boolean layoutDependsOn(SuperNestedLayout parent, View child, View dependency) {
        if(mDependency !=null){
            return mDependency == dependency;
        }
        SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) dependency.getLayoutParams();
        if(lp.getBehavior() !=null && lp.getBehavior() instanceof ScrollViewBehavior){
            mDependency = dependency;
            return true;
        }
        return false;

    }

    private class MyViewPropertyAnimatorListener implements ViewPropertyAnimatorListener {

        @Override
        public void onAnimationStart(View view) {
            isAnimating = true;
        }

        @Override
        public void onAnimationEnd(View view) {
            isAnimating = false;
        }

        @Override
        public void onAnimationCancel(View view) {
            isAnimating = false;
        }
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.listener = listener;
    }

    public interface OnStateChangeListener {
        void onChange(boolean isUp);
    }
}
