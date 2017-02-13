package com.ytjojo.viewlib.nestedsrolllayout;

import android.graphics.Rect;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.ytjojo.viewlib.nestedsrolllayout.NestedScrollLayout.LayoutParams;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static android.R.attr.layoutDirection;

/**
 * Created by Administrator on 2017/1/7 0007.
 */
public class LayoutManager {
    private final List<View> mDependencySortedChildren = new ArrayList<View>();
    private final Rect mTempRect1 = new Rect();
    private final Rect mTempRect2 = new Rect();
    private final Rect mTempRect3 = new Rect();
    private int mWidth;
    private int mHeight;
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;

    public LayoutManager(NestedScrollLayout parent) {
        this.mNestedScrollLayout = parent;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        prepareChildren();
        int childCount = mNestedScrollLayout.getChildCount();
        View found = null;
        int minHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View child = mNestedScrollLayout.getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.hasScrollFlag()) {
                if (found == null) {
                    found = child;
                    if (lp.isEitUntilCollapsed()) {
                        minHeight = ViewCompat.getMinimumHeight(child);
                        if (minHeight == 0) {
                            break;
                        }
                        continue;
                    } else {
                        break;
                    }
                }
            }
            if (lp.height == ViewGroup.MarginLayoutParams.MATCH_PARENT) {
                int widthSpec = View.MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(child.getMeasuredHeight()-minHeight,View.MeasureSpec.EXACTLY);
                child.measure(widthSpec,heightSpec);
            }

        }
    }

    private void prepareChildren() {
        mDependencySortedChildren.clear();
        for (int i = 0, count = mNestedScrollLayout.getChildCount(); i < count; i++) {
            final View child = mNestedScrollLayout.getChildAt(i);

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.findAnchorView(mNestedScrollLayout, child);
            lp.setAttactchedView(child);
            mDependencySortedChildren.add(child);
        }
        // We need to use TOP selection sort here to make sure that every item is compared
        // against each other
        selectionSort(mDependencySortedChildren, mLayoutDependencyComparator);
    }

    NestedScrollLayout mNestedScrollLayout;

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mWidth = mNestedScrollLayout.getMeasuredWidth();
        mHeight = mNestedScrollLayout.getMeasuredHeight();
        mPaddingLeft = mNestedScrollLayout.getPaddingLeft();
        mPaddingTop = mNestedScrollLayout.getPaddingTop();
        mPaddingRight = mNestedScrollLayout.getPaddingRight();
        mPaddingBottom = mNestedScrollLayout.getPaddingBottom();
        final int layoutDirection = ViewCompat.getLayoutDirection(mNestedScrollLayout);
        final int childCount = mDependencySortedChildren.size();
        int childTop =mPaddingTop;
        final int childWidthSpace = mWidth - mPaddingLeft - mPaddingRight;
        for (int i = 0; i < childCount; i++) {
            final View child = mDependencySortedChildren.get(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.checkAnchorChanged()) {
                throw new IllegalStateException("An anchor may not be changed after CoordinatorLayout"
                        + " measurement begins before layout is complete.");
            }
            final Behavior behavior = lp.getBehavior();

            if (behavior == null || !behavior.onLayoutChild(mNestedScrollLayout, child, layoutDirection)) {
                if (lp.mAnchorView != null) {
                    layoutChildWithAnchor(child, lp.mAnchorView, layoutDirection);
                } else if (lp.mLayoutFlags == LayoutParams.LAYOUT_FLAG_FRAMLAYOUT) {
                    layoutChildrenFrameLayout(child, left, top, right, bottom, false);
                } else {
                    childTop =layoutChildV(child,childTop,childWidthSpace);
                }
            }

        }
        calculateScrollRange();
    }

    private void calculateScrollRange() {
        int childCount = mNestedScrollLayout.getChildCount();
        final int childWidthSpace = mWidth - mPaddingLeft - mPaddingRight;
        for (int i = 0; i < childCount; i++) {
            View child = mNestedScrollLayout.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.calculateScrollRange(mNestedScrollLayout, child);
                mNestedScrollLayout.setMinScrollY(Math.min(viewBehavior.getMinScrollY(), mNestedScrollLayout.getMinScrollY()));
                mNestedScrollLayout.setMaxScrollY(Math.max(viewBehavior.getMaxScrollY(), mNestedScrollLayout.getMaxScrollY()));
            }
        }
    }

    private int layoutChildV(View child,int childTop,int childWidthSpace){
        int childLeft = 0;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mLayoutFlags == LayoutParams.LAYOUT_FLAG_LINEARVERTICAL) {
            if (lp.gravity < 0) {
                lp.gravity = Gravity.LEFT;
            }
            childTop += lp.topMargin;
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            final int absoluteGravity = GravityCompat.getAbsoluteGravity(
                    resolveAnchoredChildGravity(lp.gravity), layoutDirection);
            switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.CENTER_HORIZONTAL:
                    childLeft = mPaddingLeft + ((childWidthSpace - childWidth) / 2)
                            + lp.leftMargin - lp.rightMargin;
                    break;

                case Gravity.RIGHT:
                    childLeft = mWidth - mPaddingRight - childWidth - lp.rightMargin;
                    break;

                case Gravity.LEFT:
                default:
                    childLeft = mPaddingLeft + lp.leftMargin;
                    break;
            }
            setChildFrame(child, childLeft, childTop,
                    childWidth, childHeight, lp);
            return childTop +childHeight + lp.bottomMargin;
        }
        return childTop;
    }
    final int MINORGRAVITY = Gravity.NO_GRAVITY & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
    private void layoutChildVertical() {
        int layoutDirection = ViewCompat.getLayoutDirection(mNestedScrollLayout);
        int childTop = mPaddingTop;
        int childLeft = 0;
        int childCount = mNestedScrollLayout.getChildCount();
        final int childWidthSpace = mWidth - mPaddingLeft - mPaddingRight;

        for (int i = 0; i < childCount; i++) {
            View child = mNestedScrollLayout.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.mLayoutFlags == LayoutParams.LAYOUT_FLAG_LINEARVERTICAL) {
                if (lp.gravity < 0) {
                    lp.gravity = Gravity.LEFT;
                }
                childTop += lp.topMargin;
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                final int absoluteGravity = GravityCompat.getAbsoluteGravity(
                        resolveAnchoredChildGravity(lp.gravity), layoutDirection);
                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = mPaddingLeft + ((childWidthSpace - childWidth) / 2)
                                + lp.leftMargin - lp.rightMargin;
                        break;

                    case Gravity.RIGHT:
                        childLeft = mWidth - mPaddingRight - childWidth - lp.rightMargin;
                        break;

                    case Gravity.LEFT:
                    default:
                        childLeft = mPaddingLeft + lp.leftMargin;
                        break;
                }
                setChildFrame(child, childLeft, childTop,
                        childWidth, childHeight, lp);
                childTop += childHeight + lp.bottomMargin;
            }
        }

    }

    private void setChildFrame(View child, int left, int top, int width, int height, LayoutParams lp) {
        child.layout(left, top, left + width, top + height);
        lp.setLayoutTop(child.getTop());
    }

    private static final int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;

    private void layoutChildrenFrameLayout(View child, int left, int top, int right, int bottom,
                                           boolean forceLeftGravity) {
        final int parentLeft = mPaddingLeft;
        final int parentRight = right - left - mPaddingRight;

        final int parentTop = mPaddingTop;
        final int parentBottom = bottom - top - mPaddingBottom;
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();

        final int width = child.getMeasuredWidth();
        final int height = child.getMeasuredHeight();

        int childLeft;
        int childTop;

        int gravity = lp.gravity;
        if (gravity == -1) {
            gravity = DEFAULT_CHILD_GRAVITY;
        }

        final int layoutDirection = ViewCompat.getLayoutDirection(mNestedScrollLayout);
        final int absoluteGravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection);
        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                        lp.leftMargin - lp.rightMargin;
                break;
            case Gravity.RIGHT:
                if (!forceLeftGravity) {
                    childLeft = parentRight - width - lp.rightMargin;
                    break;
                }
            case Gravity.LEFT:
            default:
                childLeft = parentLeft + lp.leftMargin;
        }

        switch (verticalGravity) {
            case Gravity.TOP:
                childTop = parentTop + lp.topMargin;
                break;
            case Gravity.CENTER_VERTICAL:
                childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                        lp.topMargin - lp.bottomMargin;
                break;
            case Gravity.BOTTOM:
                childTop = parentBottom - height - lp.bottomMargin;
                break;
            default:
                childTop = parentTop + lp.topMargin;
        }

        child.layout(childLeft, childTop, childLeft + width, childTop + height);
        lp.setLayoutTop(child.getTop());
    }

    /**
     * Calculate the desired child rect relative to an anchor rect, respecting both
     * gravity and anchorGravity.
     *
     * @param child           child view to calculate TOP rect for
     * @param layoutDirection the desired layout direction for the CoordinatorLayout
     * @param anchorRect      rect in CoordinatorLayout coordinates of the anchor view area
     * @param out             rect to set to the output values
     */
    void getDesiredAnchoredChildRect(View child, int layoutDirection, Rect anchorRect, Rect out) {
        final LayoutParams lp = (NestedScrollLayout.LayoutParams) child.getLayoutParams();
        final int absGravity = GravityCompat.getAbsoluteGravity(
                resolveAnchoredChildGravity(lp.gravity), layoutDirection);
        final int absAnchorGravity = GravityCompat.getAbsoluteGravity(
                resolveGravity(lp.anchorGravity),
                layoutDirection);

        final int hgrav = absGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int vgrav = absGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int anchorHgrav = absAnchorGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int anchorVgrav = absAnchorGravity & Gravity.VERTICAL_GRAVITY_MASK;

        final int childWidth = child.getMeasuredWidth();
        final int childHeight = child.getMeasuredHeight();

        int left;
        int top;

        // Align to the anchor. This puts us in an assumed right/bottom child view gravity.
        // If this is not the case we will subtract out the appropriate portion of
        // the child size below.
        switch (anchorHgrav) {
            default:
            case Gravity.LEFT:
                left = anchorRect.left;
                break;
            case Gravity.RIGHT:
                left = anchorRect.right;
                break;
            case Gravity.CENTER_HORIZONTAL:
                left = anchorRect.left + anchorRect.width() / 2;
                break;
        }

        switch (anchorVgrav) {
            default:
            case Gravity.TOP:
                top = anchorRect.top;
                break;
            case Gravity.BOTTOM:
                top = anchorRect.bottom;
                break;
            case Gravity.CENTER_VERTICAL:
                top = anchorRect.top + anchorRect.height() / 2;
                break;
        }

        // Offset by the child view's gravity itself. The above assumed right/bottom gravity.
        switch (hgrav) {
            default:
            case Gravity.LEFT:
                left -= childWidth;
                break;
            case Gravity.RIGHT:
                // Do nothing, we're already in position.
                break;
            case Gravity.CENTER_HORIZONTAL:
                left -= childWidth / 2;
                break;
        }

        switch (vgrav) {
            default:
            case Gravity.TOP:
                top -= childHeight;
                break;
            case Gravity.BOTTOM:
                // Do nothing, we're already in position.
                break;
            case Gravity.CENTER_VERTICAL:
                top -= childHeight / 2;
                break;
        }

        final int width = mWidth;
        final int height = mHeight;


        // Obey margins and padding
        left = Math.max(mPaddingLeft + lp.leftMargin,
                Math.min(left,
                        width - mPaddingRight - childWidth - lp.rightMargin));
        top = Math.max(mPaddingTop + lp.topMargin,
                Math.min(top,
                        height - mPaddingBottom - childHeight - lp.bottomMargin));

        out.set(left, top, left + childWidth, top + childHeight);
    }

    /**
     * CORE ASSUMPTION: anchor has been laid out by the time this is called for TOP given child view.
     *
     * @param child           child to lay out
     * @param anchor          view to anchor child relative to; already laid out.
     * @param layoutDirection ViewCompat constant for layout direction
     */
    private void layoutChildWithAnchor(View child, View anchor, int layoutDirection) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();

        final Rect anchorRect = mTempRect1;
        final Rect childRect = mTempRect2;
        getDescendantRect(anchor, anchorRect);
        getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, childRect);

        child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
        lp.setLayoutTop(child.getTop());
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be TOP direct child.
     *
     * @param descendant descendant view to reference
     * @param out        rect to set to the bounds of the descendant view
     */
    void getDescendantRect(View descendant, Rect out) {
        out.set(0, 0, descendant.getMeasuredWidth(), descendant.getMeasuredHeight());
        mNestedScrollLayout.offsetDescendantRectToMyCoords(descendant, out);
        out.offset(descendant.getScrollX(), descendant.getScrollY());
    }

    /**
     * Return the given gravity value or the default if the passed value is NO_GRAVITY.
     * This should be used for children that are not anchored to another view or TOP keyline.
     */
    private static int resolveGravity(int gravity) {
        return gravity == Gravity.NO_GRAVITY ? GravityCompat.START | Gravity.TOP : gravity;
    }

    /**
     * Return the given gravity value or the default if the passed value is NO_GRAVITY.
     * This should be used for children that are anchored to another view.
     */
    private static int resolveAnchoredChildGravity(int gravity) {
        return gravity == Gravity.NO_GRAVITY ? Gravity.CENTER : gravity;
    }

    final Comparator<View> mLayoutDependencyComparator = new Comparator<View>() {
        @Override
        public int compare(View lhs, View rhs) {
            if (lhs == rhs) {
                return 0;
            } else if (((LayoutParams) lhs.getLayoutParams()).dependsOn(
                    mNestedScrollLayout, lhs, rhs)) {
                return 1;
            } else if (((LayoutParams) rhs.getLayoutParams()).dependsOn(
                    mNestedScrollLayout, rhs, lhs)) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    private static void selectionSort(final List<View> list, final Comparator<View> comparator) {
        if (list == null || list.size() < 2) {
            return;
        }

        final View[] array = new View[list.size()];
        list.toArray(array);
        final int count = array.length;

        for (int i = 0; i < count; i++) {
            int min = i;

            for (int j = i + 1; j < count; j++) {
                if (comparator.compare(array[j], array[min]) < 0) {
                    min = j;
                }
            }

            if (i != min) {
                // We have TOP different min so swap the items
                final View minItem = array[min];
                array[min] = array[i];
                array[i] = minItem;
            }
        }

        // Finally add the array back into the collection
        list.clear();
        for (int i = 0; i < count; i++) {
            list.add(array[i]);
        }
    }

}
