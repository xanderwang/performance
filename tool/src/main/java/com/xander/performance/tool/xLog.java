package com.xander.performance.tool;

import android.util.Log;

/**
 * @author Xander Wang Created on 2020/7/29.
 * @Description
 */
public class xLog {

  public static void dft(String tag, String msg, Object... args) {
    xLog.d(tag, String.format(msg, args));
  }

  public static void d(String tag, String msg) {
    Log.d(tag, msg);
  }

  public static void ift(String tag, String msg, Object... args) {
    xLog.i(tag, String.format(msg, args));
  }

  public static void i(String tag, String msg) {
    Log.i(tag, msg);
  }

  public static void wft(String tag, String msg, Object... args) {
    xLog.w(tag, String.format(msg, args));
  }

  public static void w(String tag, String msg) {
    Log.w(tag, msg);
  }

  public static void eft(String tag, String msg, Object... args) {
    xLog.e(tag, String.format(msg, args));
  }

  public static void e(String tag, String msg) {
    Log.e(tag, msg);
  }

  public static void e(String tag, String msg, Throwable tr) {
    Log.e(tag, msg,tr);
  }

}
