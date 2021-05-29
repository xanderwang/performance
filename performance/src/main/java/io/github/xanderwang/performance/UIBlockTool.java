package io.github.xanderwang.performance;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Printer;
import android.view.KeyEvent;

import io.github.xanderwang.asu.aLog;
import io.github.xanderwang.hook.HookBridge;
import io.github.xanderwang.hook.core.MethodParam;
import io.github.xanderwang.hook.core.MethodHook;


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

  /**
   * 用于发起定时任务
   */
  private static Handler uiBlockHandler;

  /**
   * 用于 dump block 时候，系统相关的信息。
   */
  private static final DumpBlockInfoRunnable dumpBlockInfoRunnable = new DumpBlockInfoRunnable();

  static void start() {
    aLog.e(TAG, "start");
    uiBlockHandler = new Handler(AppHelper.getPerfLooper());
    // hookLooperPrinter();
    hookHandlerDispatchMessage();
    hookDecorViewDispatchKeyEvent();
  }


  private static void startDumpInfo() {
    if (null == uiBlockHandler) {
      return;
    }
    uiBlockHandler.removeCallbacks(dumpBlockInfoRunnable);
    uiBlockHandler.postDelayed(dumpBlockInfoRunnable, Config.UI_BLOCK_TIME);
  }

  private static void clearDumpInfo() {
    if (null == uiBlockHandler) {
      return;
    }
    uiBlockHandler.removeCallbacks(dumpBlockInfoRunnable);
  }


  /**
   * dump 信息，目前主要 dump 主线程的调用栈，后续可以考虑 dump 更多的信息。
   */
  private static class DumpBlockInfoRunnable implements Runnable {
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

  public static void hookLooperPrinter() {
    Looper.getMainLooper().setMessageLogging(new WatcherMainLooperPrinter());
  }

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

  private static void hookHandlerDispatchMessage() {
    HookBridge.findAndHookMethod(
        Handler.class,
        "dispatchMessage",
        Message.class,
        new HandlerDispatchMessageHook()
    );
  }

  private static class HandlerDispatchMessageHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      // super.beforeHookedMethod(param);
      if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
        startDumpInfo();
      }
    }

    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      // super.afterHookedMethod(param);
      if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
        clearDumpInfo();
      }
    }
  }

  /**
   * 如果是 TV 设备，用遥控或者键盘操作的话，不会走 Looper loop Message 方法
   * <p>
   * 而是会走 InputManager 里面的方法，InputManager 最终调用 DecorView 的 dispatchKeyEvent 方法
   * <p>
   * 这里通过 epic 库来 hook DecorView 的 dispatchKeyEvent 方法的来监控 UI block
   */
  @SuppressLint("PrivateApi")
  private static void hookDecorViewDispatchKeyEvent() {
    try {
      Class<?> decorViewClass = Class.forName("com.android.internal.policy.DecorView");
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
