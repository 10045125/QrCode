/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :CaptureHandler.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.vanda.qrcode.camera.CameraManager;
import com.vanda.qrcode.inter.IScanCallback;

import java.util.Collection;
import java.util.Map;

import static com.vanda.qrcode.message.MessageConst.AUTO_FOCUS;
import static com.vanda.qrcode.message.MessageConst.DECODE;
import static com.vanda.qrcode.message.MessageConst.DECODE_FAILED;
import static com.vanda.qrcode.message.MessageConst.DECODE_SUCCEEDED;
import static com.vanda.qrcode.message.MessageConst.QUIT;
import static com.vanda.qrcode.message.MessageConst.RESTART_PREVIEW;
import static com.vanda.qrcode.message.MessageConst.RETURN_SCAN_RESULT;


/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureHandler extends Handler {

    private static final String TAG = CaptureHandler.class.getSimpleName();

    private final IScanCallback mIScanCallback;
    private final DecodeThread decodeThread;
    private State state;
    private final CameraManager cameraManager;

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    CaptureHandler(IScanCallback iScanCallback,
                   Collection<BarcodeFormat> decodeFormats,
                   Map<DecodeHintType, ?> baseHints,
                   String characterSet,
                   CameraManager cameraManager) {
        this.mIScanCallback = iScanCallback;
        decodeThread = new DecodeThread(iScanCallback, decodeFormats, baseHints, characterSet,
                new ViewfinderResultPointCallback((ViewfinderView) mIScanCallback.getViewfinderView()));
        decodeThread.start();
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case AUTO_FOCUS:
                if (state == State.PREVIEW) {
                    cameraManager.requestAutoFocus(this, AUTO_FOCUS);
                }
                break;
            case RESTART_PREVIEW:
                restartPreviewAndDecode();
                break;
            case DECODE_SUCCEEDED:
                state = State.SUCCESS;
                Bundle bundle = message.getData();
                Bitmap barcode = null;
                float scaleFactor = 1.0f;
                if (bundle != null) {
                    byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                        // Mutable copy:
                        barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
                }
                mIScanCallback.handleDecode((Result) message.obj, barcode, scaleFactor);
                break;
            case DECODE_FAILED:
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW;
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), DECODE);
                break;
            case RETURN_SCAN_RESULT:
                break;
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), QUIT);
        quit.sendToTarget();
        try {
//      // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(DECODE_SUCCEEDED);
        removeMessages(DECODE_FAILED);
    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestAutoFocus(this, AUTO_FOCUS);
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), DECODE);
            mIScanCallback.drawViewfinder();
        }
    }

}
