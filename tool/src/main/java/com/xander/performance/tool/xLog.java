package com.xander.performance.tool;

    import android.util.Log;

/**
 * @author Xander Wang Created on 2020/7/29.
 * @Description //TODO
 */
public class xLog {

  public static void ef(String tag, String msg, Object... args) {
    xLog.e(tag, String.format(msg, args));
  }

  public static void e(String tag, String msg) {
    Log.e(tag, msg);
  }

}
