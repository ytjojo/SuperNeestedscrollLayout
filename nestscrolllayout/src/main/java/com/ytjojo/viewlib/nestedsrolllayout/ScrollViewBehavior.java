package com.ytjojo.viewlib.nestedsrolllayout;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;

import com.orhanobut.logger.Logger;
import com.ytjojo.viewlib.nestedsrolllayout.NestedScrollLayout.LayoutParams;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/1/7 0007.
 */
public class ScrollViewBehavior <V extends View> extends Behavior<V> {
    ArrayList<View> mNestedScrollConsumedViews;


    public ScrollViewBehavior(){
        super();
    }
    public ScrollViewBehavior(Context context, AttributeSet attrs){
        super(context,attrs);
    }

    @Override
    public boolean hasNestedScrollChild() {
        return true;
    }

    @Override
    public void calculateScrollRange(NestedScrollLayout nestedScrollLayout, V v){
        initValue(nestedScrollLayout,v);
        mMinScrollY = mDownScrollRange;
        mMaxScrollY = mUpScrollRange;
        nestedScrollLayout.setMinScrollY(Math.min(mMinScrollY, nestedScrollLayout.getMinScrollY()));
        nestedScrollLayout.setMaxScrollY(Math.max(mMaxScrollY, nestedScrollLayout.getMaxScrollY()));
    }
    public View findDirectChildView(NestedScrollLayout nestedScrollLayout,View target){
        ViewGroup parent = (ViewGroup) target.getParent();
        if(parent == nestedScrollLayout){
            return target;
        }else{
            while(parent != nestedScrollLayout){
               View directTargetChild = parent;
                parent= (ViewGroup) parent.getParent();
                if(parent == nestedScrollLayout){
                    return directTargetChild;
                }
            }
        }
        return null;
    }

