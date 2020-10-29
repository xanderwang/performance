package com.xander.performance.tool;

import android.content.Context;
import me.weishu.reflection.Reflection;

public class pTool {

  public static final String TAG = "pTool";

  public static void startPerformance(Context context) {
    startPerformance(context, PerformanceConfig.ANR_CHECK_TIME);
  }

  public static void startPerformance(Context context, long anrCheckTime) {
    Reflection.unseal(context);
    PerformanceConfig.ANR_CHECK_TIME = anrCheckTime;
    startCheckThread();
    startCheckANR();
    startDumpTool();
  }

  private static void startCheckANR() {
    ANRTool.start();
  }

  private static void startCheckThread() {
    ThreadTool.init();
  }

  private static void startDumpTool() {
    DumpTool.init("dumptool");
  }


  static String STACK_LOG_FORMAT = "%s.%s():%s";

  static String[] LIB_PACKAGE_NAMES = {
      pTool.class.getName(),
      "com.swift.sandhook",
      "me.weishu.epic",
      "com.taobao.android.dexposed",
      "de.robv.android.xposed"
  };

  /**
   * 打印指定线程的方法调用栈
   */
  static void printThreadStackTrace(String tag, Thread thread) {
    printThreadStackTrace(tag, thread, true, "");
  }

  /**
   * @param tag
   * @param thread    需要打印的线程
   * @param allTrace  是否打印完整的 log
   * @param skipToken 过滤框架之前的方法栈的打印 allTrace 为 false 的时候才生效
   */
  static void printThreadStackTrace(String tag, Thread thread, boolean allTrace, String skipToken) {
    if (null == thread) {
      xLog.e(tag, "null thread!!!");
      return;
    }
    xLog.e(tag, "=======================printThreadStackTrace===========================");
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
        xLog.e(tag, token);
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
      xLog.e(tag, token);
    }
  }

}
