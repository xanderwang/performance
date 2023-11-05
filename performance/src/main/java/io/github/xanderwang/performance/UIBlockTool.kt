package io.github.xanderwang.performance

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Printer
import android.view.KeyEvent
import io.github.xanderwang.asu.ALog.e
import io.github.xanderwang.hook.HookBridge.findAndHookMethod
import io.github.xanderwang.hook.core.MethodHook
import io.github.xanderwang.hook.core.MethodParam
import io.github.xanderwang.performance.AppHelper.perfLooper

/**
 * UI 线程卡顿检查
 * @author xanderwang
 *
 * 原理：
 * 在触发 UI 线程操作的时候，同时在后台线程启动一个延时的任务，到了时间，延时任务还没有被移除，
 * 说明 UI 线程 block 了，此时打印出 UI 线程的状态，如果需要详细的状态，甚至可以
 * 打印出 cpu 和内存的相关信息和状态，目前只是 block 的时候，打印了 UI 线程的调用链。
 */
internal object UIBlockTool {

    private const val TAG = "UIBlockTool"

    /**
     * 用于发起定时任务
     */
    private var uiBlockHandler: Handler? = null

    /**
     * 用于 dump block 时候，系统相关的信息。
     */
    private val dumpBlockInfoRunnable = DumpBlockInfoRunnable()

    /**
     * 开始监控
     */
    fun start() {
        e(TAG, "start")
        perfLooper?.let { uiBlockHandler = Handler(it) }
        // hookLooperPrinter();
        hookHandlerDispatchMessage()
        hookDecorViewDispatchKeyEvent()
    }

    /**
     * 发送延时 dump 任务
     */
    private fun postDumpInfoRunnable() {
        uiBlockHandler?.removeCallbacks(dumpBlockInfoRunnable)
        uiBlockHandler?.postDelayed(dumpBlockInfoRunnable, Config.UI_BLOCK_TIME)
    }

    /**
     * 清理 dump 任务
     */
    private fun clearDumpInfoRunnable() {
        uiBlockHandler?.removeCallbacks(dumpBlockInfoRunnable)
    }

    /**
     * hook looper printer
     */
    fun hookLooperPrinter() {
        Looper.getMainLooper().setMessageLogging(WatcherMainLooperPrinter())
    }

    /**
     * hook Handler 的 dispatchMessage
     */
    private fun hookHandlerDispatchMessage() {
        findAndHookMethod(Handler::class.java, "dispatchMessage", Message::class.java, HandlerDispatchMessageHook())
    }

    /**
     * hook DecorView 的 dispatchKeyEvent
     * 如果是 TV 设备，用遥控或者键盘操作的话，不会走 Looper loop Message 方法
     * 而是会走 InputManager 里面的方法，InputManager 最终调用 DecorView 的 dispatchKeyEvent 方法
     * 这里通过 epic 库来 hook DecorView 的 dispatchKeyEvent 方法的来监控 UI block
     */
    @SuppressLint("PrivateApi")
    private fun hookDecorViewDispatchKeyEvent() {
        try {
            val decorViewClass = Class.forName("com.android.internal.policy.DecorView")
            findAndHookMethod(decorViewClass, "dispatchKeyEvent", KeyEvent::class.java, DecorViewDispatchKeyEventHook())
        } catch (e: Exception) {
            e(TAG, "hookDecorViewDispatchKeyEvent", e)
        }
    }

    /**
     * dump 信息，目前主要 dump 主线程的调用栈，后续可以考虑 dump 更多的信息。
     */
    private class DumpBlockInfoRunnable : Runnable {
        override fun run() {
            val uiIssue = Issue(Issue.TYPE_UI_BLOCK, "UI BLOCK", Utils.list(Looper.getMainLooper().thread))
            uiIssue.print()
        }
    }

    private class WatcherMainLooperPrinter : Printer {
        override fun println(x: String) {
            if (x.startsWith(">>>>>")) {
                postDumpInfoRunnable()
            } else if (x.startsWith("<<<<<")) {
                clearDumpInfoRunnable()
            }
        }
    }

    private class HandlerDispatchMessageHook : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            // super.beforeHookedMethod(param);
            if (Looper.getMainLooper().thread === Thread.currentThread()) {
                postDumpInfoRunnable()
            }
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            // super.afterHookedMethod(param);
            if (Looper.getMainLooper().thread === Thread.currentThread()) {
                clearDumpInfoRunnable()
            }
        }
    }

    private class DecorViewDispatchKeyEventHook : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            // super.beforeHookedMethod(param);
            postDumpInfoRunnable()
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            // super.afterHookedMethod(param);
            clearDumpInfoRunnable()
        }
    }
}