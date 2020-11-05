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
        me.weishu.epic.BuildConfig.class.getPackage().getName(),
        de.robv.android.xposed.DexposedBridge.class.getPackage().getName()

    };
  }

  /**
   * @param tag                log 的 TAG
   * @param stackTraceElements 调用栈
   * @param traceName          trace 名称
   */
  static void print(String tag, StackTraceElement[] stackTraceElements, String traceName) {
    print(tag, stackTraceElements, traceName, false, "");
  }

  /**
   * @param tag                log 的 TAG
   * @param stackTraceElements 调用栈
   * @param traceName          trace 名称
   * @param filterClassName    是否需要从指定的类名开始
   * @param className          指定的类名
   */
  static void print(String tag, StackTraceElement[] stackTraceElements, String traceName,
      boolean filterClassName, String className) {
    print(tag, stackTraceElements, traceName, filterPackageSet, filterClassName, className);
  }

  /**
   * @param tag                log 的 TAG
   * @param stackTraceElements 调用栈
   * @param traceName          trace 名称
   * @param filterPackageSet   需要过滤的包名的 set
   * @param filterClassName    是否需要从指定的类名开始
   * @param className          指定的类名
   */
  static void print(String tag, StackTraceElement[] stackTraceElements, String traceName,
      String[] filterPackageSet, boolean filterClassName, String className) {
    xLog.e(tag, "|==================  " + traceName + "  ==================");
    if (null == stackTraceElements || stackTraceElements.length == 0) {
      xLog.e(tag, "| Thread StackTraceElements is null !!!");
    } else {
      formatStackTrace(
          stackTraceElements,
          filterPackageSet,
          filterClassName,
          className,
          tag,
          true,
          false
      );
    }
    xLog.e(tag, "|---------------------------------------------------------");
  }

  static String string(StackTraceElement[] stackTraceElements) {
    return string(stackTraceElements, filterPackageSet, false, "");
  }

  static String string(StackTraceElement[] stackTraceElements, boolean filterClassName, String className) {
    return string(stackTraceElements, filterPackageSet, filterClassName, className);
  }

  static String string(StackTraceElement[] stackTraceElements, String[] filterPackageSet,
      boolean filterClassName, String className) {
    return formatStackTrace(
        stackTraceElements,
        filterPackageSet,
        filterClassName,
        className,
        pTool.TAG + "_stack_string",
        false,
        true
    );
  }

  private static String formatStackTrace(StackTraceElement[] stackTraceElements, String[] filterPackageSet,
      boolean filterClassName, String className,String tag, boolean printMode, boolean stringMode) {
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
      }
      if (stringMode) {
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
