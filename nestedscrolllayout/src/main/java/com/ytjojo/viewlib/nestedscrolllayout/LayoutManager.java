package com.ytjojo.viewlib.nestedscrolllayout;

import android.graphics.Rect;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.ytjojo.viewlib.nestedscrolllayout.NestedScrollLayout.LayoutParams;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static android.R.attr.layoutDirection;
import static android.view.View.VISIBLE;

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
    HashMap<String,Integer> mBehaviorViewValue = new HashMap<>();
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        prepareChildren();
        int childCount = mNestedScrollLayout.getChildCount();
        final WindowInsetsCompat lastInsets = mNestedScrollLayout.getLastInsets();
        final boolean applyInsets = lastInsets != null && ViewCompat.getFitsSystemWindows(mNestedScrollLayout);
        int horizInsets = 0;
        int vertInsets = 0;
        if (applyInsets) {
            final int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
            final int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
            final int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
            final int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
            horizInsets = lastInsets.getSystemWindowInsetLeft()
                    + lastInsets.getSystemWindowInsetRight();
            vertInsets = lastInsets.getSystemWindowInsetTop()
                    + lastInsets.getSystemWindowInsetBottom();

            int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    widthSize - horizInsets, widthMode);
            int childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    heightSize - vertInsets, heightMode);
        }

        mBehaviorViewValue.clear();
        for (int i = 0; i < childCount; i++) {
            View child = mNestedScrollLayout.getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!TextUtils.isEmpty(lp.mControlBehaviorName)) {
                if(mBehaviorViewValue.get(lp.mControlBehaviorName)==null){
                    if (lp.isEitUntilCollapsed()) {
                       mBehaviorViewValue.put(lp.mControlBehaviorName,ViewCompat.getMinimumHeight(child));
                    }else{
                        mBehaviorViewValue.put(lp.mControlBehaviorName,0);
                    }
                    if(applyInsets && !ViewCompat.getFitsSystemWindows(child)){
                        lp.isApplyInsets = true;
                        lp.mTopInset =  lastInsets.getSystemWindowInsetTop();
                    }else{
                        lp.isApplyInsets = false;
                        lp.mTopInset =  0;
                    }
                }

            }
            final Behavior b = lp.getBehavior();
            if (b != null && b.onMeasureChild(mNestedScrollLayout, child, widthMeasureSpec, 0,
                    heightMeasureSpec, 0)) {
                continue;
            }
            if (lp.height != ViewGroup.MarginLayoutParams.MATCH_PARENT) {
                continue;
            }
            if (lp.mLayoutFlags == LayoutParams.LAYOUT_FLAG_FRAMLAYOUT) {
                if (applyInsets && !ViewCompat.getFitsSystemWindows(child)) {
                    int widthSpec = View.MeasureSpec.makeMeasureSpec(child.getMeasuredWidth() - horizInsets, View.MeasureSpec.EXACTLY);
                    int heightSpec = View.MeasureSpec.makeMeasureSpec(child.getMeasuredHeight() - vertInsets, View.MeasureSpec.EXACTLY);
                    child.measure(widthSpec, heightSpec);
                    lp.isApplyInsets = true;
                    lp.mTopInset =  lastInsets.getSystemWindowInsetTop();
                }else{
                    lp.isApplyInsets = false;
                    lp.mTopInset =  0;
                }

            } else if (lp.mLayoutFlags == LayoutParams.LAYOUT_FLAG_LINEARVERTICAL) {
                int minHeight = mBehaviorViewValue.get(lp.mControlBehaviorName);
                if (applyInsets && !ViewCompat.getFitsSystemWindows(child)) {
                    int widthSpec = View.MeasureSpec.makeMeasureSpec(child.getMeasuredWidth() - horizInsets, View.MeasureSpec.EXACTLY);
                    int heightSpec = View.MeasureSpec.makeMeasureSpec(child.getMeasuredHeight() - minHeight - vertInsets, View.MeasureSpec.EXACTLY);
                    child.measure(widthSpec, heightSpec);
                    lp.isApplyInsets = true;
                    lp.mTopInset =  lastInsets.getSystemWindowInsetTop();
                } else {
                    if (minHeight != 0) {
                        int widthSpec = View.MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), View.MeasureSpec.EXACTLY);
                        int heightSpec = View.MeasureSpec.makeMeasureSpec(child.getMeasuredHeight() - minHeight, View.MeasureSpec.EXACTLY);
                        child.measure(widthSpec, heightSpec);
                    }
                    lp.isApplyInsets = false;
                    lp.mTopInset =  0;
                }

            }
        }
    }
    public List<View> getDependencySortedChildren(){
        return mDependencySortedChildren;
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
        final int childWidthSpace = mWidth - mPaddingLeft - mPaddingRight;
        mBehaviorViewValue.clear();

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
                   Integer childTop =  mBehaviorViewValue.get(lp.mControlBehaviorName);
                    if(childTop ==null){
                        childTop = mPaddingTop;
                    }
                    childTop = layoutChildVertical(child, childTop, childWidthSpace);
                    mBehaviorViewValue.put(lp.mControlBehaviorName,childTop);
                }
            }
            lp.setLayoutTop(child.getTop());

        }
        dispatchOnLayoutedAllchid();
    }

    private void dispatchOnLayoutedAllchid() {
        int childCount = mNestedScrollLayout.getChildCount();
//        final int childWidthSpace = mWidth - mPaddingLeft - mPaddingRight;
        for (int i = 0; i < childCount; i++) {
            View child = mNestedScrollLayout.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                viewBehavior.onAllChildLayouted(mNestedScrollLayout, child);
//                mNestedScrollLayout.setMinScrollY(Math.min(viewBehavior.getMinScrollY(), mNestedScrollLayout.getMinScrollY()));
//                mNestedScrollLayout.setMaxScrollY(Math.max(viewBehavior.getMaxScrollY(), mNestedScrollLayout.getMaxScrollY()));
            }
        }
    }

    private int layoutChildVertical(View child, int childTop, int childWidthSpace) {
        int childLeft = 0;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (childTop == mPaddingTop) {
            if (mNestedScrollLayout.getLastInsets() != null && ViewCompat.getFitsSystemWindows(mNestedScrollLayout)
                    && !ViewCompat.getFitsSystemWindows(child)) {
                childTop += mNestedScrollLayout.getLastInsets().getSystemWindowInsetTop();
            }
            if(lp.mBaseLine !=Integer.MAX_VALUE){
                childTop += lp.mBaseLine;
            }
        }

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
            return childTop + childHeight + lp.bottomMargin;
        }
        return childTop;
    }

    final int MINORGRAVITY = Gravity.NO_GRAVITY & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;

