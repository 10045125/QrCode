/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :AutoFocusCallback.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public final class AutoFocusCallback implements Camera.AutoFocusCallback {

    private static final String TAG = AutoFocusCallback.class.getSimpleName();

    private static final long AUTOFOCUS_INTERVAL_MS = 1500L;

    private Handler autoFocusHandler;
    private int autoFocusMessage;

    public void setHandler(Handler autoFocusHandler, int autoFocusMessage) {
        this.autoFocusHandler = autoFocusHandler;
        this.autoFocusMessage = autoFocusMessage;
    }

    public void onAutoFocus(boolean success, Camera camera) {
        if (autoFocusHandler != null) {
            Message message = autoFocusHandler.obtainMessage(autoFocusMessage, success);
            autoFocusHandler.sendMessageDelayed(message, AUTOFOCUS_INTERVAL_MS);
            autoFocusHandler = null;
        } else {
            Log.d(TAG, "Got auto-focus callback, but no handler for it");
        }
    }

}