    @Override
    public void onNestedScrollAccepted(NestedScrollLayout nestedScrollLayout, V v,
                                       View directTargetChild, View target, int nestedScrollAxes){
        super.onNestedScrollAccepted(nestedScrollLayout,v,directTargetChild,target,nestedScrollAxes);

        mWasNestedFlung = false;
        resetVelocityData();
       initValue(nestedScrollLayout,v);

    }
    private void initValue(NestedScrollLayout nestedScrollLayout,V v){
        if(mNestedScrollConsumedViews ==null){
            mNestedScrollConsumedViews = new ArrayList<>();
        }
        mNestedScrollConsumedViews.clear();
        LayoutParams attachedLp = (LayoutParams) v.getLayoutParams();
        mUpPreScrollRange = getViewRangeEnd(nestedScrollLayout,v);
        mUpScrollRange = mUpPreScrollRange;
        mDownPreScrollRange =mUpPreScrollRange;
        mDownScrollRange = mUpPreScrollRange;
        if(attachedLp.canScrollDownOutOfScreen){
            mDownScrollRange = -nestedScrollLayout.getHeight();
        }
        final int childCount = nestedScrollLayout.getChildCount();
        View scrollHeader = null;
        ArrayList<View> hasBehaviorViews = new ArrayList<>();
        View lastScrollView = null;
        for(int i=0;i <childCount;i ++){
            final View child = nestedScrollLayout.getChildAt(i);
            final LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
            if(lp.getBehavior() !=null){
                hasBehaviorViews.add(child);
            }
            if(lp.getTotalResolvedScrollRange()>0){
                mNestedScrollConsumedViews.add(0,child);
                if(scrollHeader ==null && hasBehaviorViews.isEmpty()){
                    scrollHeader = child;
                }
                lastScrollView = child;
            }
        }
        int index  =hasBehaviorViews.indexOf(v);
        if(index <= hasBehaviorViews.size() -2){
            mUpScrollRange = getViewRangeEnd(nestedScrollLayout,hasBehaviorViews.get(index+1));
        }
        if(index >0){
            mDownScrollRange = getViewRangeEnd(nestedScrollLayout,hasBehaviorViews.get(index-1));
        }
        if(index ==hasBehaviorViews.size()-1 &&lastScrollView !=v &&lastScrollView !=null){
            mUpScrollRange = getViewRangeEnd(nestedScrollLayout,lastScrollView);
        }

        if(index ==0 &&scrollHeader !=null){
            LayoutParams headerLp = (LayoutParams) scrollHeader.getLayoutParams();
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            int keyValue = lp.getLayoutTop() - lp.topMargin;
            mDownPreScrollRange =keyValue  -headerLp.getDownNestedPreScrollRange();
            mUpPreScrollRange = keyValue -headerLp.getUpNestedPreScrollRange();
            mDownScrollRange =keyValue - headerLp.getTotalUnResolvedScrollRange();
            mUpScrollRange = Math.max(mUpPreScrollRange,mUpScrollRange);
        }

    }
    private int getViewRangeEnd(NestedScrollLayout nestedScrollLayout,View v){
        LayoutParams lp = (LayoutParams) v.getLayoutParams();
        int parentH = nestedScrollLayout.getMeasuredHeight() -nestedScrollLayout.getPaddingTop()-nestedScrollLayout.getPaddingBottom();
        return lp.getLayoutTop() - lp.topMargin-parentH+v.getMeasuredHeight();
    }
    private int mDownPreScrollRange ;
    private int mDownScrollRange ;
    private int mUpPreScrollRange ;
    private int mUpScrollRange ;
    private boolean mSkipNestedPreScrollFling;
    private boolean mNestedPreScrollCalled = false;
    @Override
    public void onNestedPreScroll(NestedScrollLayout nestedScrollLayout, V v,View directTargetChild, View target,
                                  int dx, int dy, int[] consumed){
        if(directTargetChild != v){
            return;
        }
        mNestedPreScrollCalled = true;
        final int startScrollY = nestedScrollLayout.getScrollY();
        int endScrollY = startScrollY;
        int parentScrollDy = 0;

        if(dy != 0){
            if(dy >0){
                if(startScrollY < mUpPreScrollRange){
                    endScrollY +=dy;
                    if(endScrollY > mUpPreScrollRange){
                        endScrollY = mUpPreScrollRange;
                    }
                }
            }else if(dy < 0 &&canChildScrollUp(target)){
                if(startScrollY>=mDownPreScrollRange){
                    endScrollY +=dy;
                    if(endScrollY < mDownPreScrollRange){
                        endScrollY = mDownPreScrollRange;
                    }
                }
            }
            parentScrollDy  = endScrollY - startScrollY;
            nestedScrollLayout.scrollBy(0,parentScrollDy);
            consumed[1] = parentScrollDy;
//            Log.e("onNestedPreScroll",startScrollY+ " dy"+ dy + "parentScrollDy" +parentScrollDy+ "mDownPreScrollRange" +mDownPreScrollRange);
            dispatchScrollChanged(startScrollY,endScrollY,parentScrollDy);
            Logger.e(v.getTop()+"top  y " +v.getY()+" scrollY "+nestedScrollLayout.getScrollY() +"height" +nestedScrollLayout.getMeasuredHeight());
        }
        checkNeedSaveVelocityData(target,dy);




    }
    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is TOP custom view.
     */
    public boolean canChildScrollUp(View scrollingTarget) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (scrollingTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) scrollingTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(scrollingTarget, -1) || scrollingTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(scrollingTarget, -1);
        }
    }
    private void dispatchScrollChanged(int startScrollY,int endScrollY,int parentScrollDy){
        final int childCount = mNestedScrollConsumedViews.size();
        for(int i=0;i < childCount;i++){
            final View child = mNestedScrollConsumedViews.get(i);
            final LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
            if(!lp.hasOffsetChangedListener()){
                return;
            }
            if(lp.getTotalResolvedScrollRange() > 0){
                int rangeStart =lp.getLayoutTop() - lp.topMargin;
                int rangeEnd =rangeStart + lp.getTotalResolvedScrollRange();
                if(Math.max(startScrollY,endScrollY)>=rangeStart&& Math.min(startScrollY,endScrollY)<= rangeEnd){
                    int offsetDy = 0;
                    float rate = .0f;
                    int offsetPix = 0;
                    int endScrollYModify = endScrollY;
                    if(endScrollYModify < rangeStart){
                        endScrollYModify = rangeStart ;
                    }
                    if(endScrollYModify > rangeEnd){
                        endScrollYModify = rangeEnd;
                    }
                    offsetPix = endScrollYModify - rangeStart;
                    rate =(offsetPix+0.f)/lp.getTotalResolvedScrollRange();
                    if(rate >1f){
                        rate = 1f;
                    }else if(rate<0){
                        rate = 0;
                    }
                    offsetDy = endScrollYModify -startScrollY;
                    lp.onScrollChanged(rate,offsetDy,offsetPix,lp.getTotalResolvedScrollRange(),parentScrollDy);
                }
            }
        }

    }
    @Override
    public void onNestedScroll(NestedScrollLayout nestedScrollLayout, V v,View directTargetChild, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,int[] consumed){
        if(directTargetChild != v){
            return;
        }
        final int startScrollY = nestedScrollLayout.getScrollY();
        int endScrollY = startScrollY +dyUnconsumed;
        if(dyUnconsumed < 0){
            mSkipNestedPreScrollFling = true;
            if(endScrollY < mDownScrollRange){
                endScrollY = mDownScrollRange;
            }
        }else{
            mSkipNestedPreScrollFling = false;
            if(endScrollY > mUpScrollRange){
                endScrollY = mUpScrollRange;
            }

        }
        final int parentScrollDy = endScrollY - startScrollY;
        nestedScrollLayout.scrollBy(0,parentScrollDy);
        consumed[1] = parentScrollDy;
        dispatchScrollChanged(startScrollY,endScrollY,parentScrollDy);
    }

    @Override
    public void onStopNestedScroll(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target) {
        if(directTargetChild != child){
            return;
        }
        flingCalculate(nestedScrollLayout,child, target);
        mNestedPreScrollCalled = false;
    }

    @Override
    public boolean onStartNestedScroll(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        if(directTargetChild != child){
            return false;
        }
//        Logger.e("onStartNestedScroll" +(child == directTargetChild));
        return true;
    }

    @Override
    public boolean onNestedFling(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target, float velocityX, float velocityY, boolean consumed) {
        Log.e(getClass().getName(), "onNestedFling velocityY ="  + velocityY+ consumed);
        if(directTargetChild != child){
            return false;
        }
        if(!consumed){
            int scrollY = nestedScrollLayout.getScrollY();
            if( scrollY>mMinScrollY&&scrollY<mMaxScrollY){
                nestedScrollLayout.getEventWatcher().fling((int) velocityY,mMinScrollY,mMaxScrollY);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target, float velocityX, float velocityY) {
        Log.e(getClass().getName(),   "onNestedPreFling  velocityY =" + velocityY);
        if(directTargetChild != child){
            return false;
        }
        if(mWasNestedFlung){
            return true;
        }
        return false;
    }

    private void flingCalculate(NestedScrollLayout nestedScrollLayout, V child,View target) {
        if (!mNestedPreScrollCalled ||mSkipNestedPreScrollFling||mTotalDy ==0) {
            mSkipNestedPreScrollFling = false;
            return;
        }
        long mTotalDuration = AnimationUtils.currentAnimationTimeMillis() - mBeginTime;
        int velocityY =(int) (mTotalDy * 1000 / mTotalDuration);
        if(mTotalDy>0){
            if( nestedScrollLayout.getScrollY() < mUpPreScrollRange){
                mWasNestedFlung = true;
                nestedScrollLayout.getEventWatcher().fling(velocityY,mDownScrollRange,mUpPreScrollRange);
            }
        }else{
            if(nestedScrollLayout.getScrollY() > mDownPreScrollRange){
                mWasNestedFlung = true;
                nestedScrollLayout.getEventWatcher().fling(velocityY,mDownPreScrollRange,mUpPreScrollRange);
            }
        }


    }

    private void checkNeedSaveVelocityData(View scrollTarget,int dy){
        if(dy > 0 ||dy<0&&canChildScrollUp(scrollTarget)){
            saveVelocityYData(dy);
        }
    }

    private void saveVelocityYData(int dy) {

        if(dy ==0|| (dy<=0&& mTotalDy<=0) ||(dy >= 0&&mTotalDy>=0) ){
            mTotalDy += dy;
        }else{
            mBeginTime = mLastPreScrollTime;
            mTotalDy =dy;
        }

        mLastPreScrollTime = AnimationUtils.currentAnimationTimeMillis();
//        Log.e(getClass().getName(),"mTotalDy" +mTotalDy+ "mTotalDuration"+mTotalDuration);
    }

    private void resetVelocityData() {
        mTotalDy = 0;
        mBeginTime = AnimationUtils.currentAnimationTimeMillis();
        mLastPreScrollTime = mBeginTime;

    }
    boolean isSnap = false;
    private long mBeginTime;
    private long mLastPreScrollTime;
    private int mTotalDy = 0;
    private boolean  mWasNestedFlung;
}
