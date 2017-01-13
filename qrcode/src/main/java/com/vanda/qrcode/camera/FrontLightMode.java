/*
 * Copyright (C) 2005-2016 UCWeb Inc. All rights reserved.
 *  Description :FrontLightMode.java
 *
 *  Creation    : 2016-11-18
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda.qrcode.camera;

import android.content.SharedPreferences;

import com.vanda.qrcode.config.Config;


/**
 * Enumerates settings of the preference controlling the front light.
 */
public enum FrontLightMode {

  /** Always on. */
  ON,
  /** On only when ambient light is low. */
  AUTO,
  /** Always off. */
  OFF;

//  private static FrontLightMode parse(String modeString) {
//    return modeString == null ? OFF : valueOf(modeString);
//  }

  public static FrontLightMode readPref(SharedPreferences sharedPrefs) {
    return Config.ConfigType.KEY_FRONT_LIGHT_MODE.enable() ? ON : OFF;
//    return parse(sharedPrefs.getString(Config.KEY_FRONT_LIGHT_MODE, OFF.toString()));
  }

}
