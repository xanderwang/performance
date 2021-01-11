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
class UIBlockTool {

  private static String TAG = PERF.TAG + "_UIBlockTool";

  static void resetTag(String tag) {
    TAG = tag + "_UIBlockTool";
  }

  static void start() {
    xLog.e(TAG, "start");
    startDumpInfoThread();
    hookDecorViewDispatchKeyEvent();
    initMainLooperPrinter();
  }

  private static void startDumpInfoThread() {
    dumpInfoThread.start();
  }

  private static void initMainLooperPrinter() {
    Looper.getMainLooper().setMessageLogging(new WatcherMainLooperPrinter());
  }

  private static void startDumpInfo() {
    if (null == dumpInfoHandler) {
      return;
    }
    dumpInfoHandler.removeCallbacks(dumpMainThreadRunnable);
    dumpInfoHandler.postDelayed(dumpMainThreadRunnable, Config.UI_BLOCK_INTERVAL_TIME);
  }

  private static void clearDumpInfo() {
    if (null == dumpInfoHandler) {
      return;
    }
    dumpInfoHandler.removeCallbacks(dumpMainThreadRunnable);
  }

  private static DumpInfoThread dumpInfoThread = new DumpInfoThread("DumpInfoThread");
  private static Handler dumpInfoHandler;
  private static DumpInfoRunnable dumpMainThreadRunnable = new DumpInfoRunnable();

  private static class WatcherMainLooperPrinter implements Printer {
    @Override
    public void println(String x) {
      if (x != null && x.startsWith(">>>>>")) {
        startDumpInfo();
      } else if (x != null && x.startsWith("<<<<<")) {
        clearDumpInfo();
      }
    }
  }

  private static class DumpInfoThread extends Thread {
    DumpInfoThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      Looper.prepare();
      dumpInfoHandler = new Handler(Looper.myLooper());
      Looper.loop();
      super.run();
    }
  }

  /**
   * dump 信息，目前主要 dump 主现场调用栈
   */
  private static class DumpInfoRunnable implements Runnable {
    @Override
    public void run() {
      Issue uiIssue = new Issue(Issue.TYPE_UI_BLOCK,
          "UI BLOCK",
          StackTraceUtils.list(Looper.getMainLooper().getThread())
      );
      uiIssue.print();
    }
  }

  /**
   * 如果是 TV 设备，用遥控或者键盘操作的话，不会走到 Handler 的 dispatchMessage 方法
   * <p>
   * 而是会走 InputManager 里面的方法，最终调用 DecorView 的DispatchKeyEvent 方法
   * <p>
   * 这里通过 hook 来切入这个 KeyEvent 事件
   */
  private static void hookDecorViewDispatchKeyEvent() {
    try {
      Class decorViewClass = Class.forName("com.android.internal.policy.DecorView");
      DexposedBridge.findAndHookMethod(decorViewClass,
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
      startDumpInfo();
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      clearDumpInfo();
    }
  }

}
