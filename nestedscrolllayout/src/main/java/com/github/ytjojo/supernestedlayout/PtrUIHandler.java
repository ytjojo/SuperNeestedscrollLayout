package com.github.ytjojo.supernestedlayout;

public interface PtrUIHandler {
    void onUIReset(SuperNestedLayout parent);

    void onUIRefreshPrepare(SuperNestedLayout parent);

    void onUIRefreshBegin(SuperNestedLayout parent);

    void onUIRefreshComplete(SuperNestedLayout parent);

    void onUIPositionChange(SuperNestedLayout parent, boolean isUnderTouch, byte status, PtrIndicator indicator);


}