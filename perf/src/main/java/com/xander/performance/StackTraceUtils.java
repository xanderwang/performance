package com.xander.performance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @ProjectName: performace
 * @Package: com.xander.performance
 * @ClassName: StackTraceUtils
 * @Description:
 * @Author: Xander
 * @CreateDate: 2020/11/4 21:42
 * @Version: 1.0
 */
class StackTraceUtils {

  @Deprecated
  static Set<String> ignorePackageSet = new HashSet<>();

  static {
    StringBuilder stringBuilder = new StringBuilder();
    ignorePackageSet.add(filterPackageName(
        stringBuilder,
        com.xander.performance.StackTraceUtils.class.getName()
    ));
    ignorePackageSet.add(filterPackageName(
        stringBuilder,
        me.weishu.epic.BuildConfig.class.getName()
    ));
    ignorePackageSet.add(filterPackageName(
        stringBuilder,
        de.robv.android.xposed.DexposedBridge.class.getName()
    ));
  }

  public static List<String> list() {
    StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
    return list(stackTraceElements);
  }

  public static List<String> list(Thread thread) {
    StackTraceElement[] stackTraceElements = thread.getStackTrace();
    return list(stackTraceElements);
  }

  public static List<String> list(StackTraceElement[] stackTraceElements) {
    List<String> list = new ArrayList<>(stackTraceElements.length);
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0, len = stackTraceElements.length; i < len; i++) {
      StackTraceElement element = stackTraceElements[i];
      list.add(stringStackTraceElement(element, stringBuilder));
    }
    return list;
  }

  @Deprecated
  private static String filterPackageName(String className) {
    return filterPackageName(new StringBuilder(), className);
  }

  @Deprecated
  private static String filterPackageName(StringBuilder stringBuilder, String className) {
    // 取前面 3 段作为包名，提高后续查询效率
    stringBuilder.setLength(0);
    int count = 0;
    for (int m = 0, n = className.length(); m < n; m++) {
      char c = className.charAt(m);
      if (c == '.') {
        count++;
      }
      if (count == 3) {
        break;
      }
      stringBuilder.append(c);
    }
    return stringBuilder.toString();
  }

  private static String stringStackTraceElement(StackTraceElement element,
                                                StringBuilder stringBuilder) {
    stringBuilder.delete(0, stringBuilder.length());
    stringBuilder.append(element.getClassName())
        .append('.')
        .append(element.getMethodName())
        .append('(')
        .append(element.getFileName())
        .append(':')
        .append(element.getLineNumber())
        .append(')');
    return stringBuilder.toString();
  }

}
