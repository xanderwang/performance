package io.github.xanderwang.performance

import android.content.Context
import android.os.Looper

/**
 * @author: xander wang
 * @date:
 */
object AppHelper {
    /**
     * context 用于后续初始化
     */
    var appContext: Context? = null

    /**
     * 全局的后台线程，用于后台执行一些定时任务
     */
    private var performanceThread = PerformanceThread()

    /**
     * 全局后台线程的 Looper
     */
    @JvmStatic
    var perfLooper: Looper? = null
        internal set

    @JvmStatic
    fun init() {
        performanceThread.start()
    }

    internal class PerformanceThread : Thread() {
        init {
            name = "performance-thread"
        }

        override fun run() {
            super.run()
            Looper.prepare()
            perfLooper = Looper.myLooper()
            Looper.loop()
        }
    }
}