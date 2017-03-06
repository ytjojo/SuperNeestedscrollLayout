package com.ytjojo.viewlib.nestedsrolllayout;

import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.view.View;

public class InsetsHelperLollipop{

    public static void setupForWindowInsets(View view, OnApplyWindowInsetsListener insetsListener) {
        if (ViewCompat.getFitsSystemWindows(view)) {
            // First apply the insets listener
            ViewCompat.setOnApplyWindowInsetsListener(view, insetsListener);
            // Now set the sys ui flags to enable us to lay out in the window insets
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

}