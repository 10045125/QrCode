/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :CameraManager.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.vanda.qrcode.camera.open.OpenCamera;
import com.vanda.qrcode.camera.open.OpenCameraInterface;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private final Context context;
    private final CameraConfigurationManager configManager;
    private OpenCamera camera;
    //    private AutoFocusManager autoFocusManager;
    private Rect framingRect;
    private Rect framingRectInPreview;
    private boolean initialized;
    private boolean previewing;
    private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;
    private int frameWidth;
    private int frameHeight;
    private int topOffset;
    private int leftOffset;

    private final boolean useOneShotPreviewCallback;
    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final PreviewCallback previewCallback;
    /**
     * Autofocus callbacks arrive here, and are dispatched to the Handler which requested them.
     */
    private final AutoFocusCallback autoFocusCallback;

    static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT

    static {
        int sdkInt;
        try {
            sdkInt = Integer.parseInt(Build.VERSION.SDK);
        } catch (NumberFormatException nfe) {
            // Just to be safe
            sdkInt = 10000;
        }
        SDK_INT = sdkInt;
    }

    public CameraManager(Context context) {
        this.context = context;
        frameWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.6);//context.getResources().getDimensionPixelSize(R.dimen.qrcode_area_width);
        frameHeight = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.6);//context.getResources().getDimensionPixelSize(R.dimen.qrcode_area_height);
        this.configManager = new CameraConfigurationManager(context);

        // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
        // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
        // the more efficient one shot callback, as the older one can swamp the system and cause it
        // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
        //useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > Build.VERSION_CODES.CUPCAKE;
        useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; // 3 = Cupcake

        previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
        autoFocusCallback = new AutoFocusCallback();
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public synchronized void openDriver(SurfaceHolder holder) throws IOException {
        OpenCamera theCamera = camera;
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(requestedCameraId);
            if (theCamera == null) {
                throw new IOException("Camera.open() failed to return object from driver");
            }
            camera = theCamera;
        }

        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(theCamera);
            if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
                setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
                requestedFramingRectWidth = 0;
                requestedFramingRectHeight = 0;
            }
        }

        Camera cameraObject = theCamera.getCamera();
        Camera.Parameters parameters = cameraObject.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
        try {
            configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    cameraObject.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
        cameraObject.setPreviewDisplay(holder);

    }

    public synchronized void setOffset(int topOffset, int leftOffset) {
        this.topOffset = topOffset;
        this.leftOffset = leftOffset;
    }

    public synchronized void setScanFrameSize(int width, int height) {
        frameHeight = height;
        frameWidth = width;
    }

    public synchronized boolean isOpen() {
        return camera != null;
    }

    /**
     * Closes the camera driver if still in use.
     */
    public synchronized void closeDriver() {
        if (camera != null) {
            camera.getCamera().release();
            camera = null;
            // Make sure to clear these each time we close the camera, so that any scanning rect
            // requested by intent is forgotten.
            framingRect = null;
            framingRectInPreview = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public synchronized void startPreview() {
        OpenCamera theCamera = camera;
        if (theCamera != null && !previewing) {
            theCamera.getCamera().startPreview();
            previewing = true;
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public synchronized void stopPreview() {
        if (camera != null && previewing) {
            camera.getCamera().stopPreview();
            previewCallback.setHandler(null, 0);
            autoFocusCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    /**
     * Convenience method for {@link com.google.zxing.client.android}
     *
     * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
     */
    public synchronized void setTorch(boolean newSetting) {
        OpenCamera theCamera = camera;
        if (theCamera != null) {
            if (newSetting != configManager.getTorchState(theCamera.getCamera())) {
                configManager.setTorch(theCamera.getCamera(), newSetting);
            }
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public synchronized void requestPreviewFrame(Handler handler, int message) {
        OpenCamera theCamera = camera;
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message);
            if (useOneShotPreviewCallback) {
                theCamera.getCamera().setOneShotPreviewCallback(previewCallback);
            } else {
                theCamera.getCamera().setPreviewCallback(previewCallback);
            }
        }
    }

    /**
     * Asks the camera hardware to perform an autofocus.
     *
     * @param handler The Handler to notify when the autofocus completes.
     * @param message The message to deliver.
     */
    public void requestAutoFocus(Handler handler, int message) {
        if (camera != null && previewing) {
            autoFocusCallback.setHandler(handler, message);
            //Log.d(TAG, "Requesting auto-focus callback");
            camera.getCamera().autoFocus(autoFocusCallback);
        }
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */

    public synchronized Rect getFramingRect() {
        if (framingRect == null) {
            if (camera == null) {
                return null;
            }
            Point screenResolution = configManager.getScreenResolution();
            if (screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

            int width = frameWidth;//findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
            int height = frameHeight;//findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

            int leftOff = (screenResolution.x - width) / 2;
            int topOff = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOff - leftOffset, topOff - topOffset, leftOff + width - leftOffset, topOff + height - topOffset);
            Log.d(TAG, "Calculated framing rect: " + framingRect);
        }
        return framingRect;
    }

    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     *
     * @return {@link Rect} expressing barcode scan area in terms of the preview size
     */
    public synchronized Rect getFramingRectInPreview() {
        if (framingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Point cameraResolution = configManager.getCameraResolution();
            Point screenResolution = configManager.getScreenResolution();
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

            int left, top, right, bottom;

            //坐标转换, 我们二维码扫描是竖屏,图像是横屏,所以需要计算二维码所处的相对坐标
            left = rect.top * cameraResolution.x / screenResolution.y;
            top = rect.left * cameraResolution.y / screenResolution.x;
            right = left + rect.height() * cameraResolution.x / screenResolution.y;
            bottom = top + rect.width() * cameraResolution.y / screenResolution.x;


            //对应一些相机在竖屏下需要旋转270 才能够正常图像,但是图像是横屏的,这个图像也是调转的,所以也需要转换
            if (getCWNeededRotation() == 270) {
                int width = right - left;
                left = cameraResolution.x - left - width;
                right = left + width;
            }

            rect.left = left;
            rect.top = top;
            rect.right = right;
            rect.bottom = bottom;

            framingRectInPreview = rect;
        }
        return framingRectInPreview;
    }


    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    public synchronized void setManualCameraId(int cameraId) {
        requestedCameraId = cameraId;
    }

    /**
     * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
     * them automatically based on screen resolution.
     *
     * @param width  The width in pixels to scan.
     * @param height The height in pixels to scan.
     */
    public synchronized void setManualFramingRect(int width, int height) {
        if (initialized) {
            Point screenResolution = configManager.getScreenResolution();
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "Calculated manual framing rect: " + framingRect);
            framingRectInPreview = null;
        } else {
            requestedFramingRectWidth = width;
            requestedFramingRectHeight = height;
        }
    }

    private byte[] mOriginData;
    private byte[] mRotatedData;

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            return null;
        }

        int previewH = rect.height();
        int previewW = rect.width();
        int size = previewH * previewW;

        if (null == mOriginData) {
            mOriginData = new byte[size];
            mRotatedData = new byte[size];
        } else {
            if (mOriginData.length < size) {
                mOriginData = new byte[size];
                mRotatedData = new byte[size];
            }
        }

        int inputOffset = rect.top * width + rect.left;

        // If the width matches the full width of the underlying data, perform a single copy.
        if (width == previewW) {
            System.arraycopy(data, inputOffset, mOriginData, 0, size);
        }

        // Otherwise copy one cropped row at a time.
        for (int y = 0; y < previewH; y++) {
            int outputOffset = y * previewW;
            System.arraycopy(data, inputOffset, mOriginData, outputOffset, previewW);
            inputOffset += width;
        }

        for (int y = 0; y < previewH; y++) {
            for (int x = 0; x < previewW; x++) {
                if (x + y * previewW >= mOriginData.length) {
                    break;
                }
                mRotatedData[x * previewH + previewH - y - 1] = mOriginData[x + y * previewW];
            }
        }
        int tmp = previewW; // Here we are swapping, that's the difference to #11
        previewW = previewH;
        previewH = tmp;

        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(mRotatedData, previewW, previewH, 0, 0,
                previewW, previewH, false);
    }

    public int getCWNeededRotation() {
        return configManager.getCWNeededRotation();
    }

    public Camera getCamera() {
        return camera.getCamera();
    }

    public boolean isPreviewing() {
        return previewing;
    }

    public boolean isUseOneShotPreviewCallback() {
        return useOneShotPreviewCallback;
    }

    public PreviewCallback getPreviewCallback() {
        return previewCallback;
    }

    public AutoFocusCallback getAutoFocusCallback() {
        return autoFocusCallback;
    }

    public void setPreviewing(boolean previewing) {
        this.previewing = previewing;
    }
}
