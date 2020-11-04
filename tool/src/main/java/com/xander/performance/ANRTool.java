package com.xander.performance;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

/**
 * @author xander
 * <p>
 * 通过向 main thread 里面放一个指定的 runnable 然后定时去查看是否被运行过来检测是否有 ANR
 */
public class ANRTool {

  private static final String TAG = pTool.TAG + "_ANRTool";

  private static volatile CheckMainThread checkMainThread;

  static void start() {
    xLog.e(TAG, "ANRTool start");
    // 不严谨，后续需要优化。
    if (null != checkMainThread) {
      return;
    }
    // 后台检测线程
    checkMainThread = new CheckMainThread("check-main-thread");
    checkMainThread.start();
  }

  static void stop() {
    if (null != checkMainThread && !checkMainThread.isInterrupted()) {
      checkMainThread.interrupt();
    }
    checkMainThread = null;
  }


  /**
   * 检测 main thread 的 Thread ,在后台指定的时间
   */
  static class CheckMainThread extends Thread {

    MainThreadRunnable uiRunnable = new MainThreadRunnable();
    @SuppressLint("HandlerLeak")
    Handler mainThreadHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if( msg.obj instanceof MainThreadRunnable ) {
          ((MainThreadRunnable)msg.obj).run();
        }
      }
    };

    public CheckMainThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(PerformanceConfig.ANR_CHECK_TIME);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        //xLog.e(TAG, "---------- start check main thread ----------");
        if (!uiRunnable.done) {
          pTool.printThreadStackTrace(TAG, Looper.getMainLooper().getThread(), "ANR");
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
   * main thread 里面执行的 runnable ，执行完标记值为 true ， 否则为 false ，
   * 通过后台线程持续检测标记值可以知道 main thread
   * 是否有阻塞。
   */
  static class MainThreadRunnable implements Runnable {

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
