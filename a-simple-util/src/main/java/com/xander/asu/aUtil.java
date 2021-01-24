package com.xander.asu;

public class aUtil {

  protected static String logTag(String tag) {
    if (null == aConstants.globalTag) {
      return tag;
    }
    return aUtil.format(aConstants.tagFormat, aConstants.globalTag, tag);
  }

  public static String format(String formatStr, Object... args) {
    if (null == args || args.length == 0) {
      return formatStr;
    }
    return String.format(formatStr, args);
  }
}
