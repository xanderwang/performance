package com.xander.performance;

import android.nfc.cardemulation.HostApduService;

public class pTool {

  public static final String TAG = "pTool";

  public static class Builder {
    /**
     * 是否开启 debug 模式，这种模式下会打印相对多的 log
     */
    boolean debug = true;
    /**
     * 是否开启检测 ANR
     */
    boolean mCheckANR = true;
    /**
     * ANR 的触发时间
     */
    long mAnrCheckTime = 5000;
    /**
     * 检测线程的 start 方法调用栈
     */
    boolean mCheckThread = true;
    /**
     * 是否检测 fps
     */
    boolean mCheckFPS = true;

    boolean mCheckIPC = true;

    boolean mCheckHandler = true;
    long mHandlerCheckTime = 0;

    public Builder checkANR(boolean check) {
      mCheckANR = check;
      return this;
    }

    public Builder checkANR(boolean check, long time) {
      mCheckANR = check;
      mAnrCheckTime = time;
      return this;
    }

    public Builder checkThread(boolean check) {
      mCheckThread = check;
      return this;
    }

    public Builder checkFps(boolean check) {
      mCheckFPS = check;
      return this;
    }

    public Builder checkIPC(boolean check) {
      mCheckIPC = check;
      return this;
    }

    public Builder checkHandler(boolean check, long time) {
      mCheckHandler = check;
      mHandlerCheckTime = time;
      return this;
    }

    public Builder build() {
      return this;
    }

  }


  public static void init(Builder builder) {
    if (builder == null) {
      builder = new Builder();
    }
    if (builder.mCheckThread) {
      ThreadTool.init();
    }
    if (builder.mCheckANR) {
      PerformanceConfig.ANR_CHECK_TIME = builder.mAnrCheckTime;
      ANRTool.start();
    }
    if( builder.mCheckFPS ) {
      FPSTool.start();
    }
    if( builder.mCheckIPC ) {
      IPCTool.start();
    }
    if( builder.mCheckHandler ) {
      PerformanceConfig.HANDLER_CHECK_TIME = builder.mHandlerCheckTime;
      HandlerTool.start();
    }
  }


  static String STACK_LOG_FORMAT = "|\t\t%s.%s():%s";

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
  static void printThreadStackTrace(String tag, Thread thread, String traceName) {
    printThreadStackTrace(tag, thread, traceName, true, "");
  }

  /**
   * @param tag
   * @param thread    需要打印的线程
   * @param allTrace  是否打印完整的 log
   * @param skipToken 过滤框架之前的方法栈的打印 allTrace 为 false 的时候才生效
   */
  static void printThreadStackTrace(String tag, Thread thread, String traceName, boolean allTrace, String skipToken) {
    if (null == thread) {
      xLog.e(tag, "null thread!!!");
      return;
    }
    xLog.e(tag, "======================= "+ traceName +"  ==========================");
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
        if (token.contains(skipToken)) {
          // 找到了，跳过本次，从下次开始打印
          findSkipToken = true;
        }
        continue;
      }
      boolean needContinue = false;
      for (int m = 0; m < LIB_PACKAGE_NAMES.length; m++) {
        if (token.contains(LIB_PACKAGE_NAMES[m])) {
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
