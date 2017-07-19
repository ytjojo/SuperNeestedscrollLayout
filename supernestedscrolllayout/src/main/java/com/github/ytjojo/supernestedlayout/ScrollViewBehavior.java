package com.github.ytjojo.supernestedlayout;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;

import com.github.ytjojo.supernestedlayout.SuperNestedLayout.LayoutParams;

import java.util.ArrayList;

import static android.view.View.GONE;

/**
 * Created by Administrator on 2017/1/7 0007.
 */
public class ScrollViewBehavior <V extends View> extends Behavior<V> {

    public static final String sBehaviorName="ScrollViewBehavior";
    public static int sMaxVelocity;
    public static int sMinVelocity;
    ArrayList<View> mNestedHeaderViews;
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
    public void onAllChildLayouted(SuperNestedLayout superNestedLayout, V child) {
        if(child.getVisibility()==GONE){
            return;
        }
        calculateScrollRange(superNestedLayout,child);
        final int lastOffsetValue = mViewOffsetHelper.getTopAndBottomOffset();
        if(lastOffsetValue != 0){
            int headerOffsetValue = mViewOffsetHelper.getHeaderOffsetTop();
            mViewOffsetHelper.restoreHeaderTop(headerOffsetValue);
            mViewOffsetHelper.restoreTopAndBottomOffset();
        }else {
            if(mSavedPosition != INVALID_POSITION){
                mViewOffsetHelper.restoreHeaderTop(mSavedHeaderOffseTop);
                mViewOffsetHelper.restoreTopAndBottomOffset(mSavedPosition<-mMaxScrollValue?-mMaxScrollValue:mSavedPosition);
                mSavedPosition = INVALID_POSITION;
            }
        }


    }

    public void calculateScrollRange(SuperNestedLayout superNestedLayout, V v){
        initValue(superNestedLayout,v);
        if(sMaxVelocity == 0){
            final ViewConfiguration configuration = ViewConfiguration.get(v.getContext());
            sMaxVelocity = (int) (configuration.getScaledMaximumFlingVelocity() * 0.3f);
        }
        if(sMinVelocity ==0){
            final ViewConfiguration configuration = ViewConfiguration.get(v.getContext());
            sMinVelocity = (int) (configuration.getScaledMinimumFlingVelocity());
        }
        mMinScrollValue = mDownScrollRange;
        mMaxScrollValue = mUpScrollRange;

    }
    public void setIsSnapScroll(boolean isSnapScroll){
        this.isSnapScroll = isSnapScroll;
        if(mViewOffsetHelper !=null){
            mViewOffsetHelper.isSnapScroll = isSnapScroll;
        }
    }

