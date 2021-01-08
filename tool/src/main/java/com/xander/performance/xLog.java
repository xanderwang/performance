package com.xander.performance;

import android.util.Log;

/**
 * @author Xander Wang Created on 2020/7/29.
 * @Description
 */
class xLog {

  private static int LOG_LEVEL = Log.DEBUG;

  public static void setLogLevel(int logLevel) {
    LOG_LEVEL = logLevel;
  }

  public static void dft(String tag, String msg, Object... args) {
    if (LOG_LEVEL <= Log.DEBUG) {
      xLog.d(tag, String.format(msg, args));
    }
  }

  public static void d(String tag, String msg) {
    if (LOG_LEVEL <= Log.DEBUG) {
      Log.d(tag, msg);
    }
  }

  public static void ift(String tag, String msg, Object... args) {
    if (LOG_LEVEL <= Log.INFO) {
      xLog.i(tag, String.format(msg, args));
    }
  }

  public static void i(String tag, String msg) {
    if (LOG_LEVEL <= Log.INFO) {
      Log.i(tag, msg);
    }
  }

  public static void wft(String tag, String msg, Object... args) {
    if (LOG_LEVEL <= Log.WARN) {
      xLog.w(tag, String.format(msg, args));
    }
  }

  public static void w(String tag, String msg) {
    if (LOG_LEVEL <= Log.WARN) {
      Log.w(tag, msg);
    }
  }

  public static void w(String tag, String msg, Throwable tr) {
    if (LOG_LEVEL <= Log.WARN) {
      Log.w(tag, msg, tr);
    }
  }

  public static void eft(String tag, String msg, Object... args) {
    if (LOG_LEVEL <= Log.ERROR) {
      xLog.e(tag, String.format(msg, args));
    }
  }

  public static void e(String tag, String msg) {
    if (LOG_LEVEL <= Log.ERROR) {
      Log.e(tag, msg);
    }
  }

  public static void e(String tag, String msg, Throwable tr) {
    if (LOG_LEVEL <= Log.ERROR) {
      Log.e(tag, msg, tr);
    }
  }

}
