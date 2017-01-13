/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :OpenCamera.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode.camera.open;

import android.hardware.Camera;

/**
 * Represents an open {@link Camera} and its metadata, like facing direction and orientation.
 */
public final class OpenCamera {
  
  private final int index;
  private final Camera camera;
  private final CameraFacing facing;
  private final int orientation;
  
  public OpenCamera(int index, Camera camera, CameraFacing facing, int orientation) {
    this.index = index;
    this.camera = camera;
    this.facing = facing;
    this.orientation = orientation;
  }

  public Camera getCamera() {
    return camera;
  }

  public CameraFacing getFacing() {
    return facing;
  }

  public int getOrientation() {
    return orientation;
  }

  @Override
  public String toString() {
    return "Camera #" + index + " : " + facing + ',' + orientation;
  }

}