    @Override
    public void onNestedScrollAccepted(SuperNestedLayout superNestedLayout, V v,
                                       View directTargetChild, View target, int nestedScrollAxes){
        super.onNestedScrollAccepted(superNestedLayout,v,directTargetChild,target,nestedScrollAxes);
        mWasNestedFlung = false;
        mLastNestedScrollDy = 0;
        resetVelocityData();
        initValue(superNestedLayout,v);
        mViewOffsetHelper.stopScroll();
        mViewOffsetHelper.resetOffsetTop();

    }
    private void initValue(SuperNestedLayout superNestedLayout, V v){
        if(mNestedHeaderViews ==null){
            mNestedHeaderViews = new ArrayList<>();
        }
        mNestedHeaderViews.clear();
        mViewOffsetHelper = ViewOffsetHelper.getViewOffsetHelper(v);
        mViewOffsetHelper.isSnapScroll = isSnapScroll;
//        LayoutParams attachedLp = (LayoutParams) v.getLayoutParams();
        mUpPreScrollRange = getViewRangeEnd(superNestedLayout,v);
        mUpScrollRange = mUpPreScrollRange;
        mDownPreScrollRange =mUpPreScrollRange;
        mDownScrollRange = mUpPreScrollRange;

        final int childCount = superNestedLayout.getChildCount();
        int headerOffsetValue =0;
        View scrollHeader = null;
        ArrayList<View> hasScrollViewBehaviorViews = new ArrayList<>();
        View lastScrollView = null;
        for(int i=0;i <childCount;i ++){
            final View child = superNestedLayout.getChildAt(i);
            if(child.getVisibility() ==GONE){
                continue;
            }
            final LayoutParams lp = (SuperNestedLayout.LayoutParams) child.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if(viewBehavior !=null && viewBehavior instanceof ScrollViewBehavior){
                hasScrollViewBehaviorViews.add(child);
            }
            if(lp.isControlViewByBehavior(sBehaviorName)){
                ViewOffsetHelper.setViewOffsetHelper(child,mViewOffsetHelper);
                if(scrollHeader ==null && hasScrollViewBehaviorViews.isEmpty()){
                    scrollHeader = child;
                }
                if(child != scrollHeader){
                    mViewOffsetHelper.addScrollViews(child);
                    if(hasScrollViewBehaviorViews.isEmpty()){
                        headerOffsetValue += child.getMeasuredHeight();
                    }

                }
                if(hasScrollViewBehaviorViews.isEmpty()){
                    mNestedHeaderViews.add(child);
                }
                lastScrollView = child;
            }
        }

        mScrollviewBehaviorIndex= hasScrollViewBehaviorViews.indexOf(v);
        mScrollviewBehaviorsCount = hasScrollViewBehaviorViews.size();
        if(mScrollviewBehaviorIndex <= hasScrollViewBehaviorViews.size() -2){
            mUpScrollRange = getViewRangeEnd(superNestedLayout, hasScrollViewBehaviorViews.get(mScrollviewBehaviorIndex+1));
        }
        if(mScrollviewBehaviorIndex >0){
            mDownScrollRange = getViewRangeEnd(superNestedLayout, hasScrollViewBehaviorViews.get(mScrollviewBehaviorIndex-1));
        }
        if(mScrollviewBehaviorIndex == hasScrollViewBehaviorViews.size()-1 &&lastScrollView !=v &&lastScrollView !=null){
            mUpScrollRange = getViewRangeEnd(superNestedLayout,lastScrollView);
        }

        if(mScrollviewBehaviorIndex ==0 &&scrollHeader !=null){
            final WindowInsetsCompat lastInsets= superNestedLayout.getLastInsets();
            final boolean applyInsets=  lastInsets != null && ViewCompat.getFitsSystemWindows(superNestedLayout)
                    && !ViewCompat.getFitsSystemWindows(scrollHeader);
            LayoutParams headerLp = (LayoutParams) scrollHeader.getLayoutParams();
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            int keyValue = lp.getLayoutTop() - lp.topMargin;
            mDownPreScrollRange =keyValue  -headerLp.getDownNestedPreScrollRange()- superNestedLayout.getTopInset();
            int top = superNestedLayout.getTopInset();
            int ss =  ViewCompat.getMinimumHeight(scrollHeader);
            mUpPreScrollRange = keyValue -headerLp.getUpNestedPreScrollRange()-(!headerLp.isEitUntilCollapsed() &&isApllyInsets(v, superNestedLayout)? superNestedLayout.getTopInset():0);
//            mDownScrollRange =keyValue - headerLp.getTotalUnResolvedScrollRange()-(applyInsets?nestedScrollLayout.getTopInset():0);
            mDownScrollRange =-superNestedLayout.getPaddingTop()+headerLp.getLayoutTop()-headerLp.topMargin-(applyInsets? superNestedLayout.getTopInset():0);
            mUpScrollRange = Math.max(mUpPreScrollRange,mUpScrollRange);
            mViewOffsetHelper.setHeaderView(scrollHeader);
            mViewOffsetHelper.setHeaderViewMinOffsetTop(-mUpPreScrollRange);
            mOverScrollDistance = headerLp.mOverScrollDistance;
            mViewOffsetHelper.mHeaderOffsetValue = headerOffsetValue;

        }


    }
    public static boolean isApllyInsets(View child, SuperNestedLayout superNestedLayout){
            return superNestedLayout.getLastInsets() != null && ViewCompat.getFitsSystemWindows(superNestedLayout)
                && !ViewCompat.getFitsSystemWindows(child);
    }
    private int getViewRangeEnd(SuperNestedLayout superNestedLayout, View v){
        LayoutParams lp = (LayoutParams) v.getLayoutParams();
        int parentH = superNestedLayout.getMeasuredHeight() - superNestedLayout.getPaddingTop()- superNestedLayout.getPaddingBottom();
        return lp.getLayoutTop() - lp.topMargin-parentH+v.getMeasuredHeight();
    }
    private int mDownPreScrollRange ;
    private int mDownScrollRange ;
    private int mUpPreScrollRange ;
    private int mUpScrollRange ;
    private boolean mSkipNestedPreScrollFling;
    private boolean mNestedPreScrollCalled = false;
    private int mOverScrollDistance;
    private int mLastNestedScrollDy;
    @Override
    public void onNestedPreScroll(SuperNestedLayout superNestedLayout, V v, View directTargetChild, View target,
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
        mLastNestedScrollDy = dy;




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
    public void onNestedScroll(SuperNestedLayout superNestedLayout, V v, View directTargetChild, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed){
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
    public void onStopNestedScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target) {
        if(directTargetChild != child){
            return;
        }
        int scrollY = -mViewOffsetHelper.getTopAndBottomOffset();
        if(mNestedPreScrollCalled&&isOverScrollMode(scrollY)){
            resetVelocityData();
            mViewOffsetHelper.animateOffsetTo(mDownScrollRange);
        }else{

            preScrollFlingCalculate(superNestedLayout,child, target,true);
        }
        mNestedPreScrollCalled = false;
        mSkipNestedPreScrollFling = false;
    }

    @Override
    public boolean onStartNestedScroll(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        if(directTargetChild != child){
            return false;
        }
//        Logger.e("onStartNestedScroll" +(child == directTargetChild));
        return true;
    }

    @Override
    public boolean onNestedFling(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, float velocityX, float velocityY, boolean consumed) {
//        Log.e(getClass().getName(), "onNestedFling velocityY ="  + velocityY+ consumed);
        if(Math.abs(velocityY)>sMaxVelocity){
            if(velocityY>0){
                velocityY = sMaxVelocity;
            }else{
                velocityY = -sMaxVelocity;
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
            if( scrollY> mMinScrollValue &&scrollY< mMaxScrollValue){
                mViewOffsetHelper.fling((int) velocityY,-mMinScrollValue,-mMaxScrollValue);
                mWasNestedFlung = true;
                return true;
            }
        }else{
            if(velocityY>0){
                if(scrollY > mUpPreScrollRange){
                    mViewOffsetHelper.fling((int) velocityY,-mMinScrollValue,-mMaxScrollValue);
                    mWasNestedFlung = true;
                    return true;
                }
            }else{
                if(!canChildScrollUp(target)){
                    if(scrollY > mMinScrollValue && scrollY < mDownPreScrollRange){
                        mViewOffsetHelper.fling((int) velocityY,-mMinScrollValue,-mMaxScrollValue);
                        mWasNestedFlung = true;
                        return true;
                    }

                }else{
                    if(scrollY < mUpPreScrollRange&& scrollY >mDownPreScrollRange){
                        mViewOffsetHelper.fling((int) velocityY,-mDownPreScrollRange,-mMaxScrollValue);
                        mWasNestedFlung = true;
                        return true;

                    }
                }

            }
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(SuperNestedLayout superNestedLayout, V child, View directTargetChild, View target, float velocityX, float velocityY) {
//        Log.e(getClass().getName(),   "onNestedPreFling  velocityY =" + velocityY);
        if(directTargetChild != child){
            return false;
        }
        if(preScrollFlingCalculate(superNestedLayout,child,target,false)){
            return true;
        }
        return false;
    }
    private int mMinDragRange;
    private int mMaxDragRange;
    @Override
    public void onStartDrag(SuperNestedLayout superNestedLayout, V child, int mInitialTouchX, int mInitialTouchY, boolean acceptedByAnother, Behavior accepteBehavior) {

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
            mMinDragRange = mMinScrollValue;
            mMaxDragRange = mMaxScrollValue;
        }else{
            mMinDragRange = mUpPreScrollRange;
            mMaxDragRange = mMaxScrollValue;
        }

    }

    @Override
    public void onScrollBy(SuperNestedLayout superNestedLayout, V child, int dx, int dy, int[] consumed) {
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
    public boolean onFling(SuperNestedLayout superNestedLayout, V child, float velocityX, float velocityY) {
        mViewOffsetHelper.resetOffsetTop();
        int scrollY =- mViewOffsetHelper.getTopAndBottomOffset();
        if( scrollY>mMinDragRange&&scrollY<mMaxDragRange){
            mViewOffsetHelper.fling((int) velocityY,-mMinDragRange,-mMaxDragRange);
            return true;
        }
        return false;
    }

    private void smoothScroll(){
        if(mLastNestedScrollDy>0){
            if( -mViewOffsetHelper.getTopAndBottomOffset() < mUpPreScrollRange){
                mViewOffsetHelper.animateOffsetTo(-mDownScrollRange);
//                mViewOffsetHelper.fling(velocityY,-mDownScrollRange,-mUpPreScrollRange);
            }
        }else if(mLastNestedScrollDy <0){
            if(-mViewOffsetHelper.getTopAndBottomOffset() > mDownPreScrollRange){
//                mViewOffsetHelper.fling(velocityY,-mDownPreScrollRange,-mUpPreScrollRange);
                mViewOffsetHelper.animateOffsetTo(-mUpPreScrollRange);
            }
        }else{
            int curffset = -mViewOffsetHelper.getTopAndBottomOffset();
            if( curffset > mDownScrollRange && curffset < mUpPreScrollRange){
                if(curffset<(mDownScrollRange + mUpPreScrollRange)/2){
                    mViewOffsetHelper.animateOffsetTo(-mDownScrollRange);
                }else{
                    mViewOffsetHelper.animateOffsetTo(-mDownScrollRange);

                }

//                mViewOffsetHelper.fling(velocityY,-mDownScrollRange,-mUpPreScrollRange);
            }
        }
    }


    private boolean preScrollFlingCalculate(SuperNestedLayout superNestedLayout, V child, View target, boolean isDoFling) {
        if(!mNestedPreScrollCalled){
            return false;
        }
        if(mWasNestedFlung){
            return false;
        }
        if ((!isSnapScroll &&mSkipNestedPreScrollFling)||mTotalDy ==0) {
            return false;
        }
        long mTotalDuration = AnimationUtils.currentAnimationTimeMillis() - mBeginTime;
        int velocityY =(int) (mTotalDy * 1000 / mTotalDuration);
        if(mTotalDy>0){
            if( -mViewOffsetHelper.getTopAndBottomOffset() < mUpPreScrollRange){
                if(isDoFling){
                    mViewOffsetHelper.fling(velocityY,-mDownScrollRange,-mUpPreScrollRange);
                }
                return true;
            }
        }else{
            if(-mViewOffsetHelper.getTopAndBottomOffset() > mDownPreScrollRange){
                if(isDoFling){
                    mViewOffsetHelper.fling(velocityY,-mDownPreScrollRange,-mUpPreScrollRange);
                }
                return true;
            }
        }
        if(isDoFling){
            if(Math.abs(velocityY) >sMaxVelocity){
                velocityY = velocityY>0?sMaxVelocity:-sMaxVelocity;
            }

            superNestedLayout.dispatchNestedFling(0,velocityY,false);
        }
        return false;
    }

    private void checkNeedSaveVelocityData(View scrollTarget,int dy){
//        if(dy > 0 ||(dy<0&&canChildScrollUp(scrollTarget))){
//            saveVelocityYData(dy);
//        }
        if(dy > 0 ||(dy<0)){
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
    boolean isSnapScroll = false;
    private long mBeginTime;
    private long mLastPreScrollTime;
    private int mTotalDy = 0;
    private boolean  mWasNestedFlung;
    private final int INVALID_POSITION = Integer.MAX_VALUE;
    private int mSavedPosition = INVALID_POSITION;
    private int mSavedHeaderOffseTop = INVALID_POSITION;

    @Override
    public Parcelable onSaveInstanceState(SuperNestedLayout parent, V child) {
        return new SavedState(super.onSaveInstanceState(parent, child),mViewOffsetHelper.getTopAndBottomOffset(),mViewOffsetHelper.getHeaderOffsetTop());
    }

    @Override
    public void onRestoreInstanceState(SuperNestedLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        // Intermediate states are restored as collapsed state
        mSavedPosition = ss.scrollPosition;
        mSavedHeaderOffseTop = ss.headerOffseTop;

    }

    protected static class SavedState extends AbsSavedState {
        final int scrollPosition;
        final int headerOffseTop;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            //noinspection ResourceType
            scrollPosition = source.readInt();
            headerOffseTop = source.readInt();
        }

        public SavedState(Parcelable superState, int scrollPosition,int headerOffseTop) {
            super(superState);
            this.scrollPosition = scrollPosition;
            this.headerOffseTop = headerOffseTop;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(scrollPosition);
            out.writeInt(headerOffseTop);
        }

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                });
    }
}
