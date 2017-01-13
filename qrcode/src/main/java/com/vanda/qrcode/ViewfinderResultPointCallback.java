/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :ViewfinderResultPointCallback.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

final class ViewfinderResultPointCallback implements ResultPointCallback {

  private final ViewfinderView viewfinderView;

  ViewfinderResultPointCallback(ViewfinderView viewfinderView) {
    this.viewfinderView = viewfinderView;
  }

  @Override
  public void foundPossibleResultPoint(ResultPoint point) {
    viewfinderView.addPossibleResultPoint(point);
  }

}
