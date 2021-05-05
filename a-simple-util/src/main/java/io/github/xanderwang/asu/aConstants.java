package io.github.xanderwang.asu;

import android.util.Log;

public class aConstants {
  public static int logLevel = Log.DEBUG;

  protected static String globalTag = "asu";

  protected static String tagFormat = "%s_%s";

  public static void setGlobalTag(String tag) {
    if (null == tag || "".equals(tag.trim())) {
      return;
    }
    globalTag = tag;
  }

}
