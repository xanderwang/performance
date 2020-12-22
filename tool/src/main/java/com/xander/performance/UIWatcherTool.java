package com.xander.performance;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * @author xander
 * <p>
 * 通过向 main thread 里面放一个指定的 runnable 然后定时去查看是否被运行过来检测是否有 ANR
 */
public class UIWatcherTool {

  private static String TAG = pTool.TAG + "_UIWatcherTool";

  private static volatile UIWatcherThread UIWatcherThread;

  static void resetTag(String tag) {
    TAG = tag + "_UIWatcherTool";
  }

  static void start() {
    xLog.e(TAG, "start");
    // 不严谨，后续需要优化。
    if (null != UIWatcherThread) {
      return;
    }
    // 后台检测线程
    UIWatcherThread = new UIWatcherThread("ui-watcher-thread");
    UIWatcherThread.start();
  }

  @Deprecated
  static void stop() {
    if (null != UIWatcherThread && !UIWatcherThread.isInterrupted()) {
      UIWatcherThread.interrupt();
    }
    UIWatcherThread = null;
  }


  /**
   * 检测 main thread 的 Thread ,在后台指定的时间
   */
  static class UIWatcherThread extends Thread {

    UIWatcherRunnable uiRunnable = new UIWatcherRunnable();

    Handler mainThreadHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.obj instanceof UIWatcherRunnable) {
          ((UIWatcherRunnable) msg.obj).run();
        }
      }
    };

    public UIWatcherThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(PerformanceConfig.CHECK_UI_THREAD_TIME);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (!uiRunnable.done) {
          Issue uiIssue = new Issue(
              Issue.TYPE_ANR,
              "UI WATCHER",
              StackTraceUtils.list(Looper.getMainLooper().getThread().getStackTrace(), false, "")
          );
          uiIssue.print();
        }
        // 正常执行完或者打印完线程调用栈，开始下一个计时检测任务。
        uiRunnable.reset();
        Message msg = new Message();
        msg.obj = uiRunnable;
        mainThreadHandler.sendMessage(msg);
      }
    }
  }

  /**
   * main thread 里面执行的 runnable ，执行完标记值为 true ，
   * 否则为 false ， 通过后台线程持续检测标记值可以知道 main thread 是否有阻塞。
   */
  static class UIWatcherRunnable implements Runnable {

    boolean done = false;

    @Override
    public void run() {
      done = true;
    }

    void reset() {
      done = false;
    }
  }
}
