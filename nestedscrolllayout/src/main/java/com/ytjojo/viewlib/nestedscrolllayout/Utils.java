package com.ytjojo.viewlib.nestedscrolllayout;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.AbsListView;

/**
 * Created by Administrator on 2017/5/5 0005.
 */

public class Utils {

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is TOP custom view.
     */
    public static boolean canChildScrollUp(View scrollingTarget) {
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
    public static float dip2px(Context context,float dip){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }



    public static int constrain(int input, int a, int b) {
        int result = input;
        final int min = Math.min(a, b);
        final int max = Math.max(a, b);
        result = result > min ? result : min;
        result = result < max ? result : max;
        return result;
    }

    public static float constrain(float input, float a, float b) {
        float result = input;
        final float min = Math.min(a, b);
        final float max = Math.max(a, b);
        result = result > min ? result : min;
        result = result < max ? result : max;
        return result;
    }

}
