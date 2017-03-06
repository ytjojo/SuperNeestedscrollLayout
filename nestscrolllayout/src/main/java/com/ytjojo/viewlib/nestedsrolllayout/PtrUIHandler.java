package com.ytjojo.viewlib.nestedsrolllayout;

public interface PtrUIHandler {
    void onUIReset(NestedScrollLayout parent);

    void onUIRefreshPrepare(NestedScrollLayout parent);

    void onUIRefreshBegin(NestedScrollLayout parent);

    void onUIRefreshComplete(NestedScrollLayout parent);

    void onUIPositionChange(NestedScrollLayout parent, boolean isUnderTouch, byte status, PtrIndicator indicator);
}