package com.ytjojo.viewlib.nestedscrolllayout.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ytjojo.viewlib.nestedscrolllayout.NestedScrollLayout;
import com.ytjojo.viewlib.nestedscrolllayout.PtrIndicator;
import com.ytjojo.viewlib.nestedscrolllayout.PtrUIHandler;
import com.ytjojo.viewlib.nestedscrolllayout.R;
import com.ytjojo.viewlib.nestedscrolllayout.RefreshHeaderBehavior;
import com.ytjojo.viewlib.nestedscrolllayout.Utils;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.BallPulseIndicator;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.BallSpinFadeLoaderIndicator;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.JellyCircleDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.LoadingDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.ManyCircle;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.MaterialDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.RingDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.WaterDropDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.WindowsLoad;

import java.security.InvalidParameterException;

/**
 * Created by Administrator on 2017/5/12 0012.
 */

public class DefultRefreshView extends FrameLayout implements PtrUIHandler {
    public static final int STYLE_MATERIAL = 0;
    public static final int STYLE_JELLYCIRCLE = 1;
    public static final int STYLE_WATER_DROP = 2;
    public static final int STYLE_RING = 3;
    public static final int STYLE_BALLPULSE = 4;
    public static final int STYLE_BALLSPINFADE = 5;
    public static final int STYLE_MANYCIRCLE = 6;
    public static final int STYLE_WINDOWLOAD = 7;


    public final int  sDefaultHeightDp = 64;
    private int mHeight ;
    boolean isShowWave;
    MaterialWaveView mMaterialWaveView;
    ImageView mRefreshView;
    private LoadingDrawable mLoadingDrawable;

    private int[] mColorSchemeColors;

    private int mWaveColor =0x400000ff;

