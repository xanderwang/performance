package com.xander.performance;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.Choreographer.FrameCallback;

/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 利用 Choreographer 向系统注册一个 FrameCallback
 * 在 FrameCallback 的回调方法里面计数，这个回调方法调用一次就表示绘制了一帧。
 * 同时，每隔一段时间向 main thread 放一个 runnable ，这个 runnable 做的事情
 * 就是统计两次 run 方法之间 FrameCallback 的回调方法调用了多少次，也就是绘制
 * 了多少帧，通过两次 run 方法之间绘制的帧数就可以计算出来 app 的帧率。
 * <p>
 * 详细的原理可以参考 https://juejin.im/post/6890407553457963022
 */
public class FPSTool {

  private static String TAG = pTool.TAG + "_FPSTool";

  private static FrameRunnable frameRunnable = new FrameRunnable();

  private static Handler handler = new Handler();

  private static long FPS_INTERVAL_TIME = 1000;

  static void resetTag(String tag) {
    TAG = tag + "_FPSTool";
  }

  static void start() {
    xLog.e(TAG, "start");
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
        int fps = (int) (1000.f * count / (curTime - time) + 0.5f);
        String fpsStr = String.format("APP FPS is: %-3sHz", fps);
        if (fps <= 50) {
          xLog.e(TAG, fpsStr);
        } else {
          xLog.w(TAG, fpsStr);
        }
      }
      count = 0;
      time = curTime;
      handler.postDelayed(this, FPS_INTERVAL_TIME);
    }
  }

}
