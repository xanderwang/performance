package com.xander.performance;

import java.util.ArrayList;
import java.util.List;

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
