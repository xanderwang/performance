package io.github.xanderwang.asu;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class aUtil {

  protected static String logTag(String tag) {
    if (null == aConstants.globalTag || "".equals(aConstants.globalTag.trim())) {
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

  public static String memberToString(Member member) {
    if (member instanceof Method) {
      return ((Method) member).toString();
    }
    if (member instanceof Constructor) {
      return ((Constructor) member).toString();
    }
    if (null != member) {
      return member.getName();
    }
    return "null";
  }

}
