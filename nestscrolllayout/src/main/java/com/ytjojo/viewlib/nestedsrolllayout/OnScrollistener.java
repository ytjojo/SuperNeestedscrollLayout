package com.ytjojo.viewlib.nestedsrolllayout;

public interface OnScrollistener {
        void onScroll(float positionOffset,int dy, int positionOffsetPixels,int offsetRange);
        void onStateChanged(ScrollState state);
    }
