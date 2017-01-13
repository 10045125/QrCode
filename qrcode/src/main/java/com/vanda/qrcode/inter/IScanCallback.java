/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :IScanCallback.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode.inter;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Handler;
import android.view.View;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

public interface IScanCallback {
    Context getContext();
    View getViewfinderView();
    void addPossibleResultPoint(ResultPoint point);
    void drawViewfinder();
    void handleDecode(Result result, Bitmap barcode, float scaleFactor);
    PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height);
    Handler getScanHandler();
    int getCWNeededRotation();

    Camera.Parameters getCameraParamters();
}
