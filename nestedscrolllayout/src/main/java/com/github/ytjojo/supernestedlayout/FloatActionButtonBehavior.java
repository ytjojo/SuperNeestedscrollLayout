package com.github.ytjojo.supernestedlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import static android.view.View.VISIBLE;

/**
 * Created by Administrator on 2017/6/13 0013.
 */

public class FloatActionButtonBehavior extends Behavior<View> {
    private static final boolean AUTO_HIDE_DEFAULT = true;

    private Rect mTmpRect;
    private OnVisibilityChangedListener mInternalAutoHideListener;
    private boolean mAutoHideEnabled = AUTO_HIDE_DEFAULT;

    private View mView;
    public FloatActionButtonBehavior() {
        super();
        mAutoHideEnabled = AUTO_HIDE_DEFAULT;
    }

    public FloatActionButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
//        TypedArray a = context.obtainStyledAttributes(attrs,
//                R.styleable.FloatingActionButton_Behavior_Layout);
//        mAutoHideEnabled = a.getBoolean(
//                R.styleable.FloatingActionButton_Behavior_Layout_behavior_autoHide,
//                AUTO_HIDE_DEFAULT);
//        a.recycle();
    }

    public void setAutoHideEnabled(boolean autoHide) {
        mAutoHideEnabled = autoHide;
    }

    /**
     * Returns whether the associated FloatingActionButton automatically hides when there is
     * not enough space to be displayed.
     *
     * @attr ref android.support.design.R.styleable#FloatingActionButton_Behavior_Layout_behavior_autoHide
     * @return true if enabled
     */
    public boolean isAutoHideEnabled() {
        return mAutoHideEnabled;
    }

    @Override
    public void onAttachedToLayoutParams(@NonNull SuperNestedLayout.LayoutParams lp) {
        if (lp.anchorGravity == Gravity.NO_GRAVITY) {
            // If the developer hasn't set dodgeInsetEdges, lets set it to BOTTOM so that
            // we dodge any Snackbars
            lp.anchorGravity = Gravity.BOTTOM;
        }
    }
    @Override
    public boolean onDependentViewChanged(SuperNestedLayout parent, View child,
                                          View dependency) {
        mView = child;
        if(dependency.getLayoutParams() instanceof SuperNestedLayout.LayoutParams){
            SuperNestedLayout.LayoutParams  lp = (SuperNestedLayout.LayoutParams) dependency.getLayoutParams();
            if (lp.mControlBehaviorName !=null &&  lp.mControlBehaviorName.equals( ScrollViewBehavior.sBehaviorName)) {
                updateFabVisibilityForCollapsingLayout(parent, dependency, child);
            } else if (lp.mControlBehaviorName !=null &&  lp.mControlBehaviorName.equals( BottomSheetBehavior.sBehaviorName)) {
                updateFabVisibilityForCollapsingLayout(parent,dependency, child);
            }
        }
        return false;
    }

    private static boolean isBottomSheet(@NonNull View view) {
        final ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof SuperNestedLayout.LayoutParams) {
            return ((SuperNestedLayout.LayoutParams) lp)
                    .getBehavior() instanceof BottomSheetBehavior;
        }
        return false;
    }


    private boolean shouldUpdateVisibility(View dependency, View child) {
        final SuperNestedLayout.LayoutParams lp =
                (SuperNestedLayout.LayoutParams) child.getLayoutParams();
        if (!mAutoHideEnabled) {
            return false;
        }

        if (lp.getAnchorId() != dependency.getId()) {
            // The anchor ID doesn't match the dependency, so we won't automatically
            // show/hide the FAB
            return false;
        }

        //noinspection RedundantIfStatement
        if ( mInternalSetVisibility != VISIBLE) {
            // The view isn't set to be visible so skip changing its visibility
            return false;
        }

        return true;
    }

    private boolean updateFabVisibilityForCollapsingLayout(SuperNestedLayout parent,
                                                           View collapsingLayout, View child) {
        if (!shouldUpdateVisibility(collapsingLayout, child)) {
            return false;
        }

        if (mTmpRect == null) {
            mTmpRect = new Rect();
        }

        // First, let's get the visible rect of the dependency
//        final Rect rect = mTmpRect;
//        parent.offsetDescendantRectToMyCoords(collapsingLayout, rect);
        ViewOffsetHelper viewOffsetHelper = ViewOffsetHelper.getViewOffsetHelper(collapsingLayout);
        int min =  viewOffsetHelper.getMinHeaderTopOffset();
        int offset = viewOffsetHelper.getTopAndBottomOffset();

//        if (rect.bottom <= collapsingLayout.getMinimumHeightForVisibleOverlappingContent()) {
        if (offset <= min/2) {
            // If the anchor's bottom is below the seam, we'll animate our FAB out
            hide(mInternalAutoHideListener, false);
        } else {
            // Else, we'll animate our FAB back in
            show(mInternalAutoHideListener, false);
        }
        return true;
    }

    private boolean updateFabVisibilityForBottomSheet(View bottomSheet,
                                                      View child) {
        if (!shouldUpdateVisibility(bottomSheet, child)) {
            return false;
        }
        SuperNestedLayout.LayoutParams lp =
                (SuperNestedLayout.LayoutParams) child.getLayoutParams();
        if (bottomSheet.getTop() < child.getHeight() / 2 + lp.topMargin) {
            hide(mInternalAutoHideListener, false);
        } else {
            show(mInternalAutoHideListener, false);
        }
        return true;
    }

    /**
     * Returns the backward compatible elevation of the FloatingActionButton.
     *
     * @return the backward compatible elevation in pixels.
     */
    public float getCompatElevation() {
        return ((SuperNestedLayout.LayoutParams)mView.getLayoutParams()).mDrawingOrderElevation;
    }

    /**
     * Updates the backward compatible elevation of the FloatingActionButton.
     *
     * @param elevation The backward compatible elevation in pixels.
     */
    public void setCompatElevation(float elevation) {
        ((SuperNestedLayout.LayoutParams)mView.getLayoutParams()).mDrawingOrderElevation = elevation;
    }

    static final long PRESSED_ANIM_DELAY = 100;

    static final int ANIM_STATE_NONE = 0;
    static final int ANIM_STATE_HIDING = 1;
    static final int ANIM_STATE_SHOWING = 2;

    int mAnimState = ANIM_STATE_NONE;
    static final int SHOW_HIDE_ANIM_DURATION = 200;
    int mInternalSetVisibility = View.VISIBLE;

    boolean isOrWillBeHidden() {
        if (mView.getVisibility() == View.VISIBLE) {
            // If we currently visible, return true if we're animating to be hidden
            return mAnimState == ANIM_STATE_HIDING;
        } else {
            // Otherwise if we're not visible, return true if we're not animating to be shown
            return mAnimState != ANIM_STATE_SHOWING;
        }
    }
    boolean isOrWillBeShown() {
        if (mView.getVisibility() != View.VISIBLE) {
            // If we not currently visible, return true if we're animating to be shown
            return mAnimState == ANIM_STATE_SHOWING;
        } else {
            // Otherwise if we're visible, return true if we're not animating to be hidden
            return mAnimState != ANIM_STATE_HIDING;
        }
    }

    void hide(@Nullable final OnVisibilityChangedListener listener, final boolean fromUser) {
        if (isOrWillBeHidden()) {
            // We either are or will soon be hidden, skip the call
            return;
        }

        mView.animate().cancel();

        if (shouldAnimateVisibilityChange()) {
            mAnimState = ANIM_STATE_HIDING;

            mView.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(SHOW_HIDE_ANIM_DURATION)
                    .setInterpolator(new FastOutLinearInInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        private boolean mCancelled;

                        @Override
                        public void onAnimationStart(Animator animation) {
                            internalSetVisibility(View.VISIBLE, fromUser);
                            mCancelled = false;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mCancelled = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mAnimState = ANIM_STATE_NONE;

                            if (!mCancelled) {
                                internalSetVisibility(fromUser ? View.GONE : View.INVISIBLE,fromUser);
                                if (listener != null) {
                                    listener.onHidden(mView);
                                }
                            }
                        }
                    });
        } else {
            // If the view isn't laid out, or we're in the editor, don't run the animation
            internalSetVisibility(fromUser ? View.GONE : View.INVISIBLE, fromUser);
            if (listener != null) {
                listener.onHidden(mView);
            }
        }
    }

    void show(@Nullable final OnVisibilityChangedListener listener, final boolean fromUser) {
        if (isOrWillBeShown()) {
            // We either are or will soon be visible, skip the call
            return;
        }

        mView.animate().cancel();

        if (shouldAnimateVisibilityChange()) {
            mAnimState = ANIM_STATE_SHOWING;

            if (mView.getVisibility() != View.VISIBLE) {
                // If the view isn't visible currently, we'll animate it from a single pixel
                mView.setAlpha(0f);
                mView.setScaleY(0f);
                mView.setScaleX(0f);
            }

            mView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(SHOW_HIDE_ANIM_DURATION)
                    .setInterpolator(new LinearOutSlowInInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            internalSetVisibility(View.VISIBLE, fromUser);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mAnimState = ANIM_STATE_NONE;
                            if (listener != null) {
                                listener.onShown(mView);
                            }
                        }
                    });
        } else {
            internalSetVisibility(View.VISIBLE, fromUser);
            mView.setAlpha(1f);
            mView.setScaleY(1f);
            mView.setScaleX(1f);
            if (listener != null) {
                listener.onShown(mView);
            }
        }
    }

    final void internalSetVisibility(int visibility, boolean fromUser) {
       mView.setVisibility(visibility);
        if (fromUser) {
            mInternalSetVisibility = visibility;
        }
    }

    private boolean shouldAnimateVisibilityChange() {
        return ViewCompat.isLaidOut(mView) && !mView.isInEditMode();
    }

    /**
     * Hides the button.
     * <p>This method will animate the button hide if the view has already been laid out.</p>
     */
    public void hide() {
        hide(null);
    }

    /**
     * Hides the button.
     * <p>This method will animate the button hide if the view has already been laid out.</p>
     *
     * @param listener the listener to notify when this view is hidden
     */
    public void hide(@Nullable OnVisibilityChangedListener listener) {
        hide(listener, true);
    }

    /**
     * Shows the button.
     * <p>This method will animate the button show if the view has already been laid out.</p>
     */
    public void show() {
        show(null);
    }

    /**
     * Shows the button.
     * <p>This method will animate the button show if the view has already been laid out.</p>
     *
     * @param listener the listener to notify when this view is shown
     */
    public void show(@Nullable final OnVisibilityChangedListener listener) {
        show(listener, true);
    }
        interface OnVisibilityChangedListener {
	        public void onShown(View v);
	        public void onHidden(View v);
    }
}


