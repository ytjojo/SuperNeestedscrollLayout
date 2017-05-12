package com.ytjojo.viewlib.nestedscrolllayout;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * Created by Administrator on 2017/3/4 0004.
 */

public class ViewGroupUtil {
   public static void getDescendantRect(ViewGroup parent, View descendant, Rect rect) {
        rect.set(0,0,descendant.getMeasuredWidth(),descendant.getMeasuredHeight());
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
}
