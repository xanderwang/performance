package com.xander.performance;

import android.content.Context;
import android.util.Log;

public class pTool {

  static String TAG = "pTool";

  public static class Builder {
    /**
     * logLevel ，设置可以打印的 log 等级
     */
    int logLevel = Log.DEBUG;
    /**
     * 是否开启检测 UI 线程
     */
    boolean mCheckUI = false;
    /**
     * UI 线程的检测触发时间间隔，超过时间间隔，会被认为发生了 block
     */
    long mUIBlockIntervalTime = PerformanceConfig.UI_BLOCK_INTERVAL_TIME;
    /**
     * 检测线程的 start 方法调用栈
     */
    boolean mCheckThread = false;
    /**
     * 是否检测 fps
     */
    boolean mCheckFPS = false;
    /**
     * 是否需要检测 ipc， 也就是进程间通讯
     */
    boolean mCheckIPC = false;
    /**
     * 上下文，用于获取保存文件夹路径
     */
    Context appContext = null;

    String globalTag = TAG;

    public Builder checkUI(boolean check) {
      return checkUI(check, PerformanceConfig.UI_BLOCK_INTERVAL_TIME);
    }

    public Builder checkUI(boolean check, long blockIntervalTime) {
      mCheckUI = check;
      mUIBlockIntervalTime = blockIntervalTime;
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

    public Builder globalTag(String tag) {
      globalTag = tag;
      return this;
    }

    public Builder appContext(Context context) {
      appContext = context;
      return this;
    }

    public Builder logLevel(int level) {
      logLevel = level;
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
    xLog.setLogLevel(builder.logLevel);
    // Logger.setLogLevel(builder.logLevel);
    TAG = builder.globalTag;
    ThreadTool.resetTag(TAG);
    DumpTool.resetTag(TAG);
    FPSTool.resetTag(TAG);
    IPCTool.resetTag(TAG);
    HandlerTool.resetTag(TAG);
    UIBlockTool.resetTag(TAG);
    PerformanceHandler.resetTag(TAG);
    Issue.resetTag(TAG);
    Issue.init(builder.appContext);
    if (builder.mCheckThread) {
      ThreadTool.init();
    }
    if (builder.mCheckUI) {
      PerformanceConfig.UI_BLOCK_INTERVAL_TIME = builder.mUIBlockIntervalTime;
      UIBlockTool.start();
    }
    if (builder.mCheckFPS) {
      FPSTool.start();
    }
    if (builder.mCheckIPC) {
      IPCTool.start();
    }
  }

}