//    private void layoutChildVertical() {
//        int layoutDirection = ViewCompat.getLayoutDirection(mNestedScrollLayout);
//        int childTop = mPaddingTop;
//        int childLeft = 0;
//        int childCount = mNestedScrollLayout.getChildCount();
//        final int childWidthSpace = mWidth - mPaddingLeft - mPaddingRight;
//
//        for (int i = 0; i < childCount; i++) {
//            View child = mNestedScrollLayout.getChildAt(i);
//            LayoutParams lp = (LayoutParams) child.getLayoutParams();
//            if (lp.mLayoutFlags == LayoutParams.LAYOUT_FLAG_LINEARVERTICAL) {
//                if (lp.gravity < 0) {
//                    lp.gravity = Gravity.LEFT;
//                }
//                childTop += lp.topMargin;
//                int childWidth = child.getMeasuredWidth();
//                int childHeight = child.getMeasuredHeight();
//                final int absoluteGravity = GravityCompat.getAbsoluteGravity(
//                        resolveAnchoredChildGravity(lp.gravity), layoutDirection);
//                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
//                    case Gravity.CENTER_HORIZONTAL:
//                        childLeft = mPaddingLeft + ((childWidthSpace - childWidth) / 2)
//                                + lp.leftMargin - lp.rightMargin;
//                        break;
//
//                    case Gravity.RIGHT:
//                        childLeft = mWidth - mPaddingRight - childWidth - lp.rightMargin;
//                        break;
//
//                    case Gravity.LEFT:
//                    default:
//                        childLeft = mPaddingLeft + lp.leftMargin;
//                        break;
//                }
//                setChildFrame(child, childLeft, childTop,
//                        childWidth, childHeight, lp);
//                childTop += childHeight + lp.bottomMargin;
//            }
//        }
//
//    }

    private void setChildFrame(View child, int left, int top, int width, int height, LayoutParams lp) {
        child.layout(left, top, left + width, top + height);
    }

    private static final int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;

    private void layoutChildrenFrameLayout(View child, int left, int top, int right, int bottom,
                                           boolean forceLeftGravity) {
        final WindowInsetsCompat lastInsets = mNestedScrollLayout.getLastInsets();
        final boolean applyInsets = lastInsets != null && ViewCompat.getFitsSystemWindows(mNestedScrollLayout)
                && !ViewCompat.getFitsSystemWindows(child);
        final int parentLeft = mPaddingLeft + (applyInsets ? lastInsets.getSystemWindowInsetLeft() : 0);
        final int parentRight = right - left - mPaddingRight - (applyInsets ? lastInsets.getSystemWindowInsetRight() : 0);
        final int parentTop = mPaddingTop + (applyInsets ? lastInsets.getSystemWindowInsetTop() : 0);
        final int parentBottom = bottom - top - mPaddingBottom - (applyInsets ? lastInsets.getSystemWindowInsetBottom() : 0);
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
        top += lp.topMargin - lp.bottomMargin;
//        top = Math.max(mPaddingTop + lp.topMargin,
//                Math.min(top,
//                        height - mPaddingBottom - childHeight - lp.bottomMargin));

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
        getDescendantRect(anchor, anchorRect, true);
        getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, childRect);

        child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be TOP direct child.
     *
     * @param descendant descendant view to reference
     * @param out        rect to set to the bounds of the descendant view
     */
    void getDescendantRect(View descendant, Rect out, boolean onLayout) {
        out.set(0, 0, descendant.getMeasuredWidth(), descendant.getMeasuredHeight());
        if (onLayout) {
            mNestedScrollLayout.offsetDescendantRectToMyCoords(descendant, out);

        } else {
            offsetRectBetweenParentAndChild(mNestedScrollLayout, descendant, out);

        }
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

    public void dispatchOnDependentViewChanged() {
        final int childCount = mDependencySortedChildren.size();
        for (int i = 0; i < childCount; i++) {
            final View child = mDependencySortedChildren.get(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // Check child views before for anchor
            for (int j = 0; j < i; j++) {
                final View checkChild = mDependencySortedChildren.get(j);

                if (lp.mAnchorDirectChild == checkChild) {
                    int offsetDy = 0;
                    offsetChildToAnchor(child, offsetDy);
                }
            }
            // Update any behavior-dependent views for the change
            for (int j = i + 1; j < childCount; j++) {
                final View checkChild = mDependencySortedChildren.get(j);
                final LayoutParams checkLp = (LayoutParams) checkChild.getLayoutParams();
                final Behavior b = checkLp.getBehavior();

                if (b != null && b.layoutDependsOn(mNestedScrollLayout, checkChild, child)) {
                    final boolean handled = b.onDependentViewChanged(mNestedScrollLayout, checkChild, child);
                    checkLp.setChangedAfterNestedScroll(handled);
                }
            }
        }
    }

    void offsetChildToAnchor(View child, int layoutDirection) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mAnchorView != null) {
            final Rect anchorRect = mTempRect1;
            final Rect childRect = mTempRect2;
            final Rect desiredChildRect = mTempRect3;

            getDescendantRect(lp.mAnchorView, anchorRect, false);
            getChildRect(child, false, childRect);
            getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, desiredChildRect);

            final int dx = desiredChildRect.left - childRect.left;
            final int dy = desiredChildRect.top - childRect.top;

            if (dx != 0) {
                child.offsetLeftAndRight(dx);
            }
            if (dy != 0) {
                child.offsetTopAndBottom(dy);
            }

            if (dx != 0 || dy != 0) {
                // If we have needed to move, make sure to notify the child's Behavior
                final Behavior b = lp.getBehavior();
                if (b != null) {
                    b.onDependentViewChanged(mNestedScrollLayout, child, lp.mAnchorView);
                }
            }
        }
    }

    void getChildRect(View child, boolean transform, Rect out) {
        if (child.isLayoutRequested() || child.getVisibility() == View.GONE) {
            out.set(0, 0, 0, 0);
            return;
        }
        if (transform) {
            getDescendantRect(child, out, false);
        } else {
            out.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        }
    }

    void offsetRectBetweenParentAndChild(ViewGroup parent, View descendant, Rect rect) {
        // already in the same coord system :)
        if (descendant == parent) {
            return;
        }
        ViewParent theParent = descendant.getParent();

        // search and offset up to the parent
        while ((theParent != null)
                && (theParent instanceof View)
                && (theParent != parent)) {

            rect.offset((int) (descendant.getX() - descendant.getScrollX()),
                    (int) (descendant.getY() - descendant.getScrollY()));

            descendant = (View) theParent;
            theParent = descendant.getParent();
        }

        // now that we are up to this view, need to offset one more time
        // to get into our coordinate space
        if (theParent == parent) {
            rect.offset((int) (descendant.getX() - descendant.getScrollX()),
                    (int) (descendant.getY() - descendant.getScrollY()));
        } else {
            throw new IllegalArgumentException("parameter must be a descendant of this view");
        }
    }

    /**
     * Check if a given point in the CoordinatorLayout's coordinates are within the view bounds
     * of the given direct child view.
     *
     * @param child child view to test
     * @param x     X coordinate to test, in the CoordinatorLayout's coordinate system
     * @param y     Y coordinate to test, in the CoordinatorLayout's coordinate system
     * @return true if the point is within the child view's bounds, false otherwise
     */
    public boolean isPointInChildBounds(View child, int x, int y) {
        final Rect r = mTempRect1;
        getDescendantRect(child, r, false);
        return r.contains(x, y);
    }

    /**
     * Check whether two views overlap each other. The views need to be descendants of this
     * {@link NestedScrollLayout} in the view hierarchy.
     *
     * @param first first child view to test
     * @param second second child view to test
     * @return true if both views are visible and overlap each other
     */
    public boolean doViewsOverlap(View first, View second) {
        if (first.getVisibility() == VISIBLE && second.getVisibility() == VISIBLE) {
            final Rect firstRect = mTempRect1;

            getChildRect(first, first.getParent() != this, firstRect);
            final Rect secondRect = mTempRect2;
            getChildRect(second, second.getParent() != this, secondRect);
            try {
                return !(firstRect.left > secondRect.right || firstRect.top > secondRect.bottom
                        || firstRect.right < secondRect.left || firstRect.bottom < secondRect.top);
            } finally {
            }
        }
        return false;
    }
}
