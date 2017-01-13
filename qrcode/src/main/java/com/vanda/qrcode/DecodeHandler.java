/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :DecodeHandler.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.vanda.qrcode.inter.IScanCallback;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static com.vanda.qrcode.message.MessageConst.DECODE;
import static com.vanda.qrcode.message.MessageConst.DECODE_FAILED;
import static com.vanda.qrcode.message.MessageConst.DECODE_SUCCEEDED;
import static com.vanda.qrcode.message.MessageConst.QUIT;


final class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private final IScanCallback mIScanCallback;
    private final MultiFormatReader multiFormatReader;
    private boolean running = true;

    DecodeHandler(IScanCallback iScanCallback, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.mIScanCallback = iScanCallback;
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        switch (message.what) {
            case DECODE:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case QUIT:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        Result rawResult = null;
        Handler handler = mIScanCallback.getScanHandler();
        try {
            PlanarYUVLuminanceSource source = mIScanCallback.buildLuminanceSource(data, width, height);
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));//
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException re) {
                    // continue
                } finally {
                    multiFormatReader.reset();
                }
            }

            if (rawResult != null) {
                // Don't log the barcode contents for security.
                if (handler != null) {
                    Message message = Message.obtain(handler, DECODE_SUCCEEDED, rawResult);
                    //为了提高扫码速度,去除这里的生成bitmap的操作
//                    Bundle bundle = new Bundle();
//                    bundleThumbnail(source, bundle);
//                    message.setData(bundle);
                    message.sendToTarget();
                }
            } else {
                if (handler != null) {
                    Message message = Message.obtain(handler, DECODE_FAILED);
                    message.sendToTarget();
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            if (handler != null) {
                Message message = Message.obtain(handler, DECODE_FAILED);
                message.sendToTarget();
            }
        }
    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
    }

}
