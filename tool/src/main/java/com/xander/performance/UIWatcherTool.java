package com.xander.performance;

import android.os.Handler;
import android.os.Looper;
import android.util.Printer;
import android.view.KeyEvent;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

/**
 * @author xander
 * <p>
 * 在触发 ui 线程操作的时候，同时在后台线程启动一个延时的任务，到了时间，延时任务还没有被移除，
 * <p>
 * 说明 ui 线程 block 了，此时打印出 ui 线程的状态，如果需要详细的状态，甚至可以
 * <p>
 * 打印处俩 cpu 和内存的相关状态
 */
public class UIWatcherTool {

  private static String TAG = pTool.TAG + "_UIWatcherTool";

  static void resetTag(String tag) {
    TAG = tag + "_UIWatcherTool";
  }

  static void start() {
    xLog.e(TAG, "start");
    hookDecorViewDispatchKeyEvent();
    hookLooperPrint();
    startWatchThread();
  }

  private static void startWatchThread() {
    watcherThread.start();
  }

  private static void hookLooperPrint() {
    Looper.getMainLooper().setMessageLogging(new WatcherPrinter());
  }

  private static void tryDumpMainThread() {
    if (null == watchThreadHandler) {
      return;
    }
    watchThreadHandler.removeCallbacks(dumpMainThreadRunnable);
    watchThreadHandler.postDelayed(
        dumpMainThreadRunnable,
        PerformanceConfig.WATCH_UI_INTERVAL_TIME
    );
  }

  private static void clearDumpMainThread() {
    if (null == watchThreadHandler) {
      return;
    }
    watchThreadHandler.removeCallbacks(dumpMainThreadRunnable);
  }

  private static WatcherThread watcherThread = new WatcherThread("WatcherThread");
  private static Handler watchThreadHandler;
  private static DumpInfoRunnable dumpMainThreadRunnable = new DumpInfoRunnable();

  private static class WatcherPrinter implements Printer {
    @Override
    public void println(String x) {
      if (x != null && x.startsWith(">>>>>")) {
        tryDumpMainThread();
      } else if (x != null && x.startsWith("<<<<<")) {
        clearDumpMainThread();
      }
    }
  }

  private static class WatcherThread extends Thread {
    WatcherThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      Looper.prepare();
      watchThreadHandler = new Handler(Looper.myLooper());
      Looper.loop();
      super.run();
    }
  }

  private static class DumpInfoRunnable implements Runnable {
    @Override
    public void run() {
      Issue uiIssue = new Issue(
          Issue.TYPE_UI_BLOCK,
          "UI BLOCK",
          StackTraceUtils.list(Looper.getMainLooper().getThread().getStackTrace(), false, "")
      );
      uiIssue.print();
    }
  }

  private static void hookDecorViewDispatchKeyEvent() {
    try {
      Class decorViewClass = Class.forName("com.android.internal.policy.DecorView");
      DexposedBridge.findAndHookMethod(
          decorViewClass,
          "dispatchKeyEvent",
          KeyEvent.class,
          new DecorViewDispatchKeyEventHook()
      );
    } catch (Exception e) {
      e.printStackTrace();
      xLog.e(TAG, "hookDecorViewDispatchKeyEvent", e);
    }
  }

  static class DecorViewDispatchKeyEventHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      tryDumpMainThread();
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      clearDumpMainThread();
    }
  }

}
