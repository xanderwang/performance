package io.github.xanderwang.performance;

import java.util.ArrayList;
import java.util.HashSet;
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

  private static HashSet<String> IGNORE_CLASS_NAME_SET = new HashSet<>();

  static {
    IGNORE_CLASS_NAME_SET.add(BitmapTool.class.getName());
    IGNORE_CLASS_NAME_SET.add(IPCTool.class.getName());
    IGNORE_CLASS_NAME_SET.add(ThreadTool.class.getName());
    IGNORE_CLASS_NAME_SET.add(UIBlockTool.class.getName());
  }

  /**
   * 是否是应该忽略的 class
   *
   * @param className
   * @return
   */
  private static boolean isIgnoreClass(String className) {
    if (null == className) {
      return false;
    }
    if (!Config.FILTER_CLASS_NAME) {
      return false;
    }
    if (IGNORE_CLASS_NAME_SET.contains(className)) {
      return true;
    }
    if (className.startsWith("io.github.xanderwang.hook")
        || className.startsWith("de.robv.android")
        || className.startsWith("me.weishu.epic")
        || className.startsWith("com.swift.sandhook")) {
      return true;
    }
    return false;
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
      String lineLog = stringStackTraceElement(element, stringBuilder);
      if (null == lineLog) {
        continue;
      }
      list.add(lineLog);
    }
    return list;
  }

  private static String stringStackTraceElement(StackTraceElement element,
      StringBuilder stringBuilder) {
    String className = element.getClassName();
    if (isIgnoreClass(className)) {
      return null;
    }
    stringBuilder.delete(0, stringBuilder.length());
    stringBuilder.append(className)
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