    public DefultRefreshView(Context context) {
        this(context,null);
    }
    public DefultRefreshView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }
    public DefultRefreshView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }
    int mDrawableStyle;
    private void init(final Context context,AttributeSet attrs){
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DefultRefreshView);
        int defaultStyle = (int) (8* Math.random());
        if(defaultStyle ==8){
            defaultStyle = 7;
        }
        mDrawableStyle = a.getInteger(R.styleable.DefultRefreshView_refreshType, STYLE_WINDOWLOAD);
        isShowWave = a.getBoolean(R.styleable.DefultRefreshView_isShowWave,true);
        final int colorsId = a.getResourceId(R.styleable.DefultRefreshView_refreshColors, 0);
        final int colorId = a.getResourceId(R.styleable.DefultRefreshView_refreshColor, 0);
        a.recycle();
        if (colorsId > 0) {
            mColorSchemeColors = context.getResources().getIntArray(colorsId);
        } else {
            mColorSchemeColors = new int[]{Color.rgb(0xC9, 0x34, 0x37), Color.rgb(0x37, 0x5B, 0xF1), Color.rgb(0xF7, 0xD2, 0x3E), Color.rgb(0x34, 0xA3, 0x50)};
        }

        if (colorId > 0) {
            mColorSchemeColors = new int[]{context.getResources().getColor(colorId)};
        }
        mHeight = (int) Utils.dip2px(context,sDefaultHeightDp);
        mMaterialWaveView = new MaterialWaveView(context);
        mMaterialWaveView.setWaveColor(mWaveColor);
        addView(mMaterialWaveView);

        mRefreshView = new ImageView(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mRefreshView,params);
        setRefreshStyle(mDrawableStyle);
        if(isShowWave && !mLoadingDrawable.isFullCanvas()){
            mRefreshView.getLayoutParams().height = mHeight;
        }else{
            mMaterialWaveView.setVisibility(GONE);
        }

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isShowWave|| mLoadingDrawable.isFullCanvas()){
            if(getLayoutParams().height>0){
                if(getLayoutParams().height< mHeight*2){
                    getLayoutParams().height = (int) (mHeight*2);
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(isShowWave){
            mMaterialWaveView.setDefaulHeadHeight(mHeight);
            mMaterialWaveView.setDefaulWaveHeight(mHeight*2);


        }
        if(isShowWave || mLoadingDrawable.isFullCanvas()){
            NestedScrollLayout.LayoutParams layoutParams = (NestedScrollLayout.LayoutParams) getLayoutParams();
            RefreshHeaderBehavior behavior= (RefreshHeaderBehavior) layoutParams.getBehavior();
            behavior.setStableRefreshOffset(mHeight);
            behavior.setMaxContentOffset(getMeasuredHeight());
            if(!mLoadingDrawable.isFullCanvas()){
                mRefreshView.layout(mRefreshView.getLeft(),mRefreshView.getTop()+getMeasuredHeight()-mHeight,mRefreshView.getRight(),mRefreshView.getBottom()+getMeasuredHeight()-mHeight);

            }
        }

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public void setRefreshStyle(int type) {
        switch (type) {
            case STYLE_MATERIAL:
                mLoadingDrawable = new MaterialDrawable(getContext(),this,mHeight);
                break;
            case STYLE_WATER_DROP:
                mLoadingDrawable = new WaterDropDrawable(getContext(),mHeight);
                break;
            case STYLE_RING:
                mLoadingDrawable = new RingDrawable(getContext(),mHeight);
                break;
            case STYLE_JELLYCIRCLE:
                mLoadingDrawable = new JellyCircleDrawable(getContext(),mHeight);
                mMaterialWaveView.setVisibility(GONE);
                break;
            case STYLE_BALLPULSE:
                mLoadingDrawable = new  BallPulseIndicator();
                break;
            case STYLE_BALLSPINFADE:
                mLoadingDrawable = new BallSpinFadeLoaderIndicator();
                break;
            case STYLE_MANYCIRCLE:
                mLoadingDrawable = new ManyCircle(getContext());
                break;
            case STYLE_WINDOWLOAD:
                mLoadingDrawable = new WindowsLoad(getContext());
                break;


            default:
                throw new InvalidParameterException("Type does not exist");
        }
        mLoadingDrawable.setColorSchemeColors(mColorSchemeColors);
        mRefreshView.setImageDrawable(mLoadingDrawable);
//        mRefreshView.setImageResource(android.R.drawable.ic_menu_camera);
    }

    @Override
    public void onUIReset(NestedScrollLayout parent) {
        if(showWave())
            mMaterialWaveView.onUIReset(parent);


        mLoadingDrawable.onReset();
    }


    @Override
    public void onUIRefreshPrepare(NestedScrollLayout parent) {
        if(showWave())
            mMaterialWaveView.onUIRefreshPrepare(parent);
    }

    @Override
    public void onUIRefreshBegin(NestedScrollLayout parent) {
        if(showWave())
        mMaterialWaveView.onUIRefreshBegin(parent);
        mLoadingDrawable.start();
    }

    private boolean showWave(){
        if(mMaterialWaveView !=null && mMaterialWaveView.getVisibility() == VISIBLE){
            return true;
        }
        return false;
    }
    @Override
    public void onUIRefreshComplete(NestedScrollLayout parent) {
        if(showWave())
            mMaterialWaveView.onUIRefreshComplete(parent);

        mLoadingDrawable.stop();
    }

    @Override
    public void onUIPositionChange(NestedScrollLayout parent, boolean isUnderTouch, byte status, PtrIndicator indicator) {
        if(showWave())
            mMaterialWaveView.onUIPositionChange(parent,isUnderTouch,status,indicator);
        if(status==  RefreshHeaderBehavior.PTR_STATUS_PREPARE){
            mLoadingDrawable.setPercent(indicator.getCurrentPercent());
            mRefreshView.postInvalidate();
            indicator.setDelayScrollInitail(mLoadingDrawable.getDelayScrollInitail());
        }

    }
}
