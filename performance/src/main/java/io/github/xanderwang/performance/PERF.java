package io.github.xanderwang.performance;

import android.util.Log;

import java.io.File;

import io.github.xanderwang.asu.aConstants;
import me.weishu.reflection.Reflection;

public class PERF {

  static String TAG = "PERF";

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
    long mUIBlockTime = Config.UI_BLOCK_TIME;

    /**
     * 检测线程的 start 方法调用栈
     */
    boolean mCheckThread = false;

    /**
     * 线程 block 的时间间隔，超过了表示后台执行的任务太多了，需要注意。
     */
    long mThreadBlockTime = Config.THREAD_BLOCK_TIME;

    /**
     * UI 线程的检测触发时间间隔，超过时间间隔，会被认为发生了 block
     */
    long mFPSIntervalTime = Config.FPS_INTERVAL_TIME;

    /**
     * 是否检测 fps
     */
    boolean mCheckFPS = false;

    /**
     * 是否需要检测 ipc， 也就是进程间通讯
     */
    boolean mCheckIPC = false;

    /**
     * 是否需要检测 Bitmap 的创建
     */
    boolean mCheckBitmap = false;

    /**
     * 是否需要在打印 log 的时候过滤一些不必要的类名。
     */
    boolean mFilterClass = true;

    /**
     * issue 文件的保存目录
     */
    IssueSupplier<File> cacheDirSupplier = null;

    /**
     * issue 缓存最大的目录大小
     */
    IssueSupplier<Integer> macCacheSizeSupplier = null;

    /**
     * log file 上传器
     */
    IssueSupplier<LogFileUploader> uploaderSupplier = null;

    /**
     * 全局的 log tag
     */
    String globalTag = TAG;

    public Builder checkUI(boolean check) {
      mCheckUI = check;
      return this;
    }

    public Builder checkUI(boolean check, long blockTime) {
      mCheckUI = check;
      mUIBlockTime = blockTime;
      return this;
    }

    public Builder checkThread(boolean check) {
      mCheckThread = check;
      return this;
    }

    public Builder checkThread(boolean check, long threadBlockTime) {
      mCheckThread = check;
      mThreadBlockTime = threadBlockTime;
      return this;
    }

    public Builder checkFps(boolean check) {
      mCheckFPS = check;
      return this;
    }

    public Builder checkFps(boolean check, long fpsIntervalTime) {
      mCheckFPS = check;
      mFPSIntervalTime = fpsIntervalTime;
      return this;
    }

    public Builder checkIPC(boolean check) {
      mCheckIPC = check;
      return this;
    }

    public Builder checkBitmap(boolean check) {
      mCheckBitmap = check;
      return this;
    }

    public Builder filterClass(boolean filter) {
      mFilterClass = filter;
      return this;
    }

    public Builder globalTag(String tag) {
      globalTag = tag;
      return this;
    }

    public Builder cacheDirSupplier(IssueSupplier<File> cache) {
      cacheDirSupplier = cache;
      return this;
    }

    public Builder maxCacheSizeSupplier(IssueSupplier<Integer> cacheSize) {
      macCacheSizeSupplier = cacheSize;
      return this;
    }

    public Builder uploaderSupplier(IssueSupplier<LogFileUploader> uploader) {
      uploaderSupplier = uploader;
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

  public interface IssueSupplier<T> {
    T get();
  }

  public interface LogFileUploader {
    boolean upload(File logFile);
  }

  public static void init(Builder builder) {
    Reflection.unseal(AppHelper.appContext());
    if (builder == null) {
      builder = new Builder();
    }
    Config.FILTER_CLASS_NAME = builder.mFilterClass;
    aConstants.logLevel = builder.logLevel;
    aConstants.setGlobalTag(builder.globalTag);
    Issue.init(builder.cacheDirSupplier, builder.macCacheSizeSupplier, builder.uploaderSupplier);
    if (builder.mCheckThread) {
      Config.THREAD_BLOCK_TIME = builder.mThreadBlockTime;
      ThreadTool.init();
    }
    if (builder.mCheckUI) {
      Config.UI_BLOCK_TIME = builder.mUIBlockTime;
      UIBlockTool.start();
    }
    if (builder.mCheckIPC) {
      IPCTool.start();
    }
    if (builder.mCheckFPS) {
      Config.FPS_INTERVAL_TIME = builder.mFPSIntervalTime;
      FPSTool.start();
    }
    if (builder.mCheckBitmap) {
      BitmapTool.start();
    }
  }

}
