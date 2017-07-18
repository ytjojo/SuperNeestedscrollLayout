package com.github.ytjojo.supernestedlayout;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
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
    public static boolean intersection(Path path1, Path path2, Rect rect){
        Region clip = new Region(rect.left,rect.top,rect.right,rect.bottom);
        Region region1 = new Region();
        region1.setPath(path1, clip);
        Region region2 = new Region();
        region2.setPath(path2, clip);

        if (!region1.quickReject(region2) && region1.op(region2, Region.Op.INTERSECT)) {
            // Collision!
            return true;
        }
        return false;
    }
    public static Path op(Path path1, Path path2, Rect rect,Region.Op op){
        Region clip = new Region(rect.left,rect.top,rect.right,rect.bottom);
        Region region1 = new Region();
        region1.setPath(path1, clip);
        Region region2 = new Region();
        region2.setPath(path2, clip);
        Region region = new Region();
        region.op(region1,region2,op);
        return region.getBoundaryPath();
    }

    public static boolean isHeightMatchParentLinearView(View child, SuperNestedLayout.LayoutParams lp){
       if(lp ==null){
           lp = (SuperNestedLayout.LayoutParams) child.getLayoutParams();
       }
       if(lp.getLayoutFlags() == SuperNestedLayout.LayoutParams.LAYOUT_FLAG_LINEARVERTICAL&& lp.height == ViewGroup.MarginLayoutParams.MATCH_PARENT){
           return true;
       }
       return false;
    }

    static boolean objectEquals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
