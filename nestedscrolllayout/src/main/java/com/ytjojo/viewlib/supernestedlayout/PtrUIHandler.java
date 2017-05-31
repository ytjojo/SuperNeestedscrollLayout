package com.ytjojo.viewlib.supernestedlayout;

public interface PtrUIHandler {
    void onUIReset(SuperNestedLayout parent);

    void onUIRefreshPrepare(SuperNestedLayout parent);

    void onUIRefreshBegin(SuperNestedLayout parent);

    void onUIRefreshComplete(SuperNestedLayout parent);

    void onUIPositionChange(SuperNestedLayout parent, boolean isUnderTouch, byte status, PtrIndicator indicator);


}