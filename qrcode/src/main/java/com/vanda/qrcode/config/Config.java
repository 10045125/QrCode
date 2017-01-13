/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :Config.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode.config;

public class Config {
    public enum ConfigType {

        KEY_DECODE_1D_PRODUCT(true),
        KEY_DECODE_1D_INDUSTRIAL(true),
        KEY_DECODE_QR(true),
        KEY_DECODE_DATA_MATRIX(true),
        KEY_DECODE_AZTEC(true),
        KEY_DECODE_PDF417(false),

        KEY_PLAY_BEEP(false),
        KEY_VIBRATE(true),

        KEY_FRONT_LIGHT_MODE(false),
        KEY_BULK_MODE(false),

        KEY_AUTO_FOCUS(true),

        KEY_INVERT_SCAN(true),

        KEY_DISABLE_EXPOSURE(false),
        KEY_DISABLE_CONTINUOUS_FOCUS(false),
        KEY_DISABLE_METERING(false),
        KEY_DISABLE_BARCODE_SCENE_MODE(false),

        KEY_DISABLE_AUTO_ORIENTATION(true);

        private boolean mEnable;
        ConfigType(boolean enable) {
            mEnable = enable;
        }

        public boolean enable() {
            return mEnable;
        }

        public void setEnable(boolean enable) {
            this.mEnable = enable;
        }
    }
}
