package com.ytjojo.viewlib.nestedsrolllayout;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.ytjojo.viewlib.nestedsrolllayout.NestedScrollLayout.LayoutParams;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/1/7 0007.
 */
public class ScrollViewBehavior <V extends View> extends Behavior<V> {
    NestedScrollLayout mNestedScrollLayout;
    ArrayList<View> mNestedScrollConsumedViews;

    @Override
    public void calculateScrollRange(NestedScrollLayout nestedScrollLayout, V v,
                                     View directTargetChild, View target){
        final int childCount = mNestedScrollLayout.getChildCount();


        View theLastScrollChild=null;
        View theFirstScrollChild=null;
        boolean found= false;
        for(int i=0;i < childCount;i++){
            final View child = mNestedScrollLayout.getChildAt(i);
            final LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();

            if(lp.getTotalScrollRange()>0){
                if(theFirstScrollChild ==null){
                    theFirstScrollChild = child;
                }
                lp.mScrollYStart = lp.getLayoutTop() -lp.topMargin;
                mMinScrollY = Math.min(mMinScrollY,lp.mScrollYStart);
                lp.mScrollYEnd = lp.mScrollYStart + lp.getTotalScrollRange();
                mMaxScrollY = Math.max(lp.mScrollYStart,mMaxScrollY);
                theLastScrollChild = child;
            }
            if(lp.getBehavior()!=null ){
                if(directTargetChild == child){
                    if(lp.canScrollDownOutOfScreen){
                        LayoutParams firstChildLp = (LayoutParams) theFirstScrollChild.getLayoutParams();
                        mMinScrollY = -mNestedScrollLayout.getMeasuredHeight();
                    }
                    found = true;
                }else{
                    if(!found){
                        mMinScrollY = nestedScrollLayout.getMeasuredHeight();
                        mMaxScrollY = 0;
                        theFirstScrollChild = null;
                    }else{
                    }
                }
            }
        }

        final LayoutParams lp = (NestedScrollLayout.LayoutParams) theLastScrollChild.getLayoutParams();
        mMaxScrollY = mMaxScrollY - mNestedScrollLayout.getMeasuredHeight() +mNestedScrollLayout.getPaddingBottom()+ mNestedScrollLayout.getPaddingBottom()+lp.getTotalScrollRange();
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
        final int childCount = mNestedScrollLayout.getChildCount();
        if(mNestedScrollConsumedViews ==null){
            mNestedScrollConsumedViews = new ArrayList<>();
        }

        mNestedScrollConsumedViews.clear();
        mUpPreScrollRange = Integer.MIN_VALUE;
        mUpScrollRange = Integer.MIN_VALUE;
        mDownPreScrollRange = Integer.MAX_VALUE;
        mDownScrollRange = Integer.MAX_VALUE;
        View topOfTargetView = null;
        boolean found =false;
        for(int i=childCount -1;i >= 0;i --){
            final View child = mNestedScrollLayout.getChildAt(i);
            final LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();

            if(child == directTargetChild){
                found = true;
                if(lp.canScrollDownOutOfScreen){
                    mDownScrollRange = -mNestedScrollLayout.getHeight();
                }
                mUpPreScrollRange =  Math.max(mUpPreScrollRange,lp.getLayoutTop() -lp.topMargin);
                mDownScrollRange = Math.min(mDownScrollRange,lp.getLayoutTop() -lp.topMargin);
                continue;

            }
            if(lp.getBehavior()!=null && child != directTargetChild){
                if(found){
                    mDownScrollRange = Math.min(mDownScrollRange,lp.getLayoutTop() -lp.topMargin);
                    break;
                }
                mDownScrollRange = Integer.MAX_VALUE;
                mUpPreScrollRange = Integer.MIN_VALUE;
                topOfTargetView = null;
            }
            if(lp.getTotalScrollRange()>0){
                if(topOfTargetView == null){
                    topOfTargetView = child;
                }
//                mNestedConsumedViews.add(child);
                mDownScrollRange = Math.min(mDownScrollRange,lp.getLayoutTop() -lp.topMargin);
            }
        }
        if(topOfTargetView !=null){
            final LayoutParams lpTopView= (LayoutParams) topOfTargetView.getLayoutParams();
            if(lpTopView.getDownNestedPreScrollRange()> 0){
                mDownPreScrollRange =lpTopView.getLayoutTop() +lpTopView.bottomMargin + topOfTargetView.getMeasuredHeight() -lpTopView.getDownNestedPreScrollRange();
            }
        }
        boolean began = false;
        for(int i=0;i <childCount;i ++){
            final View child = mNestedScrollLayout.getChildAt(i);
            final LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
            if(child == directTargetChild){
                mUpScrollRange = lp.getLayoutTop() -lp.topMargin;
//                mUpScrollRange = Math.max(mUpScrollRange,lp.getLayoutTop()+child.getMeasuredHeight()+lp.bottomMargin);
                began = true;
                mNestedScrollConsumedViews.add(0,child);
                continue;
            }
            if(began &&lp.getTotalScrollRange()>0){
                mNestedScrollConsumedViews.add(0,child);
                if(lp.getBehavior() != null){
                    LayoutParams directLp =((LayoutParams)directTargetChild.getLayoutParams());
                    mUpScrollRange +=directTargetChild.getMeasuredHeight() + directLp.bottomMargin +directLp.topMargin;
                    break;
                }else{

//                    mNestedConsumedViews.add(child);
//                    mUpScrollRange = Math.max(mUpScrollRange,lp.getLayoutTop()+child.getMeasuredHeight()+lp.bottomMargin);
                    mUpScrollRange += lp.getTotalScrollRange();
                }
            }

        }

    }
    private int mDownPreScrollRange ;
    private int mDownScrollRange ;
    private int mUpPreScrollRange ;
    private int mUpScrollRange ;
    @Override
    public void onNestedPreScroll(NestedScrollLayout nestedScrollLayout, V v, View target,
                                  int dx, int dy, int[] consumed){

        final int startScrollY = mNestedScrollLayout.getScrollY();
        int endScrollY = startScrollY +dy;
        if(dy >0){
            if(endScrollY > mUpPreScrollRange){
                endScrollY = mUpPreScrollRange;
            }
        }else{
            if(endScrollY < mDownPreScrollRange){
                endScrollY = mDownPreScrollRange;
            }
        }
        final int parentScrollDy = endScrollY - startScrollY;
        mNestedScrollLayout.scrollBy(0,parentScrollDy);
        consumed[1] = parentScrollDy;
        dispatchScrollChanged(startScrollY,endScrollY,parentScrollDy);
    }
    private void dispatchScrollChanged(int startScrollY,int endScrollY,int parentScrollDy){
        final int childCount = mNestedScrollConsumedViews.size();
        for(int i=0;i < childCount;i++){
            final View child = mNestedScrollConsumedViews.get(i);
            final LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
            if(!lp.hasOffsetChangedListener()){
                return;
            }
            if(lp.getTotalScrollRange() > 0){
                int rangeStart =lp.getLayoutTop() - lp.topMargin;
                int rangeEnd =rangeStart + lp.getTotalScrollRange();
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
                    rate =(offsetPix+0.f)/lp.getTotalScrollRange();
                    if(rate >1f){
                        rate = 1f;
                    }else if(rate<0){
                        rate = 0;
                    }
                    offsetDy = endScrollYModify -startScrollY;
                    lp.onScrollChanged(rate,offsetDy,offsetPix,lp.getTotalScrollRange(),parentScrollDy);
                }
            }
        }

    }
    @Override
    public void onNestedScroll(NestedScrollLayout nestedScrollLayout, V v, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,int[] consumed){
        final int startScrollY = mNestedScrollLayout.getScrollY();
        int endScrollY = startScrollY +dyUnconsumed;
        if(dyUnconsumed >0){
            if(endScrollY > mUpScrollRange){
                endScrollY = mUpScrollRange;
            }
        }else{
            if(endScrollY < mDownScrollRange){
                endScrollY = mDownScrollRange;
            }
        }
        final int parentScrollDy = endScrollY - startScrollY;
        mNestedScrollLayout.scrollBy(0,parentScrollDy);
        consumed[1] = parentScrollDy;
        dispatchScrollChanged(startScrollY,endScrollY,parentScrollDy);
    }

    @Override
    public void onStopNestedScroll(NestedScrollLayout nestedScrollLayout, V child, View target) {

    }

    private void saveVelocityYData(int dy, int derection) {

        if (derection > 0) {
            if (dy < 0 || mTotalDy < 0) {
                clearVelocityData();
                if (dy > 0) {
                    mLastTime = AnimationUtils.currentAnimationTimeMillis();
                    mTotalDy = 0;
                }
            } else {
                if (mLastTime != 0) {
                    long curTimeMillis = AnimationUtils.currentAnimationTimeMillis();
                    long duration = curTimeMillis - mLastTime;
                    mTotalDuration += duration;
                    mTotalDy += dy;

                } else {
                    mLastTime = AnimationUtils.currentAnimationTimeMillis();
                }

            }
        } else if (derection < 0) {
            if (dy >= 0 || mTotalDy > 0) {
                clearVelocityData();
                if (dy < 0) {
                    mLastTime = AnimationUtils.currentAnimationTimeMillis();
                    mTotalDy = 0;
                }
            } else {
                if (mLastTime != 0) {
                    long curTimeMillis = AnimationUtils.currentAnimationTimeMillis();
                    long duration = curTimeMillis - mLastTime;
                    mTotalDy += dy;
                    mTotalDuration += duration;
                    Log.e(getClass().getName(), " saveVelocityYData dy=" + dy);
                    mLastTime = curTimeMillis;
                } else {
                    mLastTime = AnimationUtils.currentAnimationTimeMillis();
                }
            }
        } else {
            clearVelocityData();
        }
    }

    private void clearVelocityData() {
        mLastTime = 0;
        mTotalDy = 0;
        mTotalDuration = 0;
    }
    boolean isEnterAlways = false;
    boolean isSnap = false;
    boolean mCanScrollWhenChildScroll = false;
    private long mLastTime;
    boolean mSkipNestedPreScroll;
    private int mTotalDy = 0;
    private long mTotalDuration = 0;
    private boolean  mWasNestedFlung;
}
