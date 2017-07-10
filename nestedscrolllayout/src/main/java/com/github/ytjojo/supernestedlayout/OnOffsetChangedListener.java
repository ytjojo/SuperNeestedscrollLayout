package com.github.ytjojo.supernestedlayout;

import android.view.View;

/**
 * Created by Administrator on 2017/2/5 0005.
 */

public interface OnOffsetChangedListener {
    void onOffsetChanged(View view,float ratio, int dy, int offsetPix, int totalRange, int parentScrollDy);
}
