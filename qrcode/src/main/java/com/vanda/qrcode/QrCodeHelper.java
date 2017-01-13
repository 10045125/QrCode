/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :QrCode.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;
import java.util.Vector;

public class QrCodeHelper {


    private QrCodeHelper() {
    }

    public static void analyzeBitmap(final String path, final IAnalyzeCallback analyzeCallback) {
        if (path == null) {
            if (analyzeCallback != null) {
                analyzeCallback.onAnalyzeFailed();
            }
            return;
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                options.inJustDecodeBounds = false;
                int sampleSize = (int) ((float) options.outHeight / 400.0F);
                if (sampleSize <= 0) {
                    sampleSize = 1;
                }

                options.inSampleSize = sampleSize;
                Bitmap mBitmap = BitmapFactory.decodeFile(path, options);
                final Result mRawResult = decodeBitmatResult(mBitmap);

                callbackMainLooper(new Runnable() {
                    @Override
                    public void run() {
                        if (mRawResult != null) {
                            if (analyzeCallback != null) {
                                analyzeCallback.onAnalyzeSuccess(mRawResult.getText());
                            }
                        } else {
                            if (analyzeCallback != null) {
                                analyzeCallback.onAnalyzeFailed();
                            }
                        }
                    }
                });
            }
        };

        new Thread(runnable).start();
    }

    private static Result decodeBitmatResult(Bitmap bitmap) {
        Result rawResult = null;
        MultiFormatReader multiFormatReader = new MultiFormatReader();
        Hashtable hints = new Hashtable(2);
        Vector decodeFormats = new Vector();
        if (decodeFormats == null || decodeFormats.isEmpty()) {
            decodeFormats = new Vector();
            decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        }

        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);


        multiFormatReader.setHints(hints);


        try {
            rawResult = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(new BitmapLuminanceSource(bitmap))));
        } catch (Exception var10) {
            var10.printStackTrace();
        }

        return rawResult;
    }

    public static void createImage(final String text, final int w, final int h, final Bitmap logo, final ICreateQRCode iCreateQRCode) {
        if (TextUtils.isEmpty(text)) {
            if (iCreateQRCode != null) {
                iCreateQRCode.onCallbackBitmap(null);
            }
        } else {

            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap e = getScaleLogo(logo, w, h);
                        int offsetX = w / 2;
                        int offsetY = h / 2;
                        int scaleWidth = 0;
                        int scaleHeight = 0;
                        if (e != null) {
                            scaleWidth = e.getWidth();
                            scaleHeight = e.getHeight();
                            offsetX = (w - scaleWidth) / 2;
                            offsetY = (h - scaleHeight) / 2;
                        }

                        Hashtable hints = new Hashtable();
                        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
                        hints.put(EncodeHintType.MARGIN, Integer.valueOf(0));
                        BitMatrix bitMatrix = (new QRCodeWriter()).encode(text, BarcodeFormat.QR_CODE, w, h, hints);
                        int[] pixels = new int[w * h];

                        for (int bitmap = 0; bitmap < h; ++bitmap) {
                            for (int x = 0; x < w; ++x) {
                                if (x >= offsetX && x < offsetX + scaleWidth && bitmap >= offsetY && bitmap < offsetY + scaleHeight) {
                                    int pixel = e.getPixel(x - offsetX, bitmap - offsetY);
                                    if (pixel == 0) {
                                        if (bitMatrix.get(x, bitmap)) {
                                            pixel = -16777216;
                                        } else {
                                            pixel = -1;
                                        }
                                    }

                                    pixels[bitmap * w + x] = pixel;
                                } else if (bitMatrix.get(x, bitmap)) {
                                    pixels[bitmap * w + x] = -16777216;
                                } else {
                                    pixels[bitmap * w + x] = -1;
                                }
                            }
                        }
                        final Bitmap mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        mBitmap.setPixels(pixels, 0, w, 0, 0, w, h);

                        callbackMainLooper(new Runnable() {
                            @Override
                            public void run() {
                                if (iCreateQRCode != null) {
                                    iCreateQRCode.onCallbackBitmap(mBitmap);
                                }
                            }
                        });
                    } catch (WriterException var15) {
                        var15.printStackTrace();
                        callbackMainLooper(new Runnable() {
                            @Override
                            public void run() {
                                if (iCreateQRCode != null) {
                                    iCreateQRCode.onCallbackBitmap(null);
                                }
                            }
                        });
                    }
                }
            };
        }
    }

    private static Handler mHandler;

    private static void callbackMainLooper(Runnable runnable) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }

        if (runnable != null) {
            mHandler.post(runnable);
        }
    }

    private static Bitmap getScaleLogo(Bitmap logo, int w, int h) {
        if (logo == null) {
            return null;
        } else {
            Matrix matrix = new Matrix();
            float scaleFactor = Math.min((float) w * 1.0F / 5.0F / (float) logo.getWidth(), (float) h * 1.0F / 5.0F / (float) logo.getHeight());
            matrix.postScale(scaleFactor, scaleFactor);
            Bitmap result = Bitmap.createBitmap(logo, 0, 0, logo.getWidth(), logo.getHeight(), matrix, true);
            return result;
        }
    }

    public interface IAnalyzeCallback {
        void onAnalyzeSuccess(String var2);

        void onAnalyzeFailed();
    }

    public interface ICreateQRCode {
        void onCallbackBitmap(Bitmap bitmap);
    }
}
