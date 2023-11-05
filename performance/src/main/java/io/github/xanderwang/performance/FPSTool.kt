package io.github.xanderwang.performance

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import io.github.xanderwang.asu.ALog.e
import io.github.xanderwang.asu.ALog.w

/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 利用 Choreographer 向系统注册一个 FrameCallback
 *
 * 在 FrameCallback 的回调方法里面计数，这个回调方法调用一次就可以初步认为绘制了一帧。
 * 同时，每隔一段时间向 main thread 放一个 runnable ，这个 runnable 做的事情
 * 就是统计两次 run 方法之间 FrameCallback 的回调方法调用了多少次，也就是绘制
 * 了多少帧，通过两次 run 方法之间绘制的帧数就可以计算出来 app 的帧率。
 *
 *
 * 详细的原理可以参考 https://juejin.im/post/6890407553457963022
 */
internal object FPSTool {

    private const val TAG = "FPSTool"

    private val fpsCountRunnable = FpsCountRunnable()

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    @JvmStatic
    fun start() {
        e(TAG, "start")
        handler.post(fpsCountRunnable)
        Choreographer.getInstance().postFrameCallback(fpsCountRunnable)
    }

    private class FpsCountRunnable : Runnable, Choreographer.FrameCallback {
        var time: Long = 0
        var count = 0
        override fun doFrame(frameTimeNanos: Long) {
            count++
            Choreographer.getInstance().postFrameCallback(this)
        }

        override fun run() {
            val curTime = SystemClock.elapsedRealtime()
            if (time == 0L) {
                // 第一次开始监控，跳过
            } else {
                val fps = (1000f * count / (curTime - time) + 0.5f).toInt()
                val fpsStr = String.format("APP FPS is: %-3sHz", fps)
                if (fps <= 50) {
                    e(TAG, fpsStr)
                } else {
                    w(TAG, fpsStr)
                }
            }
            count = 0
            time = curTime
            handler.postDelayed(this, Config.FPS_INTERVAL_TIME)
        }
    }
}