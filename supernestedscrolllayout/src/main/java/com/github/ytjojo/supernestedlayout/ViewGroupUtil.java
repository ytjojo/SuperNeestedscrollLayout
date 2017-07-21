package com.github.ytjojo.supernestedlayout;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/3/4 0004.
 */

public class ViewGroupUtil {
   public static void getDescendantRect(ViewGroup parent, View descendant, Rect rect) {
       rect.set(0,0,descendant.getMeasuredWidth(),descendant.getMeasuredHeight());
       parent.offsetDescendantRectToMyCoords(descendant, rect);

//        // already in the same coord system :)
//        if (descendant == parent) {
//            return;
//        }
//        ViewParent theParent = descendant.getParent();
//
//        // search and offset up to the parent
//        while ((theParent != null)
//                && (theParent instanceof View)
//                && (theParent != parent)) {
//
//            rect.offset((int) (descendant.getX() - descendant.getScrollX()),
//                    (int) (descendant.getY() - descendant.getScrollY()));
//
//            descendant = (View) theParent;
//            theParent = descendant.getParent();
//        }
//
//        // now that we are up to this view, need to offset one more time
//        // to get into our coordinate space
//        if (theParent == parent) {
//            rect.offset((int) (descendant.getX() - descendant.getScrollX()),
//                    (int) (descendant.getY() - descendant.getScrollY()));
//        } else {
//            throw new IllegalArgumentException("parameter must be a descendant of this view");
//        }
    }
    public static boolean isPointInChild(View parent,View child,int x,int y){
        Rect rect = new Rect();
//        child.getDrawingRect(rect);

        rect.left = parent.getScrollX()+child.getLeft();
        rect.top = parent.getScrollY()+child.getTop();
        rect.right = parent.getScrollX()+child.getRight();
        rect.bottom = parent.getScrollY()+child.getBottom();
        return rect.contains(x,y);
    }

    public static boolean hasChildWithZ(ViewGroup viewGroup) {
        final int mChildrenCount = viewGroup.getChildCount();
        for (int i = 0; i < mChildrenCount; i++) {
            if (viewGroup.getChildAt(i).getZ() != 0) return true;
        }
        return false;
    }
    public static View findTouchedChild(SuperNestedLayout parent,int x,int y){
        ArrayList<View> orderedList = parent.buildTouchDispatchChildList();
        if(orderedList !=null){
            int size = orderedList.size();
            for(int i=size-1;i>=0;i--){
                View child = orderedList.get(i);
                if(child.getVisibility() != View.VISIBLE){
                    continue;
                }
                if(isPointInChild(parent,child,x,y)){
                    return child;
                }
            }
        }else{
            int size = parent.getChildCount();
            for(int i= size-1 ; i >= 0 ; i--){
                View child = parent.getChildAt(i);
                if(child.getVisibility() != View.VISIBLE){
                    continue;
                }
                if(isPointInChild(parent,child,x,y)){
                    return child;
                }
            }
        }
        return null;
    }
}
