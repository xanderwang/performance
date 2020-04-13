package com.xander.performance.tool;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

/**
 * @author xander
 *
 * 通过向 main thread 里面放一个指定的 runnable ,然后定时去查看是否被运行过
 */
public class UiTool {

  private static final String TAG = "UiTool";

  private static volatile CheckUiThread checkUiThread;

  public static void start() {
    // 不严谨，后续需要优化。
    if (null != checkUiThread) {
      return;
    }
    // 后台检测线程
    checkUiThread = new CheckUiThread("check-ui");
    checkUiThread.start();
  }

  public static void stop() {
    if (null != checkUiThread && !checkUiThread.isInterrupted()) {
      checkUiThread.interrupt();
    }
    checkUiThread = null;
  }

  static String STACK_LOG_FORMAT = "\t%s.%s:%s";

  /**
   * 打印指定线程的方法调用栈
   */
  private static void printThreadStackTrace(Thread thread) {
    if (null == thread) {
      return;
    }
    StackTraceElement[] stacks = thread.getStackTrace();
    // 没有执行完，说明 ui 线程阻塞了，打印方法堆栈
    for (int i = 0; i < stacks.length; i++) {
      Log.e(TAG,
        String.format(STACK_LOG_FORMAT, stacks[i].getClassName(), stacks[i].getMethodName(),
          stacks[i].getLineNumber()
        )
      );
    }
  }

  /**
   * 检测 main thread 的 Thread ,在后台指定的时间
   */
  static class CheckUiThread extends Thread {

    MainThreadRunnable uiRunnable = new MainThreadRunnable();
    @SuppressLint("HandlerLeak") Handler checkHandler = new Handler() { };

    public CheckUiThread(String s) {
      super(s);
    }

    @Override public void run() {
      while (true) {
        try {
          Thread.sleep(PerformanceConfig.UI_CHECK_TIME);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        Log.e(TAG, "---------- start check ui thread ----------");
        if (!uiRunnable.done) {
          printThreadStackTrace(checkHandler.getLooper().getThread());
        }
        // 正常执行完或者打印完线程调用栈，开始下一个计时检测任务。
        uiRunnable.reset();
        checkHandler.post(uiRunnable);
      }
    }
  }

  /**
   * main thread 里面执行的 runnable ，执行完标记值为 true ，
   * 否则为 false ，通过后台线程持续检测标记值可以知道
   * main thread 是否有阻塞。
   */
  static class MainThreadRunnable implements Runnable {

    boolean done = false;

    @Override public void run() {
      done = true;
    }

    void reset() {
      done = false;
    }
  }
}
