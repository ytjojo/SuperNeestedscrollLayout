package com.ytjojo.viewlib.nestedsrolllayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.LinkedList;

import static android.R.attr.y;


/**
 * Created by Administrator on 2017/1/11 0011.
 */

public class HeaderBehavior<V extends View> extends Behavior<V> implements PtrUIHandler {
    public static final byte PTR_STATUS_INIT = 1;
    private byte mStatus = PTR_STATUS_INIT;
    public static final byte PTR_STATUS_PREPARE = 2;
    //    public static final byte PTR_STATUS_SETTING = 3;
    public static final byte PTR_STATUS_LOADING = 4;
    public static final byte PTR_STATUS_COMPLETE = 5;
    ArrayList<View> mToTranslationYViews = new ArrayList<>();
    V mChild;
    RefreshIndicator mRefreshIndicator;
    private boolean canRefresh;

    @Override
    public void onNestedScrollAccepted(NestedScrollLayout nestedScrollLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        super.onNestedScrollAccepted(nestedScrollLayout, child, directTargetChild, target, nestedScrollAxes);
        isIgnore = false;
        if (!canRefresh) {
            isIgnore = true;
            return;
        }
        NestedScrollLayout.LayoutParams headerLp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
        if (mRefreshIndicator == null) {
            mRefreshIndicator = new RefreshIndicator();
            int height = child.getMeasuredHeight() + headerLp.topMargin + headerLp.bottomMargin;
            mRefreshIndicator.setHeaderHeight(height);
        }

        this.onUIRefreshPrepare(nestedScrollLayout);
        mChild = child;
        final int childCount = nestedScrollLayout.getChildCount();
        boolean found = false;
        mToTranslationYViews.clear();
        for (int i = 0; i < childCount; i++) {
            View itemView = nestedScrollLayout.getChildAt(i);
            NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) itemView.getLayoutParams();
            if (directTargetChild == itemView) {
                found = true;
            }
            if (found && lp.getTotalScrollRange() > 0) {
                View showAtBollowView = headerLp.getShowAtBollowView();
                if (showAtBollowView != null) {
                    break;
                }
                mToTranslationYViews.add(itemView);
            }
        }
    }

    @Override
    public void onNestedPreScroll(NestedScrollLayout nestedScrollLayout, V header, View target, int dx, int dy, int[] consumed) {
        if (isRunning() || isIgnore) {
            return;
        }
        if (dy > 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_LOADING)) {
            NestedScrollLayout.LayoutParams headerLp = (NestedScrollLayout.LayoutParams) header.getLayoutParams();
            final int childCount = nestedScrollLayout.getChildCount();
            boolean found = false;
            int consumedY = 0;
            for (int i = 0; i < childCount; i++) {
                View itemView = nestedScrollLayout.getChildAt(i);
                NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) itemView.getLayoutParams();
                if (directTargetChild == itemView) {
                    found = true;
                }
                if (itemView == header) {
                    continue;
                }
                if (found && lp.getTotalScrollRange() > 0) {
                    View showAtBollowView = headerLp.getShowAtBollowView();
                    if (showAtBollowView == itemView) {
                        break;
                    }
                    int y = (int) ViewCompat.getTranslationY(itemView);
                    if (y > 0) {
                        int finalY = y - dy;
                        if (finalY < 0) {
                            finalY = 0;
                        }
                        consumedY = y - finalY;
                        ViewCompat.setTranslationY(itemView, finalY);

                    } else {
                        break;
                    }
                }
            }
            if (mStatus == PTR_STATUS_PREPARE) {
                int y = (int) ViewCompat.getTranslationY(header);
                if (y > 0) {
                    int finalY = y - dy;
                    if (finalY < 0) {
                        finalY = 0;
                    }
                    consumedY = y - finalY;
                    ViewCompat.setTranslationY(header, finalY);
                    mRefreshIndicator.onMove(0, finalY);
                    this.onUIPositionChange(nestedScrollLayout, true, mStatus, mRefreshIndicator);
                }
            }
            consumed[1] = consumedY;
        }
    }

    @Override
    public void onNestedScroll(NestedScrollLayout nestedScrollLayout, V header, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] consumed) {
        if (isRunning() || isIgnore) {
            return;
        }
        if (dyUnconsumed < 0 && (mStatus == PTR_STATUS_PREPARE || mStatus == PTR_STATUS_INIT || mStatus == PTR_STATUS_LOADING)) {
            if (mStatus == PTR_STATUS_INIT) {
                mStatus = PTR_STATUS_PREPARE;
            }
            NestedScrollLayout.LayoutParams headerLp = (NestedScrollLayout.LayoutParams) header.getLayoutParams();
            final int childCount = nestedScrollLayout.getChildCount();
            boolean found = false;
            int consumedY = 0;
            int finalY = 0;
            for (int i = 0; i < childCount; i++) {
                View itemView = nestedScrollLayout.getChildAt(i);
                NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) itemView.getLayoutParams();
                if (directTargetChild == itemView) {
                    found = true;
                }
                if (itemView == header) {
                    continue;
                }
                if (found && lp.getTotalScrollRange() > 0) {
                    View showAtBollowView = headerLp.getShowAtBollowView();
                    if (showAtBollowView == itemView) {
                        break;
                    }
                    int y = (int) ViewCompat.getTranslationY(itemView);
                    final int maxOffsetY = mRefreshIndicator.getMaxOffsetY();
                    if (y < maxOffsetY) {
                        finalY = y - dyUnconsumed;
                        if (finalY >= maxOffsetY) {
                            finalY = maxOffsetY;
                        }
                        consumed[1] = finalY - y;
                        ViewCompat.setTranslationY(itemView, finalY);
                        if (ViewCompat.getTranslationY(header) < finalY) {
                            ViewCompat.setTranslationY(header, finalY);
                        }
                    }

                }
            }

            int y = (int) ViewCompat.getTranslationY(header);
            if (y < finalY) {
                ViewCompat.setTranslationY(header, finalY);
                mRefreshIndicator.onMove(0, finalY);
                this.onUIPositionChange(nestedScrollLayout, true, mStatus, mRefreshIndicator);
            }
        }
    }

    public boolean isRunning() {
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

    boolean isIgnore;

    public void setRefreshComplete() {
        if (mStatus == PTR_STATUS_LOADING) {
            mStatus = PTR_STATUS_COMPLETE;
            startValueAnimitor(mChild, 0, PTR_STATUS_INIT);
            isIgnore = true;
            HeaderBehavior.this.onUIRefreshComplete(mNestedScrollLayout);
        }
    }

    @Override
    public void onStopNestedScroll(NestedScrollLayout nestedScrollLayout, V child, View target) {
        if (isIgnore) {
            isIgnore = false;
            return;
        }
        if (y >= mRefreshIndicator.getOffsetToRefresh() && mStatus == PTR_STATUS_PREPARE) {
            startValueAnimitor(child, mRefreshIndicator.getHeaderHeight(), PTR_STATUS_LOADING);
        } else if (mStatus == PTR_STATUS_LOADING) {
            startValueAnimitor(child, mRefreshIndicator.getHeaderHeight(), PTR_STATUS_LOADING);
        } else {
            startValueAnimitor(child, 0, PTR_STATUS_INIT);
        }
        mRefreshIndicator.onRelease();

    }

    ValueAnimator mValueAnimator;

    private void startValueAnimitor(final V header, final int finalY, final byte endStatus) {
        int y = (int) ViewCompat.getTranslationY(header);
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
                if (mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_LOADING) {
                    int count = mToTranslationYViews.size();
                    mRefreshIndicator.onMove(0, value);
                    for (int i = 0; i < count; i++) {
                        View view = mToTranslationYViews.get(i);
                        if (value < ViewCompat.getTranslationY(view)) {
                            ViewCompat.setTranslationY(view, value);
                        } else {
                            break;
                        }
                    }
                    ViewCompat.setTranslationY(header, value);
                } else if (mStatus == PTR_STATUS_PREPARE) {
                    int count = mToTranslationYViews.size();
                    for (int i = 0; i < count; i++) {
                        View view = mToTranslationYViews.get(i);
                        ViewCompat.setTranslationY(view, value);
                    }
                    ViewCompat.setTranslationY(header, value);
                    mRefreshIndicator.onMove(0, value);
                } else if (mStatus == PTR_STATUS_LOADING) {
                    int count = mToTranslationYViews.size();
                    for (int i = 0; i < count; i++) {
                        View view = mToTranslationYViews.get(i);
                        if (value < ViewCompat.getTranslationY(view)) {
                            ViewCompat.setTranslationY(view, value);
                        } else {
                            break;
                        }
                    }
                    mRefreshIndicator.onMove(0, value);
                    ViewCompat.setTranslationY(header, value);
                }
                HeaderBehavior.this.onUIPositionChange(mNestedScrollLayout, false, mStatus, mRefreshIndicator);
            }
        });
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (endStatus == PTR_STATUS_LOADING&& mStatus !=endStatus) {
                    HeaderBehavior.this.onUIRefreshBegin(mNestedScrollLayout);
                } else if ((endStatus == PTR_STATUS_INIT) && mRefreshIndicator.isInStartPosition()) {
                    HeaderBehavior.this.onUIReset(mNestedScrollLayout);
                }
                mStatus = endStatus;
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


    @Override
    public boolean onNestedPreFling(NestedScrollLayout nestedScrollLayout, V child, View target, float velocityX, float velocityY) {
        return super.onNestedPreFling(nestedScrollLayout, child, target, velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(NestedScrollLayout nestedScrollLayout, V child, View target, float velocityX, float velocityY, boolean consumed) {
        return super.onNestedFling(nestedScrollLayout, child, target, velocityX, velocityY, consumed);
    }

    @Override
    public boolean onLayoutChild(NestedScrollLayout nestedScrollLayout, V child, int layoutDirection) {
        NestedScrollLayout.LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
        int left = nestedScrollLayout.getPaddingLeft() + lp.leftMargin;
        int top = -lp.bottomMargin - child.getMeasuredHeight();
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        return true;
    }

    public void setCanRefresh(boolean canRefresh) {
        this.canRefresh = canRefresh;
    }

    LinkedList<PtrUIHandler> mPtrUIHandlers;

    public void addUIHandler(PtrUIHandler handler) {
        if (mPtrUIHandlers == null) {
            mPtrUIHandlers = new LinkedList<>();
        }
        mPtrUIHandlers.add(handler);
    }

    public void remove(PtrUIHandler handler) {
        if (mPtrUIHandlers != null)
            mPtrUIHandlers.remove(handler);
    }

    @Override
    public void onUIReset(NestedScrollLayout parent) {
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIReset(parent);
        }
    }

    @Override
    public void onUIRefreshPrepare(NestedScrollLayout parent) {
        mRefreshIndicator.onPressDown();
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIRefreshPrepare(parent);
        }
    }

    @Override
    public void onUIRefreshBegin(NestedScrollLayout parent) {
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIRefreshBegin(parent);
        }

    }

    @Override
    public void onUIRefreshComplete(NestedScrollLayout parent) {
        mRefreshIndicator.onUIRefreshComplete();
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIRefreshComplete(parent);
        }
    }

    @Override
    public void onUIPositionChange(NestedScrollLayout parent, boolean isUnderTouch, byte status, RefreshIndicator indicator) {
        if (mPtrUIHandlers == null) {
            return;
        }
        for (PtrUIHandler handler : mPtrUIHandlers) {
            handler.onUIPositionChange(parent, isUnderTouch, status, indicator);
        }
    }

    OnRefreshListener mOnRefreshListener;

    public void setOnRefreshListener(OnRefreshListener l) {
        this.mOnRefreshListener = l;
    }

}
