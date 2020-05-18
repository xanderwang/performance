package com.xander.performance.tool;

import android.util.Log;

public class pTool {

  public static final String TAG = "pTool";

  public static void startPerformance() {
    startPerformance(PerformanceConfig.ANR_CHECK_TIME);
  }

  public static void startPerformance(long anrCheckTime) {
    PerformanceConfig.ANR_CHECK_TIME = anrCheckTime;
    startCheckThread();
    startCheckANR();
  }

  private static void startCheckANR() {
    ANRTool.start();
  }

  private static void startCheckThread() {
    ThreadTool.init();
  }


  static String STACK_LOG_FORMAT = "%s.%s():%s";

  static String[] LIB_PACKAGE_NAMES = {
      pTool.class.getName(),
      "me.weishu.epic",
      "com.taobao.android.dexposed"
  };

  /**
   * 打印指定线程的方法调用栈
   */
  static void printThreadStackTrace(String tag, Thread thread) {
    printThreadStackTrace(tag, thread, true,"");
  }

  /**
   *
   * @param tag
   * @param thread 需要打印的线程
   * @param allTrace 是否打印完整的 log
   * @param skipToken 过滤框架之前的方法栈的打印 allTrace 为 false 的时候才生效
   */
  static void printThreadStackTrace(String tag, Thread thread, boolean allTrace, String skipToken) {
    if (null == thread) {
      Log.e(TAG, "null thread!!!");
      return;
    }
    tag = TAG + "-" + tag;
    boolean findSkipToken = false;
    StackTraceElement[] stacks = thread.getStackTrace();
    // 没有执行完，说明 ui 线程阻塞了，打印方法堆栈
    for (int i = 0; i < stacks.length; i++) {
      String token = String.format(
          STACK_LOG_FORMAT,
          stacks[i].getClassName(),
          stacks[i].getMethodName(),
          stacks[i].getLineNumber()
      );
      if (allTrace) {
        Log.e(tag, token);
        continue;
      }
      if (!findSkipToken) {
        // 不是完整打印，并且没有找到 token 先找一下当前是否是 token
        if (token.startsWith(skipToken)) {
          // 找到了，跳过本次，从下次开始打印
          findSkipToken = true;
        }
        continue;
      }
      boolean needContinue = false;
      for (int m = 0; m < LIB_PACKAGE_NAMES.length; m++) {
        if (token.startsWith(LIB_PACKAGE_NAMES[m])) {
          needContinue = true;
          break;
        }
      }
      if (needContinue) {
        continue;
      }
      Log.e(tag, token);
    }
  }

}
