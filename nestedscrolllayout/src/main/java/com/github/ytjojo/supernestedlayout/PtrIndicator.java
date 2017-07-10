package com.github.ytjojo.supernestedlayout;

import android.graphics.PointF;

import com.orhanobut.logger.Logger;

public class PtrIndicator {

    public static final float INVALID_MAXOFFSETRATIO = -1;
    public static final int POS_START = 0;
    protected int mOffsetToRefresh = 0;
    private PointF mPtLastMove = new PointF();
    private float mOffsetDx;
    private float mOffsetDy;
    private int mCurrentPos = 0;
    private int mLastPos = 0;
    private int mStableRefreshOffset;
    private int mPressedPos = 0;
    private float mRatioOfHeaderHeightToRefresh = 1.2F;
    private float mMaxDistanceRatio = 3F;
    private boolean mIsUnderTouch = false;
    private int mOffsetToKeepHeaderWhileLoading = -1;
    private int mRefreshCompleteY = 0;
    private int mMaxContentOffsetY;

    private long mDelayScrollInitail;
    public void setDelayScrollInitail(long delayScrollInitail){
        this.mDelayScrollInitail = delayScrollInitail;
    }
    public long getDelayScrollInitail(){
        return mDelayScrollInitail;
    }

    public PtrIndicator() {
    }

    public boolean isUnderTouch() {
        return this.mIsUnderTouch;
    }

    public float getMaxDistanceRatio() {
        return this.mMaxDistanceRatio;
    }

    public void setMaxDistanceRatio(float maxDistanceRatio) {
        this.mMaxDistanceRatio = maxDistanceRatio;
        mMaxContentOffsetY = (int) (mMaxDistanceRatio * mStableRefreshOffset);
    }

    public void onRelease() {
        this.mIsUnderTouch = false;
    }

    public void onUIRefreshComplete() {
        this.mRefreshCompleteY = this.mCurrentPos;
    }

    public boolean goDownCrossFinishPosition() {
        return this.mCurrentPos >= this.mRefreshCompleteY;
    }

    protected void processOnMove(float currentX, float currentY, float offsetDx, float offsetDy) {
        this.setOffset(offsetDx, offsetDy);
    }

    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        this.mRatioOfHeaderHeightToRefresh = ratio;
        this.mOffsetToRefresh = (int)((float)this.mStableRefreshOffset * ratio);
    }

    public float getRatioOfHeaderToHeightToRefresh() {
        return this.mRatioOfHeaderHeightToRefresh;
    }

    public int getOffsetToRefresh() {
        return this.mOffsetToRefresh;
    }

    public void setOffsetToRefresh(int offset) {
        this.mRatioOfHeaderHeightToRefresh = (float)this.mStableRefreshOffset * 1.0F / (float)offset;
        this.mOffsetToRefresh = offset;
    }

    public void onPressDown() {
        this.mIsUnderTouch = true;
    }

    public final void onMove(float x, float y) {
        float offsetDx = x - this.mPtLastMove.x;
        float offsetDy = y - this.mPtLastMove.y;
        this.processOnMove(x, y, offsetDx, offsetDy);
        this.mPtLastMove.set(x, y);
        setCurrentPos((int) y);
    }

    protected void setOffset(float x, float y) {
        this.mOffsetDx = x;
        this.mOffsetDy = y;
    }

    public float getOffsetDx() {
        return this.mOffsetDx;
    }

    public float getOffsetDy() {
        return this.mOffsetDy;
    }

    public int getLastPosY() {
        return this.mLastPos;
    }

    public int getCurrentPosY() {
        return this.mCurrentPos;
    }

    public final void setCurrentPos(int current) {
        this.mLastPos = this.mCurrentPos;
        this.mCurrentPos = current;
        this.onUpdatePos(current, this.mLastPos);
    }

    protected void onUpdatePos(int current, int last) {
    }

    public int getStableRefreshOffset() {
        return this.mStableRefreshOffset;
    }

    public void setStableRefreshOffset(int stableOffset) {
        this.mStableRefreshOffset = stableOffset;
        this.updateValue();
    }

    protected void updateValue() {
        this.mOffsetToRefresh = (int)(this.mRatioOfHeaderHeightToRefresh * (float)this.mStableRefreshOffset);
        if(mMaxDistanceRatio !=INVALID_MAXOFFSETRATIO){
            this.mMaxContentOffsetY = (int) (mMaxDistanceRatio * mStableRefreshOffset);
        }
    }
    public int getMaxContentOffsetY(){
        return mMaxContentOffsetY;
    }
    public void setMaxContentOffsetY(int maxOffsetY){
        if(maxOffsetY>0){
            mMaxDistanceRatio = INVALID_MAXOFFSETRATIO;
            mMaxContentOffsetY = maxOffsetY;
        }
    }

    public void convertFrom(PtrIndicator ptrSlider) {
        this.mCurrentPos = ptrSlider.mCurrentPos;
        this.mLastPos = ptrSlider.mLastPos;
        this.mStableRefreshOffset = ptrSlider.mStableRefreshOffset;
    }

    public boolean hasLeftStartPosition() {
        return this.mCurrentPos > 0;
    }

    public boolean hasJustLeftStartPosition() {
        return this.mLastPos == 0 && this.hasLeftStartPosition();
    }

    public boolean hasJustBackToStartPosition() {
        return this.mLastPos != 0 && this.isInStartPosition();
    }

    public boolean isOverOffsetToRefresh() {
        return this.mCurrentPos >= this.getOffsetToRefresh();
    }

    public boolean hasMovedAfterPressedDown() {
        return this.mCurrentPos != this.mPressedPos;
    }

    public boolean isInStartPosition() {
        return this.mCurrentPos == 0;
    }

    public boolean crossRefreshLineFromTopToBottom() {
        return this.mLastPos < this.getOffsetToRefresh() && this.mCurrentPos >= this.getOffsetToRefresh();
    }

    public boolean hasJustReachedHeaderHeightFromTopToBottom() {
        return this.mLastPos < this.mStableRefreshOffset && this.mCurrentPos >= this.mStableRefreshOffset;
    }

    public boolean isOverOffsetToKeepHeaderWhileLoading() {
        return this.mCurrentPos > this.getOffsetToKeepHeaderWhileLoading();
    }

    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        this.mOffsetToKeepHeaderWhileLoading = offset;
    }

    public int getOffsetToKeepHeaderWhileLoading() {
        return this.mOffsetToKeepHeaderWhileLoading >= 0?this.mOffsetToKeepHeaderWhileLoading:this.mStableRefreshOffset;
    }

    public boolean isAlreadyHere(int to) {
        return this.mCurrentPos == to;
    }

    public float getLastPercent() {
        float oldPercent = this.mStableRefreshOffset == 0?0.0F:(float)this.mLastPos * 1.0F / (float)this.mStableRefreshOffset;
        return oldPercent;
    }

    public float getCurrentPercent() {
        Logger.e("mCurrentPos"+ mCurrentPos);
        float currentPercent = this.mStableRefreshOffset == 0?0.0F:(float)this.mCurrentPos * 1.0F / (float)this.mStableRefreshOffset;
        return currentPercent;
    }

    public boolean willOverTop(int to) {
        return to < 0;
    }
}