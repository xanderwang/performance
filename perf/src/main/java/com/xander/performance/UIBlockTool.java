package com.xander.performance;

import android.os.Handler;
import android.os.Looper;
import android.util.Printer;
import android.view.KeyEvent;

import com.xander.asu.aLog;
import com.xander.performance.hook.HookBridge;
import com.xander.performance.hook.core.MethodParam;
import com.xander.performance.hook.core.MethodHook;


/**
 * @author xander
 * <p>
 * 在触发 UI 线程操作的时候，同时在后台线程启动一个延时的任务，到了时间，延时任务还没有被移除，
 * <p>
 * 说明 UI 线程 block 了，此时打印出 UI 线程的状态，如果需要详细的状态，甚至可以
 * <p>
 * 打印出 cpu 和内存的相关信息和状态，目前只是 block 的时候，打印了 UI 线程的调用链。
 */
class UIBlockTool {

  private static final String TAG = "UIBlockTool";

  static void start() {
    aLog.e(TAG, "start");
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

  private static DumpInfoThread   dumpInfoThread         = new DumpInfoThread("DumpInfoThread");
  private static Handler          dumpInfoHandler;
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

  /**
   * 后台异步 dump info 线程，利用 Looper 机制，做延时任务处理。
   */
  private static class DumpInfoThread extends Thread {
    DumpInfoThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      Looper.prepare();
      dumpInfoHandler = new Handler(Looper.myLooper());
      Looper.loop();
    }
  }

  /**
   * dump 信息，目前主要 dump 主线程的调用栈，后续可以考虑 dump 更多的信息。
   */
  private static class DumpInfoRunnable implements Runnable {
    @Override
    public void run() {
      Issue uiIssue = new Issue(
          Issue.TYPE_UI_BLOCK,
          "UI BLOCK",
          StackTraceUtils.list(Looper.getMainLooper().getThread())
      );
      uiIssue.print();
    }
  }

  /**
   * 如果是 TV 设备，用遥控或者键盘操作的话，不会走 Looper loop Message 方法
   * <p>
   * 而是会走 InputManager 里面的方法，InputManager 最终调用 DecorView 的 dispatchKeyEvent 方法
   * <p>
   * 这里通过 epic 库来 hook DecorView 的 dispatchKeyEvent 方法的来监控 UI block
   */
  private static void hookDecorViewDispatchKeyEvent() {
    try {
      Class decorViewClass = Class.forName("com.android.internal.policy.DecorView");
      HookBridge.findAndHookMethod(
          decorViewClass,
          "dispatchKeyEvent",
          KeyEvent.class,
          new DecorViewDispatchKeyEventHook()
      );
    } catch (Exception e) {
      aLog.e(TAG, "hookDecorViewDispatchKeyEvent", e);
    }
  }

  private static class DecorViewDispatchKeyEventHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      // super.beforeHookedMethod(param);
      startDumpInfo();
    }

    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      // super.afterHookedMethod(param);
      clearDumpInfo();
    }
  }

}
