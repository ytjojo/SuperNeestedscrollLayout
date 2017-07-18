package com.github.ytjojo.supernestedlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.app.AppCompatDialog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

/**
 * Base class for {@link android.app.Dialog}s styled as a bottom sheet.
 */
public class BottomSheetDialog extends AppCompatDialog {

    private BottomSheetBehavior<FrameLayout> mBehavior;
    private View mBottomSheet;
    private View mContentView;

    boolean mCancelable = true;
    private boolean mCanceledOnTouchOutside = true;
    private boolean mCanceledOnTouchOutsideSet;

    public BottomSheetDialog(@NonNull Context context) {
        this(context, 0);
    }

    public BottomSheetDialog(@NonNull Context context, @StyleRes int theme) {
        super(context, getThemeResId(context, theme));
        // We hide the title bar for any style configuration. Otherwise, there will be a gap
        // above the bottom sheet when it is expanded.
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    protected BottomSheetDialog(@NonNull Context context, boolean cancelable,
            OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        mCancelable = cancelable;
    }


    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        super.setContentView(wrapInBottomSheet(layoutResId, null, null));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if(mAnimDismiss){
            getWindow().setWindowAnimations(0);
        }

    }

    @Override
    public void setContentView(View view) {
        super.setContentView(wrapInBottomSheet(0, view, null));
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(wrapInBottomSheet(0, view, params));
    }

    @Override
    public void setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);
        if (mCancelable != cancelable) {
            mCancelable = cancelable;
            if (mBehavior != null) {
                mBehavior.setHideable(cancelable);
            }
        }
    }
    int mInitState = -1;
    @Override
    protected void onStart() {
        super.onStart();
        if (mBehavior != null) {
            if(mAnimDismiss){
                mInitState = mBehavior.getState();
                if(!mBehavior.getHideable()){
                    mBehavior.setHideable(true);
                }
                mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                mBottomSheet.post(new Runnable() {
                    @Override
                    public void run() {
                        mBehavior.setState(mInitState);
                        mBehavior.setBottomSheetCallback(mBottomSheetCallback);
                        if(mCancelable!=mBehavior.getHideable()){
                            mBehavior.setHideable(mCancelable);
                        }
                    }
                });
            }else{
                mBehavior.setBottomSheetCallback(mBottomSheetCallback);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(mAnimDismiss){
            mBehavior.setHideable(true);
            mBehavior.setStatePost(BottomSheetBehavior.STATE_HIDDEN);
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
        if (cancel && !mCancelable) {
            mCancelable = true;
        }
        mCanceledOnTouchOutside = cancel;
        mCanceledOnTouchOutsideSet = true;
    }

    private View wrapInBottomSheet(int layoutResId, View view, ViewGroup.LayoutParams params) {

        final ViewGroup content;
        if (layoutResId != 0 && view == null) {
            content = (ViewGroup) View.inflate(getContext(),
                    layoutResId, null);
        }else{
            content = (ViewGroup) view;
        }
        SuperNestedLayout superNestedLayout =null;
        if(content instanceof SuperNestedLayout){
            superNestedLayout = (SuperNestedLayout) content;
        }else{
            int childCount = content.getChildCount();
            for (int i = 0; i <  childCount; i++) {
                View child =  content.getChildAt(i);
                if(child instanceof SuperNestedLayout){
                    superNestedLayout = (SuperNestedLayout) child;
                    break;
                }
            }
        }
        if(superNestedLayout ==null){
            throw new IllegalArgumentException("can't find SuperNestedLayout");
        }
        int childCount = superNestedLayout.getChildCount();
        View bottomSheet = null;
        for (int i = 0; i < childCount; i++) {
            View child =  content.getChildAt(i);
            child.setClickable(true);
            if(child.getVisibility()==View.GONE){
                continue;
            }
            SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) child.getLayoutParams();
            if(lp.mBehavior!=null && lp.mBehavior instanceof BottomSheetBehavior){
                mBehavior = (BottomSheetBehavior<FrameLayout>) lp.mBehavior;
                bottomSheet = child;
                mBottomSheet = bottomSheet;
            }

        }
        if(bottomSheet ==null){
            throw new IllegalArgumentException("can't find BottomSheetBehavior in children's LayoutParams");
        }
        mCancelable = mBehavior.getHideable();
//        mBehavior.setHideable(mCancelable);

        // Handle accessibility events
        ViewCompat.setAccessibilityDelegate(bottomSheet, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host,
                    AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                if (mCancelable) {
                    info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS);
                    info.setDismissable(true);
                } else {
                    info.setDismissable(false);
                }
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS && mCancelable) {
                    cancel();
                    return true;
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
        // We treat the SuperNestedLayout as outside the dialog though it is technically inside
        superNestedLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCancelable && isShowing() && shouldWindowCloseOnTouchOutside()) {
                    if(mAnimDismiss){
                        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    }else{
                        cancel();
                    }
                }
            }
        });
        mContentView = content;
        return content;
    }
    boolean mAnimDismiss = true;
    public void setAnimDismiss(boolean animDismiss){
        this.mAnimDismiss = animDismiss;
    }

    boolean shouldWindowCloseOnTouchOutside() {
        if (!mCanceledOnTouchOutsideSet) {
            if (Build.VERSION.SDK_INT < 11) {
                mCanceledOnTouchOutside = true;
            } else {
                TypedArray a = getContext().obtainStyledAttributes(
                        new int[]{android.R.attr.windowCloseOnTouchOutside});
                mCanceledOnTouchOutside = a.getBoolean(0, true);
                a.recycle();
            }
            mCanceledOnTouchOutsideSet = true;
        }
        return mCanceledOnTouchOutside;
    }

    private static int getThemeResId(Context context, int themeId) {
        if (themeId == 0) {
            // If the provided theme is 0, then retrieve the dialogTheme from our theme
            TypedValue outValue = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    R.attr.SuperbottomSheetDialogTheme, outValue, true)) {
                themeId = outValue.resourceId;
            } else {
                // bottomSheetDialogTheme is not provided; we default to our light theme
                themeId = R.style.Theme_Light_BottomSheetDialog;
            }
        }
        return themeId;
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetCallback
            = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, View header, int newState) {
            if(mContentView.isLayoutRequested() ){
                return;
            }
            if (mBehavior.isHiddenState()) {
                cancel();
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, View header, int slideOffset, int minOffset, int maxOffset) {

        }
    };
    public BottomSheetBehavior getBehavior(){
        return mBehavior;
    }

}