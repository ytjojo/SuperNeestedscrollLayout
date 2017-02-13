package com.ytjojo.viewlib.nestedsrolllayout;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Administrator on 2017/1/8 0008.
 */
public abstract class Behavior<V extends View> {
    protected int mMinScrollY;
    protected int mMaxScrollY;

    public Behavior() {
    }
    public Behavior(Context context, AttributeSet attrs) {
    }
    private boolean hasNestedScrollChild;
    public boolean hasNestedScrollChild(){
        return false;
    }
    public boolean onStartNestedScroll(NestedScrollLayout nestedScrollLayout,
                                       V child, View directTargetChild, View target, int nestedScrollAxes) {

        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
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
    public void calculateScrollRange(NestedScrollLayout nestedScrollLayout, V v){

    }
    public void onNestedScrollAccepted(NestedScrollLayout nestedScrollLayout, V child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
    }

    public void onStopNestedScroll(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target) {
        // Do nothing
    }
    public void onNestedScroll(NestedScrollLayout nestedScrollLayout,V child, View directTargetChild, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed) {
        // Do nothing
    }

    public void onNestedPreScroll(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target,
                                  int dx, int dy, int[] consumed) {
        // Do nothing
    }

    public boolean onNestedFling(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target,
                                 float velocityX, float velocityY, boolean consumed) {
        return false;
    }
    public boolean onFling(NestedScrollLayout nestedScrollLayout, V child,
                                 float velocityX, float velocityY) {
        return false;
    }
    public void onScrollBy(NestedScrollLayout nestedScrollLayout, V child,
                           int dx, int dy, int[] consumed) {
    }

    public boolean onNestedPreFling(NestedScrollLayout nestedScrollLayout, V child ,View directTargetChild, View target,
                                    float velocityX, float velocityY) {
        return false;
    }
    public boolean onLayoutChild(NestedScrollLayout nestedScrollLayout, V child, int layoutDirection){
        return false;
    }
    public boolean onMeasureChild(NestedScrollLayout nestedScrollLayout, V child, int layoutDirection){
        return false;
    }
    public boolean layoutDependsOn(NestedScrollLayout nestedScrollLayout, V child, View dependency) {
        return false;
    }

}
