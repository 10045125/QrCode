/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :QrCodeView.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.vanda.qrcode.camera.CameraManager;
import com.vanda.qrcode.config.Config;
import com.vanda.qrcode.inter.IScanCallback;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static com.vanda.qrcode.message.MessageConst.DECODE_SUCCEEDED;
import static com.vanda.qrcode.message.MessageConst.RESTART_PREVIEW;


public class QrCodeView extends FrameLayout implements SurfaceHolder.Callback, IScanCallback, IQRCodeParams {

    private static final String TAG = QrCodeView.class.getSimpleName();

    private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

    private CameraManager cameraManager;
    private CaptureHandler handler;
    private Result savedResultToShow;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;

    private SurfaceView mSurfaceView;
    private ViewfinderView mViewfinderView;
    private Activity mActivity;

    private IntentSource source;

    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> decodeHints;
    private String characterSet;

    private ResultCallback mResultCallback;

    public interface ResultCallback {
        void onResultCallback(Result result);
    }

    public void registerResultCallback(ResultCallback resultCallback) {
        mResultCallback = resultCallback;
    }

    public QrCodeView(Activity context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = context;
        onCreate();
    }

    private void onCreate() {
        Window window = mActivity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hasSurface = false;
        ambientLightManager = new AmbientLightManager(mActivity);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        init();
        onResume();
    }

    private void init() {
        cameraManager = new CameraManager(mActivity);
        mViewfinderView = new ViewfinderView(getContext(), null);
        mViewfinderView.setmCameraManager(cameraManager);

        handler = null;
        source = IntentSource.NONE;

        mSurfaceView = new SurfaceView(mActivity, null);

        addView(mSurfaceView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mViewfinderView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void onResume() {
        if (inactivityTimer == null) {
            inactivityTimer = new InactivityTimer(mActivity);
        }
        if (beepManager == null) {
            beepManager = new BeepManager(mActivity);
        }

        if (inactivityTimer != null) {
            inactivityTimer.onResume();
        }

        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    public void setScanFrameSize(int width, int height) {
        mViewfinderView.setScanFrameSize(width, height);
    }

    @Override
    public void setOffset(int topOffset, int leftOffset) {
        mViewfinderView.setOffset(topOffset, leftOffset);
    }

    @Override
    public void setText(String mText) {
        mViewfinderView.setText(mText);
    }

    @Override
    public void setTextSize(int size) {
        mViewfinderView.setTextSize(size);
    }

    @Override
    public void setTextColor(int color) {
        mViewfinderView.setTextColor(color);
    }

    @Override
    public void setTextMarginScanTop(int marginScanTop) {
        mViewfinderView.setTextMarginScanTop(marginScanTop);
    }

    @Override
    public void setScanBgColor(int color, int alpha) {
        mViewfinderView.setScanBgColor(color, alpha);
    }

    @Override
    public void setScanLineColor(int color, int alpha) {
        mViewfinderView.setScanLineColor(color, alpha);
    }

    @Override
    public void setLineHeight(int height) {
        mViewfinderView.setLineHeight(height);
    }

    @Override
    public void setInnerCornerLenght(int lenght) {
        mViewfinderView.setInnerCornerLenght(lenght);
    }

    @Override
    public void setInnerCornerWidth(int width) {
        mViewfinderView.setInnerCornerWidth(width);
    }

    @Override
    public void setInnerCornerColor(int color) {
        mViewfinderView.setInnerCornerColor(color);
    }

    @Override
    public void setRawBeep(int rawBeep) {
        if (beepManager != null) {
            beepManager.setRawBeep(rawBeep);
        }
    }

    public void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        if (inactivityTimer != null) {
            inactivityTimer.onPause();
        }
        if (ambientLightManager != null) {
            ambientLightManager.stop();
        }
        if (beepManager != null) {
            beepManager.close();
        }
        if (cameraManager != null) {
            cameraManager.closeDriver();
        }
        if (!hasSurface) {
            mSurfaceView = new SurfaceView(mActivity, null);
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }

    public void onDestroy() {
        if (inactivityTimer != null) {
            inactivityTimer.shutdown();
            inactivityTimer.onPause();
        }
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        if (ambientLightManager != null) {
            ambientLightManager.stop();
        }
        if (beepManager != null) {
            beepManager.close();
        }
        if (cameraManager != null) {
            cameraManager.closeDriver();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                restartPreviewAfterDelay(0L);
                return true;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, DECODE_SUCCEEDED, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
        if (cameraManager != null && cameraManager.isPreviewing()) {
            if (!cameraManager.isUseOneShotPreviewCallback()) {
                cameraManager.getCamera().setPreviewCallback(null);
            }
            cameraManager.stopPreview();
            cameraManager.getPreviewCallback().setHandler(null, 0);
            cameraManager.getAutoFocusCallback().setHandler(null, 0);
            cameraManager.setPreviewing(false);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, rawResult);
        }

        switch (source) {
            case NATIVE_APP_INTENT:
            case NONE:
                if (fromLiveScan && Config.ConfigType.KEY_BULK_MODE.enable()) {
                    restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
                }
                if (mResultCallback != null) {
                    mResultCallback.onResultCallback(rawResult);
                }
                break;
        }
    }

    @Override
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        return cameraManager.buildLuminanceSource(data, width, height);
    }

    @Override
    public int getCWNeededRotation() {
        return cameraManager.getCWNeededRotation();
    }

    @Override
    public Camera.Parameters getCameraParamters() {
        return cameraManager.getCamera().getParameters();
    }

    @Override
    public Handler getScanHandler() {
        return handler;
    }

    public ViewfinderView getViewfinderView() {
        return mViewfinderView;
    }

    @Override
    public void addPossibleResultPoint(ResultPoint point) {
        mViewfinderView.addPossibleResultPoint(point);
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(RESTART_PREVIEW, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
        mViewfinderView.setVisibility(View.VISIBLE);
    }

    public void drawViewfinder() {
        mViewfinderView.drawViewfinder();
    }
}
