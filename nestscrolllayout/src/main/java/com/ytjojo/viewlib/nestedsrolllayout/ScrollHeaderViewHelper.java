package com.ytjojo.viewlib.nestedsrolllayout;

import android.view.View;

/**
 * Created by Administrator on 2017/2/28 0028.
 */

public class ScrollHeaderViewHelper<V extends View> {
    V mHeaderView;
    NestedScrollLayout mParent;
    public ScrollHeaderViewHelper(V header,NestedScrollLayout parent){
        this.mHeaderView = header;
        this.mParent = parent;
    }
}
