package com.ytjojo.viewlib.nestedsrolllayout;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;

import com.ytjojo.viewlib.nestedsrolllayout.NestedScrollLayout.LayoutParams;

import java.util.ArrayList;

import static android.view.View.GONE;

/**
 * Created by Administrator on 2017/1/7 0007.
 */
public class ScrollViewBehavior <V extends View> extends Behavior<V> {

    public static final String sBehaviorName="ScrollViewBehavior";
    ArrayList<View> mNestedScrollConsumedViews;
    ViewOffsetHelper mViewOffsetHelper;
    private int mScrollviewBehaviorIndex;
    private int mScrollviewBehaviorsCount;
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
    public void onAllChildLayouted(NestedScrollLayout nestedScrollLayout, V child) {
        calculateScrollRange(nestedScrollLayout,child);
    }

    public void calculateScrollRange(NestedScrollLayout nestedScrollLayout, V v){
        initValue(nestedScrollLayout,v);
        mMinScrollY = mDownScrollRange;
        mMaxScrollY = mUpScrollRange;

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
        mViewOffsetHelper.stopScroll();
        mViewOffsetHelper.resetOffsetTop();

    }
    private void initValue(NestedScrollLayout nestedScrollLayout,V v){
        if(mNestedScrollConsumedViews ==null){
            mNestedScrollConsumedViews = new ArrayList<>();
        }
        mNestedScrollConsumedViews.clear();
        mViewOffsetHelper = ViewOffsetHelper.getViewOffsetHelper(v);
        LayoutParams attachedLp = (LayoutParams) v.getLayoutParams();
        mUpPreScrollRange = getViewRangeEnd(nestedScrollLayout,v);
        mUpScrollRange = mUpPreScrollRange;
        mDownPreScrollRange =mUpPreScrollRange;
        mDownScrollRange = mUpPreScrollRange;

        final int childCount = nestedScrollLayout.getChildCount();
        View scrollHeader = null;
        ArrayList<View> hasScrollViewBehaviorViews = new ArrayList<>();
        View lastScrollView = null;
        for(int i=0;i <childCount;i ++){
            final View child = nestedScrollLayout.getChildAt(i);
            if(child.getVisibility() ==GONE){
                continue;
            }
            final LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if(viewBehavior !=null && viewBehavior instanceof ScrollViewBehavior){
                hasScrollViewBehaviorViews.add(child);
            }
            if(lp.isControlViewByBehavior(sBehaviorName)){
                ViewOffsetHelper.setViewOffsetHelper(child,mViewOffsetHelper);
                if(scrollHeader ==null && hasScrollViewBehaviorViews.isEmpty()){
                    scrollHeader = child;
                }
                if(child!=scrollHeader){
                    mViewOffsetHelper.addScrollViews(child);
                    mNestedScrollConsumedViews.add(0,child);
                }
                lastScrollView = child;
            }
        }

        mScrollviewBehaviorIndex= hasScrollViewBehaviorViews.indexOf(v);
        mScrollviewBehaviorsCount = hasScrollViewBehaviorViews.size();
        if(mScrollviewBehaviorIndex <= hasScrollViewBehaviorViews.size() -2){
            mUpScrollRange = getViewRangeEnd(nestedScrollLayout, hasScrollViewBehaviorViews.get(mScrollviewBehaviorIndex+1));
        }
        if(mScrollviewBehaviorIndex >0){
            mDownScrollRange = getViewRangeEnd(nestedScrollLayout, hasScrollViewBehaviorViews.get(mScrollviewBehaviorIndex-1));
        }
        if(mScrollviewBehaviorIndex == hasScrollViewBehaviorViews.size()-1 &&lastScrollView !=v &&lastScrollView !=null){
            mUpScrollRange = getViewRangeEnd(nestedScrollLayout,lastScrollView);
        }

        if(mScrollviewBehaviorIndex ==0 &&scrollHeader !=null){
            final WindowInsetsCompat lastInsets= nestedScrollLayout.getLastInsets();
            final boolean applyInsets=  lastInsets != null && ViewCompat.getFitsSystemWindows(nestedScrollLayout)
                    && !ViewCompat.getFitsSystemWindows(scrollHeader);
            LayoutParams headerLp = (LayoutParams) scrollHeader.getLayoutParams();
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            int keyValue = lp.getLayoutTop() - lp.topMargin;
            mDownPreScrollRange =keyValue  -headerLp.getDownNestedPreScrollRange()-nestedScrollLayout.getTopInset();
            mUpPreScrollRange = keyValue -headerLp.getUpNestedPreScrollRange()-(!headerLp.isEitUntilCollapsed() &&isApllyInsets(v,nestedScrollLayout)?nestedScrollLayout.getTopInset():0);
//            mDownScrollRange =keyValue - headerLp.getTotalUnResolvedScrollRange()-(applyInsets?nestedScrollLayout.getTopInset():0);
            mDownScrollRange =-nestedScrollLayout.getPaddingTop()+headerLp.getLayoutTop()-headerLp.topMargin-(applyInsets?nestedScrollLayout.getTopInset():0);
            mUpScrollRange = Math.max(mUpPreScrollRange,mUpScrollRange);
            mViewOffsetHelper.setHeaderView(scrollHeader);
            mViewOffsetHelper.setHeaderViewMinOffsetTop(-mUpPreScrollRange);
            mOverScrollDistance = headerLp.mOverScrollDistance;

        }


    }
    public boolean isApllyInsets(View child,NestedScrollLayout nestedScrollLayout){
            return nestedScrollLayout.getLastInsets() != null && ViewCompat.getFitsSystemWindows(nestedScrollLayout)
                && !ViewCompat.getFitsSystemWindows(child);
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
    private int mOverScrollDistance;
    @Override
    public void onNestedPreScroll(NestedScrollLayout nestedScrollLayout, V v,View directTargetChild, View target,
                                  int dx, int dy, int[] consumed){
        if(directTargetChild != v){
            return;
        }
        mNestedPreScrollCalled = true;
        final int startScrollY = -mViewOffsetHelper.getTopAndBottomOffset();
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
//            Logger.e( "parentScrollDy =  "+ parentScrollDy+ " start = "+ startScrollY + " end = "+ endScrollY + " dy = "+dy);
            mViewOffsetHelper.setTopAndBottomOffset(-endScrollY);
            consumed[1] = parentScrollDy;
//            Log.e("onNestedPreScroll",startScrollY+ " dy"+ dy + "parentScrollDy" +parentScrollDy+ "mDownPreScrollRange" +mDownPreScrollRange);
//            Logger.e(v.getTop()+"top  y " +v.getY()+" scrollY "+nestedScrollLayout.getScrollY() +"height" +nestedScrollLayout.getMeasuredHeight());
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

    private boolean isOverScrollMode(int scrollY){
        if(mScrollviewBehaviorIndex==0 &&mOverScrollDistance >0&&scrollY < mDownScrollRange){
            return true;
        }
        return false;
    }
    @Override
    public void onNestedScroll(NestedScrollLayout nestedScrollLayout, V v,View directTargetChild, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,int[] consumed){
        if(directTargetChild != v){
            return;
        }
        final int startScrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        int endScrollY = startScrollY +dyUnconsumed;
        if(dyUnconsumed < 0){
            mSkipNestedPreScrollFling = true;
            if(endScrollY < mDownScrollRange-mOverScrollDistance){
                endScrollY = mDownScrollRange-mOverScrollDistance;
            }

//            if(endScrollY<0){
//                endScrollY =0;
//            }
        }else{
            mSkipNestedPreScrollFling = false;
            if(endScrollY > mUpScrollRange){
                endScrollY = mUpScrollRange;
            }

        }
        final int parentScrollDy = endScrollY - startScrollY;
        mViewOffsetHelper.setTopAndBottomOffset(-endScrollY);
        consumed[1] = parentScrollDy;
    }

    @Override
    public void onStopNestedScroll(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target) {
        if(directTargetChild != child){
            return;
        }
        int scrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        if(mNestedPreScrollCalled&&isOverScrollMode(scrollY)){
            resetVelocityData();
            mViewOffsetHelper.animateOffsetTo(mDownScrollRange);
        }else{

            flingCalculate(nestedScrollLayout,child, target);
        }
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

    public static final int MAX_VELOCITY = 6000;
    @Override
    public boolean onNestedFling(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target, float velocityX, float velocityY, boolean consumed) {
//        Log.e(getClass().getName(), "onNestedFling velocityY ="  + velocityY+ consumed);
        if(Math.abs(velocityY)>MAX_VELOCITY){
            if(velocityY>0){
                velocityY = MAX_VELOCITY;
            }else{
                velocityY = -MAX_VELOCITY;
            }
        }
        if(directTargetChild != child){
            return false;
        }
        if(isOverScrollMode(-mViewOffsetHelper.getTopAndBottomOffset())&&velocityY<0){
            return true;
        }
        int scrollY =- mViewOffsetHelper.getTopAndBottomOffset();
        if(!consumed){
            if( scrollY>mMinScrollY&&scrollY<mMaxScrollY){
                mViewOffsetHelper.fling((int) velocityY,-mMinScrollY,-mMaxScrollY);
                return true;
            }
        }else{
            if(velocityY>0){
                if(scrollY > mUpPreScrollRange){
                    mViewOffsetHelper.fling((int) velocityY,-mMinScrollY,-mMaxScrollY);
                    return true;
                }
            }else{
                if(scrollY <mUpPreScrollRange){
                    mViewOffsetHelper.fling((int) velocityY,-mMinScrollY,-mMaxScrollY);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(NestedScrollLayout nestedScrollLayout, V child,View directTargetChild, View target, float velocityX, float velocityY) {
//        Log.e(getClass().getName(),   "onNestedPreFling  velocityY =" + velocityY);
        if(directTargetChild != child){
            return false;
        }
        if(mWasNestedFlung){
            return true;
        }
        return false;
    }
    private int mMinDragRange;
    private int mMaxDragRange;
    @Override
    public void onStartDrag(NestedScrollLayout nestedScrollLayout,V child,int mInitialTouchX,int mInitialTouchY,boolean acceptedByAnother,Behavior accepteBehavior) {

        if(acceptedByAnother){
            if(accepteBehavior !=null&&accepteBehavior instanceof ScrollViewBehavior){
            }else{
                return;
            }

        }
        mViewOffsetHelper.stopScroll();
        this.setCanAcceptedDrag(true);
        mViewOffsetHelper.resetOffsetTop();
        final int startScrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        if(startScrollY >= mDownScrollRange &&startScrollY < mUpPreScrollRange){
            mMinDragRange =mDownScrollRange;
            mMaxDragRange = mUpPreScrollRange;
        }else if(startScrollY == mUpPreScrollRange){
            mMinDragRange = mMinScrollY;
            mMaxDragRange = mMaxScrollY;
        }else{
            mMinDragRange = mUpPreScrollRange;
            mMaxDragRange = mMaxScrollY;
        }

    }

    @Override
    public void onScrollBy(NestedScrollLayout nestedScrollLayout, V child, int dx, int dy, int[] consumed) {
        final int startScrollY = -mViewOffsetHelper.getTopAndBottomOffset();

        int endScrollY = startScrollY +dy;

        if(dy < 0){
            if(startScrollY <mMinDragRange){
                return;
            }
            if(endScrollY < mMinDragRange){
                endScrollY = mMinDragRange;
            }
        }else{
            if(startScrollY >mMaxDragRange){
                return;
            }
            if(endScrollY > mMaxDragRange){
                endScrollY = mMaxDragRange;
            }
        }
        final int parentScrollDy = endScrollY - startScrollY;
        mViewOffsetHelper.setTopAndBottomOffset(-endScrollY);
        consumed[1] = parentScrollDy;

    }

    @Override
    public boolean onFling(NestedScrollLayout nestedScrollLayout, V child, float velocityX, float velocityY) {
        mViewOffsetHelper.resetOffsetTop();
        int scrollY =- mViewOffsetHelper.getTopAndBottomOffset();
        if( scrollY>mMinDragRange&&scrollY<mMaxDragRange){
            mViewOffsetHelper.fling((int) velocityY,-mMinDragRange,-mMaxDragRange);
            return true;
        }
        return false;
    }

    private void flingCalculate(NestedScrollLayout nestedScrollLayout, V child, View target) {
        if (!mNestedPreScrollCalled ||mSkipNestedPreScrollFling||mTotalDy ==0) {
            mSkipNestedPreScrollFling = false;
            return;
        }
        long mTotalDuration = AnimationUtils.currentAnimationTimeMillis() - mBeginTime;
        int velocityY =(int) (mTotalDy * 1000 / mTotalDuration);
        if(mTotalDy>0){
            if( -mViewOffsetHelper.getTopAndBottomOffset() < mUpPreScrollRange){
                mWasNestedFlung = true;
                mViewOffsetHelper.fling(velocityY,-mDownScrollRange,-mUpPreScrollRange);
            }
        }else{
            if(-mViewOffsetHelper.getTopAndBottomOffset() > mDownPreScrollRange){
                mWasNestedFlung = true;
                mViewOffsetHelper.fling(velocityY,-mDownPreScrollRange,-mUpPreScrollRange);
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
