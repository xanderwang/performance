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

  static String STACK_TRACE_FORMAT = "|\t%s.%s(%s:%s)";

  static String[] filterPackageSet;

  static Set<String> ignorePackageSet = new HashSet<>();

  static {
    ignorePackageSet.add(me.weishu.epic.BuildConfig.class.getPackage().getName());
    ignorePackageSet.add(de.robv.android.xposed.DexposedBridge.class.getPackage().getName());
    filterPackageSet = new String[]{
        //"com.swift.sandhook",
        me.weishu.epic.BuildConfig.class.getPackage().getName(),
        de.robv.android.xposed.DexposedBridge.class.getPackage().getName()};
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
  static void print(String tag, StackTraceElement[] stackTraceElements, String traceName, boolean filterClassName,
      String className) {
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
  static void print(String tag, StackTraceElement[] stackTraceElements, String traceName, String[] filterPackageSet,
      boolean filterClassName, String className) {
    xLog.e(tag, "|==================  " + traceName + "  ==================");
    if (null == stackTraceElements || stackTraceElements.length == 0) {
      xLog.e(tag, "| Thread StackTraceElements is null !!!");
    } else {
      formatStackTrace(stackTraceElements, filterPackageSet, filterClassName, className, tag, true, false);
    }
    xLog.e(tag, "|---------------------------------------------------------");
  }

  static String string(StackTraceElement[] stackTraceElements) {
    return string(stackTraceElements, filterPackageSet, false, "");
  }

  static String string(StackTraceElement[] stackTraceElements, boolean filterClassName, String className) {
    return string(stackTraceElements, filterPackageSet, filterClassName, className);
  }

  static String string(StackTraceElement[] stackTraceElements, String[] filterPackageSet, boolean filterClassName,
      String className) {
    return formatStackTrace(stackTraceElements, filterPackageSet, filterClassName, className,
        pTool.TAG + "_stack_string", false, true
    );
  }

  private static String formatStackTrace(StackTraceElement[] stackTraceElements, String[] filterPackageSet,
      boolean filterClassName, String className, String tag, boolean printMode, boolean stringMode) {
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
        String token =
            String.format(STACK_TRACE_FORMAT, element.getClassName(), element.getMethodName(), element.getFileName(),
                element.getLineNumber()
            );
        xLog.e(tag, token);
      }
      if (stringMode) {
        stringBuilder.append(element.getClassName())
            .append('.')
            .append(element.getMethodName())
            .append('(')
            .append(element.getFileName())
            .append(':')
            .append(element.getLineNumber())
            .append(')');
        if (i + 1 < stackTraceElements.length) {
          stringBuilder.append(" <- ");
        }
      }
    }
    return stringBuilder.toString();
  }

  public static void print(String tag, String type) {
    print(tag, type, Thread.currentThread().getStackTrace(), true, StackTraceUtils.class.getName(), true,
        ignorePackageSet
    );
  }

  public static void print(String tag, String type, boolean filterPoint, boolean filterPackage) {
    print(tag, type, Thread.currentThread().getStackTrace(), filterPoint, StackTraceUtils.class.getName(),
        filterPackage, ignorePackageSet
    );
  }

  public static void print(String tag, String type, boolean filterPoint, boolean filterPackage,
      Set<String> ignorePackageSet) {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    print(tag, type, stackTraceElements, filterPoint, StackTraceUtils.class.getName(), filterPackage, ignorePackageSet);
  }


  public static void print(String tag, String type, StackTraceElement[] stackTraceElements, boolean filterPoint,
      String pointClassName, boolean filterPackage, Set<String> ignorePackageSet) {
    List<String> list = list(stackTraceElements, filterPoint, pointClassName, filterPackage, ignorePackageSet);
    for (int i = 0, len = list.size(); i < len; i++) {
      xLog.w(tag, list.get(i));
    }
    xLog.w(tag, type + " end ============================================");
  }

  public static List<String> list() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    return list(stackTraceElements, true, StackTraceUtils.class.getName(), true, ignorePackageSet);
  }

  public static List<String> list(StackTraceElement[] stackTraceElements, boolean filterPoint, String pointClassName) {
    return list(stackTraceElements, filterPoint, pointClassName, true, ignorePackageSet);
  }

  public static List<String> list(StackTraceElement[] stackTraceElements, boolean filterPoint, String pointClassName,
      boolean filterPackage, Set<String> ignorePackageSet) {
    List<String> list = new ArrayList<>(stackTraceElements.length);
    StringBuilder stringBuilder = new StringBuilder();
    boolean hasFindPointClass = !filterPoint;
    for (int i = 0, len = stackTraceElements.length; i < len; i++) {
      StackTraceElement element = stackTraceElements[i];
      // 看 element 是否需要跳过，应该有些 element 是框架的栈，可以跳过
      if (!hasFindPointClass) {
        // 对比是否是 point class, point class 表示是框架切入的点，
        // 这个切入的点和之前的点都可以跳过，因为是框架调用，开发者不应该看到
        if (!pointClassName.equals(element.getClassName())) {
          continue;
        }
        // 第一次找到 point class ，标记下，下次就不用再找了
        hasFindPointClass = true;
        continue;
      }
      String parentClassOrSelf = element.getClassName().split("$")[0];
      int packageEndIndex = parentClassOrSelf.lastIndexOf('.');
      String packageName = parentClassOrSelf.substring(0, packageEndIndex);
      if (filterPackage && ignorePackageSet.contains(packageName)) {
        // ignorePackageSet 表示忽略的包名，有些包名是框架库里面的方法调用，可以忽略掉
        // 因为不影响开发者
        continue;
      }
      list.add(stringStackTraceElement(stackTraceElements[i], stringBuilder));
    }
    return list;
  }

  private static String stringStackTraceElement(StackTraceElement element, StringBuilder stringBuilder) {
    stringBuilder.delete(0, stringBuilder.length());
    stringBuilder.append('\t')
        .append(element.getClassName())
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
