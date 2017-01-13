/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :DecodeThread.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;
import com.vanda.qrcode.config.Config;
import com.vanda.qrcode.inter.IScanCallback;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {

  public static final String BARCODE_BITMAP = "barcode_bitmap";
  public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";

  private final IScanCallback mIScanCallback;
  private final Map<DecodeHintType,Object> hints;
  private Handler handler;
  private final CountDownLatch handlerInitLatch;

  DecodeThread(IScanCallback iScanCallback,
               Collection<BarcodeFormat> decodeFormats,
               Map<DecodeHintType,?> baseHints,
               String characterSet,
               ResultPointCallback resultPointCallback) {

    this.mIScanCallback = iScanCallback;
    handlerInitLatch = new CountDownLatch(1);

    hints = new EnumMap<>(DecodeHintType.class);
    if (baseHints != null) {
      hints.putAll(baseHints);
    }

    // The prefs can't change while the thread is running, so pick them up once here.
    if (decodeFormats == null || decodeFormats.isEmpty()) {
//      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(iScanCallback.getContext());
      decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
      if (Config.ConfigType.KEY_DECODE_1D_PRODUCT.enable()) {
        decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
      }
      if (Config.ConfigType.KEY_DECODE_1D_INDUSTRIAL.enable()) {
        decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
      }
      if (Config.ConfigType.KEY_DECODE_QR.enable()) {
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
      }
      if (Config.ConfigType.KEY_DECODE_DATA_MATRIX.enable()) {
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
      }
      if (Config.ConfigType.KEY_DECODE_AZTEC.enable()) {
        decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
      }
      if (Config.ConfigType.KEY_DECODE_PDF417.enable()) {
        decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
      }
    }
    hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

    if (characterSet != null) {
      hints.put(DecodeHintType.CHARACTER_SET, characterSet);
    }
    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
    Log.i("DecodeThread", "Hints: " + hints);
  }

  Handler getHandler() {
    try {
      handlerInitLatch.await();
    } catch (InterruptedException ie) {
      // continue?
    }
    return handler;
  }

  @Override
  public void run() {
    Looper.prepare();
    handler = new DecodeHandler(mIScanCallback, hints);
    handlerInitLatch.countDown();
    Looper.loop();
  }

}
