package com.xander.asu;

import android.util.Log;

public class aLog {

  public static void d(String tag, String formatMsg, Object... args) {
    if (aConstants.logLevel <= Log.DEBUG) {
      Log.d(aUtil.logTag(tag), aUtil.format(formatMsg, args));
    }
  }

  public static void de(String tag, String errMsg, Throwable throwable) {
    if (aConstants.logLevel <= Log.DEBUG) {
      Log.d(aUtil.logTag(tag), errMsg, throwable);
    }
  }

  public static void w(String tag, String formatMsg, Object... args) {
    if (aConstants.logLevel <= Log.WARN) {
      Log.w(aUtil.logTag(tag), aUtil.format(formatMsg, args));
    }
  }

  public static void we(String tag, String errMsg, Throwable throwable) {
    if (aConstants.logLevel <= Log.WARN) {
      Log.w(aUtil.logTag(tag), errMsg, throwable);
    }
  }

  public static void e(String tag, String formatMsg, Object... args) {
    if (aConstants.logLevel <= Log.ERROR) {
      Log.e(aUtil.logTag(tag), aUtil.format(formatMsg, args));
    }
  }

  public static void ee(String tag, String errMsg, Throwable throwable) {
    if (aConstants.logLevel <= Log.ERROR) {
      Log.e(aUtil.logTag(tag), errMsg, throwable);
    }
  }
}
