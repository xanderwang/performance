package com.xander.performance;

public class pTool {

  static String TAG = "pTool";

  public static class Builder {
    /**
     * 是否开启 debug 模式，这种模式下会打印相对多的 log
     */
    boolean debug = false;
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

    boolean mCheckIPC = false;

    boolean mCheckHandler = false;

    long mHandlerCheckTime = 0;

    String globalTag = "pTool";

    public Builder checkANR(boolean check) {
      return checkANR(check, 5000);
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

    @Deprecated
    public Builder checkHandler(boolean check, long time) {
      mCheckHandler = check;
      mHandlerCheckTime = time;
      return this;
    }

    public Builder globalTag(String tag) {
      globalTag = tag;
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
    TAG = builder.globalTag;
    if (builder.mCheckThread) {
      ThreadTool.init();
    }
    if (builder.mCheckANR) {
      PerformanceConfig.ANR_CHECK_TIME = builder.mAnrCheckTime;
      MainThreadTool.start();
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

}
