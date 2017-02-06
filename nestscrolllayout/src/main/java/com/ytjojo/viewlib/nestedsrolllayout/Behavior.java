package com.ytjojo.viewlib.nestedsrolllayout;

import android.view.View;

/**
 * Created by Administrator on 2017/1/8 0008.
 */
public abstract class Behavior<V extends View> {
    NestedScrollLayout mNestedScrollLayout;
    protected View directTargetChild;
    protected int mMinScrollY;
    protected int mMaxScrollY;
    public boolean onStartNestedScroll(NestedScrollLayout nestedScrollLayout,
                                       V child, View directTargetChild, View target, int nestedScrollAxes) {

        return false;
    }

    public int getMinScrollY() {
        return mMinScrollY;
    }

    public void setMinScrollY(int minScrollY) {
        mMinScrollY = minScrollY;
    }

    public int getMaxScrollY() {
        return mMaxScrollY;
    }

    public void setMaxScrollY(int maxScrollY) {
        mMaxScrollY = maxScrollY;
    }
    public void calculateScrollRange(NestedScrollLayout nestedScrollLayout, V v,
                                     View directTargetChild, View target){

    }
    public void onNestedScrollAccepted(NestedScrollLayout nestedScrollLayout, V child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        mNestedScrollLayout = nestedScrollLayout;
    }

    public void onStopNestedScroll(NestedScrollLayout nestedScrollLayout, V child, View target) {
        // Do nothing
    }
    public void onNestedScroll(NestedScrollLayout nestedScrollLayout, V child, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed) {
        // Do nothing
    }

    public void onNestedPreScroll(NestedScrollLayout nestedScrollLayout, V child, View target,
                                  int dx, int dy, int[] consumed) {
        // Do nothing
    }

    public boolean onNestedFling(NestedScrollLayout nestedScrollLayout, V child, View target,
                                 float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    public boolean onNestedPreFling(NestedScrollLayout nestedScrollLayout, V child, View target,
                                    float velocityX, float velocityY) {
        return false;
    }
    public boolean onLayoutChild(NestedScrollLayout nestedScrollLayout, V child, int layoutDirection){
        return false;
    }
}
