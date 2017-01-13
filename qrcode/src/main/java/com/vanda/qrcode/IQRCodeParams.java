/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :IQRCodeParams.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode;

interface IQRCodeParams {
    void setScanFrameSize(int width, int height);
    void setOffset(int topOffset, int leftOffset);
    void setText(String mText);
    void setTextSize(int size);
    void setTextColor(int color);
    void setTextMarginScanTop(int marginScanTop);
    void setScanBgColor(int color, int alpha);
    void setScanLineColor(int color, int alpha);
    void setLineHeight(int height);
    void setInnerCornerLenght(int lenght);
    void setInnerCornerWidth(int width);
    void setInnerCornerColor(int color);
    void setRawBeep(int rawBeep);
}
