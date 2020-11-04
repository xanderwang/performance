package com.xander.performance;

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


  private static final String TAG = pTool.TAG + "_StackTraceUtils";

  static String STACK_TRACE_FORMAT = "|\t%s.%s():%s";

  static String[] filterPackageSet;

  static {
    filterPackageSet = new String[]{
        //"com.swift.sandhook",
        //pTool.class.getPackage().getName(),
        me.weishu.epic.BuildConfig.class.getPackage().getName(),
        de.robv.android.xposed.DexposedBridge.class.getPackage().getName()

    };
  }

  static void print(String tag, Thread thread, boolean filterClassName, String className) {
    print(tag, thread.getStackTrace(), filterPackageSet, filterClassName, className);
  }

  /**
   * @param tag                log 的 TAG
   * @param stackTraceElements 调用栈
   * @param filterClassName    是否需要从指定的类名开始
   * @param className          指定的类名
   */
  static void print(String tag, StackTraceElement[] stackTraceElements,
                    boolean filterClassName, String className) {
    print(tag, stackTraceElements, filterPackageSet, filterClassName, className);
  }

  /**
   * @param tag                log 的 TAG
   * @param stackTraceElements 调用栈
   * @param filterPackageSet   需要过滤的包名的 set
   * @param filterClassName    是否需要从指定的类名开始
   * @param className          指定的类名
   */
  static void print(String tag, StackTraceElement[] stackTraceElements, String[] filterPackageSet,
                    boolean filterClassName, String className) {
    formatStackTrace(tag, true, stackTraceElements, filterPackageSet, filterClassName, className);
  }

  static String string(StackTraceElement[] stackTraceElements) {
    return string(stackTraceElements, filterPackageSet, false, "");
  }

  static String string(StackTraceElement[] stackTraceElements, boolean filterClassName, String className) {
    return string(stackTraceElements, filterPackageSet, filterClassName, className);
  }

  static String string(StackTraceElement[] stackTraceElements, String[] filterPackageSet,
                       boolean filterClassName, String className) {
    return formatStackTrace("", false, stackTraceElements, filterPackageSet, filterClassName, className);
  }

  private static String formatStackTrace(String tag, boolean printMode, StackTraceElement[] stackTraceElements,
                                         String[] filterPackageSet, boolean filterClassName, String className) {
    StringBuilder stringBuilder = new StringBuilder();
    boolean hasFindClass = !filterClassName;
    for (int i = 0; i < stackTraceElements.length; i++) {
      StackTraceElement element = stackTraceElements[i];
      String eClassName = element.getClassName();
      if (filterClassName) {
        if (!hasFindClass && !eClassName.equals(className)) {
          continue;
        }
        if (!hasFindClass) {
          hasFindClass = true;
          continue;
        }
      }
      boolean needContinue = false;
      if (null != filterPackageSet) {
        for (int j = 0; j < filterPackageSet.length; j++) {
          if (eClassName.contains(filterPackageSet[j])) {
            needContinue = true;
            break;
          }
        }
        if (needContinue) {
          continue;
        }
      }
      if (printMode) {
        String token = String.format(
            STACK_TRACE_FORMAT,
            element.getClassName(),
            element.getMethodName(),
            element.getLineNumber()
        );
        xLog.e(tag, token);
      } else {
        stringBuilder.append(element.getClassName())
            .append('.')
            .append(element.getMethodName())
            .append("():")
            .append(element.getLineNumber());
        if (i + 1 < stackTraceElements.length) {
          stringBuilder.append(" <- ");
        }
      }
    }
    return stringBuilder.toString();
  }

}
