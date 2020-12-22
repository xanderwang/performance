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
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    return list(stackTraceElements, true, StackTraceUtils.class.getName(), true, ignorePackageSet);
  }

  public static List<String> list(StackTraceElement[] stackTraceElements, boolean filterPoint,
      String pointClassName) {
    return list(stackTraceElements, filterPoint, pointClassName, true, ignorePackageSet);
  }

  public static List<String> list(StackTraceElement[] stackTraceElements, boolean filterPoint,
      String pointClassName, boolean filterPackage, Set<String> ignorePackageSet) {
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
      String packageName = filterPackageName(stringBuilder, element.getClassName());
      if (filterPackage && ignorePackageSet.contains(packageName)) {
        // ignorePackageSet 表示忽略的包名，有些包名是框架库里面的方法调用，可以忽略掉
        // 因为不影响开发者
        continue;
      }
      list.add(stringStackTraceElement(stackTraceElements[i], stringBuilder));
    }
    return list;
  }

  private static String filterPackageName(String className) {
    return filterPackageName(new StringBuilder(), className);
  }

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
