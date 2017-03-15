package com.ytjojo.viewlib.nestedsrolllayout;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
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

    public void onScrollBy(NestedScrollLayout nestedScrollLayout, V child,
                           int dx, int dy, int[] consumed) {
    }
    public void onStartDrag(NestedScrollLayout nestedScrollLayout, V child,int mInitialTouchX,int mInitialTouchY,boolean acceptedByAnother,Behavior accepteBehavior){}
    public void onStopDrag(NestedScrollLayout nestedScrollLayout) {
    }
    boolean isAcceptedDrag;
    boolean isAcceptedDrag() {
        return isAcceptedDrag;
    }
    public void setCanAcceptedDrag(boolean isAcceptedDrag) {
        this.isAcceptedDrag = isAcceptedDrag;
    }
    public boolean onFling(NestedScrollLayout nestedScrollLayout, V child,
                           float velocityX, float velocityY) {
        return false;
    }
    public boolean onNestedPreFling(NestedScrollLayout parent, V child ,View directTargetChild, View target,
                                    float velocityX, float velocityY) {
        return false;
    }
    public boolean onLayoutChild(NestedScrollLayout parent, V child, int layoutDirection){
        return false;
    }
    public boolean onMeasureChild(NestedScrollLayout parent, V child, int parentWidthMeasureSpec, int widthUsed,
                                  int parentHeightMeasureSpec, int heightUsed){
        return false;
    }
    public boolean layoutDependsOn(NestedScrollLayout parent, V child, View dependency) {
        return false;
    }
    public void onAllChildLayouted(NestedScrollLayout parent,V child){

    }
    public boolean onDependentViewChanged(NestedScrollLayout parent, V child, View dependency) {
        return false;
    }
    @ColorInt
    public int getScrimColor(NestedScrollLayout parent, V child) {
        return Color.BLACK;
    }
    @FloatRange(from = 0, to = 1)
    public float getScrimOpacity(NestedScrollLayout parent, V child) {
        return 0.f;
    }

    public boolean blocksInteractionBelow(NestedScrollLayout parent, V child,int mInitialTouchX, int mInitialTouchY) {
        return getScrimOpacity(parent, child) > 0.f;
    }

}
