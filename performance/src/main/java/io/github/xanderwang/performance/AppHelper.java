package io.github.xanderwang.performance;

import android.content.Context;
import android.os.Looper;

/**
 * @author: xander wang
 * @date:
 */
public class AppHelper {

    /**
     * context 用于后续初始化
     */
    protected static Context mAppContext = null;

    /**
     * 全局的后台线程，用于后台执行一些定时任务
     */
    protected static PerfThread perfThread = new PerfThread();

    /**
     * 全局后台线程的 Looper
     */
    protected static Looper perfLooper;

    protected static Context appContext() {
        return mAppContext;
    }

    public static void init() {
        perfThread.start();
    }

    public static Looper getPerfLooper() {
        return perfLooper;
    }

    static class PerfThread extends Thread {

        public PerfThread() {
            setName("performance-thread");
        }

        @Override
        public void run() {
            super.run();
            Looper.prepare();
            perfLooper = Looper.myLooper();
            Looper.loop();
        }
    }
}
