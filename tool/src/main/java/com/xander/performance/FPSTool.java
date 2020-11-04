package com.xander.performance;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.Choreographer.FrameCallback;

/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 利用 Choreographer 向系统
 */
public class FPSTool {

  private static final String TAG = pTool.TAG + "_FPSTool";

  static FrameRunnable frameRunnable = new FrameRunnable();

  static Handler handler = new Handler();

  static long FPS_INTERVAL_TIME = 1000;

  static void start() {
    handler = new Handler(Looper.getMainLooper());
    handler.post(frameRunnable);
    Choreographer.getInstance().postFrameCallback(frameRunnable);
  }


  private static class FrameRunnable implements Runnable, FrameCallback {

    long time = 0;
    int count = 0;

    @Override
    public void doFrame(long frameTimeNanos) {
      count++;
      Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void run() {
      long curTime = SystemClock.elapsedRealtime();
      if (time == 0) {
        // 第一次开始监控，跳过
      } else {
        xLog.e(TAG, "app fps:" + (1000.f * count / (curTime - time)));
      }
      count = 0;
      time = curTime;
      handler.postDelayed(this, FPS_INTERVAL_TIME);
    }
  }

}
