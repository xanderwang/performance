package com.xander.performance;

import android.content.Context;

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
    boolean mCheckUI = true;
    /**
     * ANR 的触发时间
     */
    long mCheckUIThreadTime = 300;
    /**
     * 检测线程的 start 方法调用栈
     */
    boolean mCheckThread = true;
    /**
     * 是否检测 fps
     */
    boolean mCheckFPS = true;

    /**
     * 是否需要检测 ipc， 也就是进程间通讯
     */
    boolean mCheckIPC = false;

    Context appContext = null;

    long mHandlerCheckTime = 0;

    String globalTag = TAG;

    public Builder checkUIThread(boolean check) {
      return checkUIThread(check, 5000);
    }

    public Builder checkUIThread(boolean check, long time) {
      mCheckUI = check;
      mCheckUIThreadTime = time;
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

    public Builder checkHandlerCostTime(long maxCostTime) {
      mHandlerCheckTime = maxCostTime;
      return this;
    }

    public Builder globalTag(String tag) {
      globalTag = tag;
      return this;
    }

    public Builder appContext(Context context) {
      appContext = context;
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
    Issue.init(builder.appContext);
    DumpTool.resetTag(TAG);
    FPSTool.resetTag(TAG);
    IPCTool.resetTag(TAG);
    UIWatcherTool.resetTag(TAG);
    PerformanceHandler.resetTag(TAG);
    ThreadTool.resetTag(TAG);
    if (builder.mCheckThread) {
      ThreadTool.init();
    }
    if (builder.mCheckUI) {
      PerformanceConfig.CHECK_UI_THREAD_TIME = builder.mCheckUIThreadTime;
      UIWatcherTool.start();
    }
    if (builder.mCheckFPS) {
      FPSTool.start();
    }
    if (builder.mCheckIPC) {
      IPCTool.start();
    }
    PerformanceConfig.HANDLER_CHECK_TIME = builder.mHandlerCheckTime;
  }

}
