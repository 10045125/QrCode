/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :ViewfinderView.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.vanda.qrcode.camera.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class ViewfinderView extends View {
    private static final long ANIMATION_DELAY = 10L;
    private static final int POINT_SIZE = 6;
    // 扫描线移动速度
    private static final int SCAN_VELOCITY = 15;
    private static final int ALPHA_VELOCITY = 1;
    private static final int ALPHA_VELOCITY_LINE = 4;
    private static final int ALPHA_SCAN_LINE = 204;
    private static final int ALPHA_SCAN_BG = 51;

    private String mText;

    private CameraManager mCameraManager;
    private final Paint mPaint;
    private final int mMaskColor;

    // 扫描线移动的y
    private int mScanLineTop;
    private int mScanLineColor;
    private int mScanLineColorAphla;
    private int mScanBgColorAphla;
    private int mScanBgColor;
    private int mScanLineHeight;

    private int mTextColor;
    private int mTextSize;
    private int mTextMarginTop;
    private int mTextWidth;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        mMaskColor = resources.getColor(R.color.viewfinder_mask);
        mTextSize = (int) getContext().getResources().getDimension(R.dimen.qrcode_area_scan_text_size);
        mTextColor = resources.getColor(R.color.viewfinder_text_color);

        mTextMarginTop = (int) getContext().getResources().getDimension(R.dimen.qrcode_area_scan_text_size_margin_top);
        mScanLineColor = resources.getColor(R.color.viewfinder_scan_line_color);//cc
        mScanLineColorAphla = ALPHA_SCAN_LINE;
        mScanBgColor = resources.getColor(R.color.viewfinder_scan_bg_color);//33
        mScanBgColorAphla = ALPHA_SCAN_BG;
        mScanLineHeight = getContext().getResources().getDimensionPixelSize(R.dimen.qrcode_area_scan_line_height);

        // 扫描框边角颜色
        mInnercornercolor = resources.getColor(R.color.viewfinder_scan_inner_corner_color);
        // 扫描框边角长度
        mInnercornerlength = getContext().getResources().getDimensionPixelSize(R.dimen.qrcode_area_corner_width);
        // 扫描框边角宽度
        mInnercornerwidth = getContext().getResources().getDimensionPixelSize(R.dimen.qrcode_area_corner_height);
    }

    public void setmCameraManager(CameraManager mCameraManager) {
        this.mCameraManager = mCameraManager;
    }

    public void setOffset(int topOffset, int leftOffset) {
        this.mCameraManager.setOffset(topOffset, leftOffset);
    }

    public void setScanFrameSize(int width, int height) {
        this.mCameraManager.setScanFrameSize(width, height);
    }

    public void setText(String mText) {
        this.mText = mText;
        if (!TextUtils.isEmpty(mText)) {
            mPaint.setTextSize(mTextSize);
            mTextWidth = (int) mPaint.measureText(mText) + 1;
        }
    }

    public void setTextSize(int size) {
        mTextSize = size;
        if (!TextUtils.isEmpty(mText)) {
            mPaint.setTextSize(mTextSize);
            mTextWidth = (int) mPaint.measureText(mText) + 1;
        }
    }

    public void setTextColor(int color) {
        mTextColor = color;
    }

    public void setTextMarginScanTop(int marginScanTop) {
        mTextMarginTop = marginScanTop;
    }

    public void setScanBgColor(int color, int alpha) {
        mScanBgColor = color;
        mScanBgColorAphla = alpha;
    }

    public void setScanLineColor(int color, int alpha) {
        mScanLineColor = color;
        mScanBgColorAphla = alpha;
    }

    public void setLineHeight(int height) {
        mScanLineHeight = height;
    }

    public void setInnerCornerLenght(int lenght) {
        mInnercornerlength = lenght;
    }

    public void setInnerCornerWidth(int width) {
        mInnercornerwidth = width;
    }

    public void setInnerCornerColor(int color) {
        mInnercornercolor = color;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (mCameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = mCameraManager.getFramingRect();
        Rect previewFrame = mCameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        mPaint.setColor(mMaskColor);
        canvas.drawRect(0, 0, width, frame.top, mPaint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, mPaint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, mPaint);
        canvas.drawRect(0, frame.bottom + 1, width, height, mPaint);

        drawFrameBounds(canvas, frame);
        drawScanLight(canvas, frame);
        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
    }

    public void drawViewfinder() {
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
    }

    private boolean mIsClean = false;
    private int mStartBgAlpha;
    private int mStartLineAlpha;

    /**
     * 绘制移动扫描线
     *
     * @param canvas
     * @param frame
     */
    private void drawScanLight(final Canvas canvas, final Rect frame) {

        if (mScanLineTop == 0) {
            mScanLineTop = frame.top;
        }

        if (!mIsClean && mScanLineTop >= frame.bottom - mScanLineHeight) {
            mScanLineTop = frame.top;
        } else {
            mScanLineTop += (SCAN_VELOCITY);
            int height = frame.bottom - mScanLineHeight;
            if (mScanLineTop >= height) {
                mScanLineTop = height;
                mIsClean = true;
            }
        }

        mPaint.setColor(mScanBgColor);
        mStartBgAlpha = mIsClean ? mStartBgAlpha - ALPHA_VELOCITY : mScanBgColorAphla;
        mPaint.setAlpha(mStartBgAlpha);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(frame.left, frame.top, frame.right,
                mScanLineTop, mPaint);
        mPaint.setColor(mScanLineColor);
        mStartLineAlpha = mIsClean ? mStartLineAlpha - ALPHA_VELOCITY_LINE : mScanLineColorAphla;
        mPaint.setAlpha(mStartLineAlpha);
        canvas.drawRect(frame.left, mScanLineTop, frame.right,
                mScanLineTop + mScanLineHeight, mPaint);

        if (!TextUtils.isEmpty(mText)) {
            mPaint.setColor(mTextColor);
            mPaint.setTextSize(mTextSize);
            canvas.drawText(mText, frame.left + (frame.right - frame.left) / 2 - mTextWidth / 2, frame.bottom + mTextMarginTop, mPaint);
        }

        if (mStartBgAlpha <= 0 || mStartLineAlpha <= 0) {
            mIsClean = false;
        }
    }


    // 扫描框边角颜色
    private int mInnercornercolor;
    // 扫描框边角长度
    private int mInnercornerlength;
    // 扫描框边角宽度
    private int mInnercornerwidth;

    /**
     * 绘制取景框边框
     *
     * @param canvas
     * @param frame
     */
    private void drawFrameBounds(Canvas canvas, Rect frame) {

        /*mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);

        canvas.drawRect(frame, mPaint);*/

        mPaint.setColor(mInnercornercolor);
        mPaint.setStyle(Paint.Style.FILL);

        int corWidth = mInnercornerwidth;
        int corLength = mInnercornerlength;

        // 左上角
        canvas.drawRect(frame.left - corWidth, frame.top - corWidth, frame.left, frame.top
                + corLength, mPaint);
        canvas.drawRect(frame.left - corWidth, frame.top - corWidth, frame.left
                + corLength, frame.top, mPaint);
        // 右上角
        canvas.drawRect(frame.right, frame.top - corWidth, frame.right + corWidth,
                frame.top + corLength, mPaint);
        canvas.drawRect(frame.right - corLength, frame.top - corWidth,
                frame.right + corWidth, frame.top, mPaint);
        // 左下角
        canvas.drawRect(frame.left - corWidth, frame.bottom - corLength,
                frame.left, frame.bottom + corWidth, mPaint);
        canvas.drawRect(frame.left - corWidth, frame.bottom, frame.left
                + corLength, frame.bottom + corWidth, mPaint);
        // 右下角
        canvas.drawRect(frame.right, frame.bottom - corLength,
                frame.right + corWidth, frame.bottom + corWidth, mPaint);
        canvas.drawRect(frame.right - corLength, frame.bottom,
                frame.right + corWidth, frame.bottom + corWidth, mPaint);
    }
}
