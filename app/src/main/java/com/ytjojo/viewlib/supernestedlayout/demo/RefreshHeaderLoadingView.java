package com.ytjojo.viewlib.supernestedlayout.demo;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.ytjojo.viewlib.supernestedlayout.SuperNestedLayout;
import com.ytjojo.viewlib.supernestedlayout.PtrIndicator;
import com.ytjojo.viewlib.supernestedlayout.PtrUIHandler;
import com.ytjojo.viewlib.supernestedlayout.RefreshHeaderBehavior;


/**
 * Created by Administrator on 2015/11/30 0030.
 */
public class RefreshHeaderLoadingView extends FrameLayout implements PtrUIHandler {
    ValueAnimator mValueAinimator ;
    
    private ImageView mRotateView;
    public RefreshHeaderLoadingView(Context context) {
        super(context);
        init();
    }
    public RefreshHeaderLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public RefreshHeaderLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RefreshHeaderLoadingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();

    }
    public int dip2px(int dp){
        DisplayMetrics metric =getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metric);

    }
    TextView mTextView;
    private void init(){
        int padding =dip2px(15);
        setBackgroundColor(0x2211ff00);
        this.setPadding(padding,padding,padding,padding);
        ImageView imagView = new ImageView(getContext());
        imagView.setImageResource(R.mipmap.refresh_loading_center);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        this.addView(imagView, layoutParams);
        mRotateView = new ImageView(getContext());
         layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        mRotateView.setImageResource(R.mipmap.refresh_loading_circle);
        this.addView(mRotateView, layoutParams);
        mValueAinimator = createValueAnim();

        mTextView = new TextView(getContext());
        mTextView.setText("------------sdw睡得晚-------------------------");
        mTextView.setTextColor(Color.BLACK);
        addView(mTextView);
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public void onUIReset(SuperNestedLayout frame) {
        if(mValueAinimator ==null){
            mValueAinimator = createValueAnim();
        }
    }

    @Override
    public void onUIRefreshPrepare(SuperNestedLayout frame) {
        mTextView.setText("准备开始下拉了");
    }

    @Override
    public void onUIRefreshBegin(SuperNestedLayout frame) {


        mValueAinimator.start();
        mTextView.setText("刷新动画开始，网络开始请求");
    }

    @Override
    public void onUIRefreshComplete(SuperNestedLayout frame) {
        if(mValueAinimator !=null ||mValueAinimator.isRunning()){
            mValueAinimator.cancel();
            mRotateView.clearAnimation();
        }

        mTextView.setText("网络请求结束，准备滚动回初始位置" +mValueAinimator.isRunning());
    }

    @Override
    public void onUIPositionChange(SuperNestedLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {


        float percent = Math.min(1f, ptrIndicator.getCurrentPercent());
        percent = ptrIndicator.getCurrentPercent() ;
        if (status == RefreshHeaderBehavior.PTR_STATUS_PREPARE) {
            float rotation = (.4f * percent + percent ) * .5f * 360;
            ViewCompat.setRotation(mRotateView,rotation);
//            mTextView.setText("正在下拉，还未触发刷新"+ percent);
        }
    }

    private ValueAnimator createValueAnim(){
//        ViewCompat.setPivotX(mRotateView, 0.5f);
//        ViewCompat.setPivotY(mRotateView, 0.5f);
        ValueAnimator animator = ValueAnimator.ofFloat(ViewCompat.getRotation(mRotateView), ViewCompat.getRotation(mRotateView) +359).setDuration(1200);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (float) valueAnimator.getAnimatedValue();
                ViewCompat.setRotation(mRotateView,value);
            }
        });
        return  animator;
    }

}
