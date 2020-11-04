package com.xander.performance;

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
    if (builder.mCheckFPS) {
      FPSTool.start();
    }
    if (builder.mCheckIPC) {
      IPCTool.start();
    }
    if (builder.mCheckHandler) {
      PerformanceConfig.HANDLER_CHECK_TIME = builder.mHandlerCheckTime;
      HandlerTool.start();
    }
  }


  /**
   * 打印指定线程的方法调用栈
   */
  static void printThreadStackTrace(String tag, Thread thread, String traceName) {
    printThreadStackTrace(tag, thread, traceName, false, "");
  }

  /**
   * @param tag
   * @param thread    需要打印的线程
   * @param filterClassName  是否打印完整的 log
   * @param className 过滤框架之前的方法栈的打印 filterClassName 为 false 的时候才生效
   */
  static void printThreadStackTrace(String tag, Thread thread, String traceName, boolean filterClassName, String className) {
    xLog.e(tag, "|==================  " + traceName + "  ==================");
    if (null != thread) {
      StackTraceUtils.print(tag, thread, filterClassName, className);
    } else {
      xLog.e(tag, "| thread is null !!!");
    }
    xLog.e(tag, "|---------------------------------------------------------");
  }

}
