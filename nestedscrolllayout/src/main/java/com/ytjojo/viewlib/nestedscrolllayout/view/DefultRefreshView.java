package com.ytjojo.viewlib.nestedscrolllayout.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ytjojo.viewlib.nestedscrolllayout.NestedScrollLayout;
import com.ytjojo.viewlib.nestedscrolllayout.PtrIndicator;
import com.ytjojo.viewlib.nestedscrolllayout.PtrUIHandler;
import com.ytjojo.viewlib.nestedscrolllayout.R;
import com.ytjojo.viewlib.nestedscrolllayout.RefreshHeaderBehavior;
import com.ytjojo.viewlib.nestedscrolllayout.Utils;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.MaterialDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.RefreshDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.RingDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.SmartisanDrawable;
import com.ytjojo.viewlib.nestedscrolllayout.drawable.WaterDropDrawable;

import java.security.InvalidParameterException;

/**
 * Created by Administrator on 2017/5/12 0012.
 */

public class DefultRefreshView extends FrameLayout implements PtrUIHandler {
    public static final int STYLE_MATERIAL = 0;
    public static final int STYLE_CIRCLES = 1;
    public static final int STYLE_WATER_DROP = 2;
    public static final int STYLE_RING = 3;
    public static final int STYLE_SMARTISAN = 4;

    public final int  sDefaultHeightDp = 64;
    private int mHeight ;
    boolean isShowWave;
    MaterialWaveView mMaterialWaveView;
    ImageView mRefreshView;
    private RefreshDrawable mRefreshDrawable;

    private int[] mColorSchemeColors;

    private int mWaveColor =0x880000ff;

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
        mDrawableStyle = a.getInteger(R.styleable.DefultRefreshView_refreshType, STYLE_MATERIAL);
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
        addView(mRefreshView);
        mRefreshView.getLayoutParams().height = mHeight;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)mRefreshView.getLayoutParams();
        params.topMargin = mHeight;
        if(!isShowWave){
            mMaterialWaveView.setVisibility(GONE);
        }

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isShowWave){
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
        if(mRefreshDrawable ==null){
            setRefreshStyle(mDrawableStyle);
            if(isShowWave){
                mMaterialWaveView.setDefaulHeadHeight(mHeight);
                mMaterialWaveView.setDefaulWaveHeight(mHeight*2);
                NestedScrollLayout.LayoutParams layoutParams = (NestedScrollLayout.LayoutParams) getLayoutParams();
                RefreshHeaderBehavior behavior= (RefreshHeaderBehavior) layoutParams.getBehavior();
                behavior.setStableRefreshOffset(mHeight);
                behavior.setMaxContentOffset(getMeasuredHeight());
            }
        }
    }

    public void setRefreshStyle(int type) {
        switch (type) {
            case STYLE_MATERIAL:
                mRefreshDrawable = new MaterialDrawable(getContext(),mHeight);
                break;
            case STYLE_CIRCLES:
                break;
            case STYLE_WATER_DROP:
                mRefreshDrawable = new WaterDropDrawable(getContext(),mHeight);
                break;
            case STYLE_RING:
                mRefreshDrawable = new RingDrawable(getContext(),mHeight);
                break;
            case STYLE_SMARTISAN:
                mRefreshDrawable = new SmartisanDrawable(getContext(),mHeight);
                break;
            default:
                throw new InvalidParameterException("Type does not exist");
        }
        mRefreshDrawable.setColorSchemeColors(mColorSchemeColors);
        mRefreshView.setImageDrawable(mRefreshDrawable);
    }

    @Override
    public void onUIReset(NestedScrollLayout parent) {
        if(mMaterialWaveView!=null)
            mMaterialWaveView.onUIReset(parent);
    }


    @Override
    public void onUIRefreshPrepare(NestedScrollLayout parent) {
        if(mMaterialWaveView!=null)
            mMaterialWaveView.onUIRefreshPrepare(parent);
    }

    @Override
    public void onUIRefreshBegin(NestedScrollLayout parent) {
        if(mMaterialWaveView!=null)
        mMaterialWaveView.onUIRefreshBegin(parent);
    }

    @Override
    public void onUIRefreshComplete(NestedScrollLayout parent) {
        if(mMaterialWaveView!=null)
            mMaterialWaveView.onUIRefreshComplete(parent);
    }

    @Override
    public void onUIPositionChange(NestedScrollLayout parent, boolean isUnderTouch, byte status, PtrIndicator indicator) {
        if(mMaterialWaveView!=null)
            mMaterialWaveView.onUIPositionChange(parent,isUnderTouch,status,indicator);

        mRefreshDrawable.setPercent(indicator.getCurrentPercent());
    }
}
