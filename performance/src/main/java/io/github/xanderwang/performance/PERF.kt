package io.github.xanderwang.performance

import android.util.Log
import io.github.xanderwang.asu.AConstants
import me.weishu.reflection.Reflection
import java.io.File

object PERF {

    const val TAG = "PERF"

    @JvmStatic
    fun init(builder: Builder?) {
        val perfBuilder = builder ?: Builder()
        // 绕过反射限制
        Reflection.unseal(AppHelper.appContext)
        // 是否过滤类名
        Config.FILTER_CLASS_NAME = perfBuilder.mFilterClass
        // log 等级
        AConstants.logLevel = perfBuilder.logLevel
        // 设置全局 tag
        AConstants.setGlobalTag(perfBuilder.globalTag)
        // 初始化 app helper
        AppHelper.init()
        // 初始化 issue
        Issue.init(perfBuilder.cacheDirSupplier, perfBuilder.macCacheSizeSupplier, perfBuilder.logFileUploader)

        // 启动各种检测
        if (perfBuilder.mCheckThread) {
            Config.THREAD_BLOCK_TIME = perfBuilder.mThreadBlockTime
            ThreadTool.init()
        }
        if (perfBuilder.mCheckUI) {
            Config.UI_BLOCK_TIME = perfBuilder.mUIBlockTime
            UIBlockTool.start()
        }
        if (perfBuilder.mCheckIPC) {
            Config.IPC_BLOCK_TIME = perfBuilder.mIpcBlockTime
            IPCTool.start()
        }
        if (perfBuilder.mCheckFPS) {
            Config.FPS_INTERVAL_TIME = perfBuilder.mFPSIntervalTime
            FPSTool.start()
        }
        if (perfBuilder.mCheckBitmap) {
            BitmapTool.start()
        }
    }

    class Builder {
        /**
         * logLevel ，设置可以打印的 log 等级
         */
        var logLevel = Log.DEBUG

        /**
         * 是否开启检测 UI 线程
         */
        var mCheckUI = false

        /**
         * UI 线程的检测触发时间间隔，超过时间间隔，会被认为发生了 block
         */
        var mUIBlockTime = Config.UI_BLOCK_TIME

        /**
         * 检测线程的 start 方法调用栈
         */
        var mCheckThread = false

        /**
         * 线程 block 的时间间隔，超过了表示后台执行的任务太多了，需要注意。
         */
        var mThreadBlockTime = Config.THREAD_BLOCK_TIME

        /**
         * IPC 调用 block 的时间间隔
         */
        var mIpcBlockTime = Config.IPC_BLOCK_TIME

        /**
         * UI 线程的检测触发时间间隔，超过时间间隔，会被认为发生了 block
         */
        var mFPSIntervalTime = Config.FPS_INTERVAL_TIME

        /**
         * 是否检测 fps
         */
        var mCheckFPS = false

        /**
         * 是否需要检测 ipc， 也就是进程间通讯
         */
        var mCheckIPC = false

        /**
         * 是否需要检测 Bitmap 的创建
         */
        var mCheckBitmap = false

        /**
         * 是否需要在打印 log 的时候过滤一些不必要的类名。
         */
        var mFilterClass = true

        /**
         * issue 文件的保存目录
         */
        var cacheDirSupplier: IssueSupplier<File>? = null

        /**
         * issue 缓存最大的目录大小
         */
        var macCacheSizeSupplier: IssueSupplier<Int>? = null

        /**
         * log file 上传器
         */
        var logFileUploader: LogFileUploader? = null

        /**
         * 全局的 log tag
         */
        var globalTag = TAG
        fun checkUI(check: Boolean): Builder {
            mCheckUI = check
            return this
        }

        fun checkUI(check: Boolean, blockTime: Long): Builder {
            mCheckUI = check
            mUIBlockTime = blockTime
            return this
        }

        fun checkThread(check: Boolean): Builder {
            mCheckThread = check
            return this
        }

        fun checkThread(check: Boolean, threadBlockTime: Long): Builder {
            mCheckThread = check
            mThreadBlockTime = threadBlockTime
            return this
        }

        fun checkFps(check: Boolean): Builder {
            mCheckFPS = check
            return this
        }

        fun checkFps(check: Boolean, fpsIntervalTime: Long): Builder {
            mCheckFPS = check
            mFPSIntervalTime = fpsIntervalTime
            return this
        }

        fun checkIPC(check: Boolean): Builder {
            mCheckIPC = check
            return this
        }

        fun checkIPC(check: Boolean, ipcBlockTime: Long): Builder {
            mCheckIPC = check
            mIpcBlockTime = ipcBlockTime
            return this
        }

        fun checkBitmap(check: Boolean): Builder {
            mCheckBitmap = check
            return this
        }

        fun filterClass(filter: Boolean): Builder {
            mFilterClass = filter
            return this
        }

        fun globalTag(tag: String): Builder {
            globalTag = tag
            return this
        }

        fun cacheDirSupplier(cache: IssueSupplier<File>?): Builder {
            cacheDirSupplier = cache
            return this
        }

        fun maxCacheSizeSupplier(cacheSize: IssueSupplier<Int>?): Builder {
            macCacheSizeSupplier = cacheSize
            return this
        }

        fun uploaderSupplier(uploader: LogFileUploader?): Builder {
            logFileUploader = uploader
            return this
        }

        fun logLevel(level: Int): Builder {
            logLevel = level
            return this
        }

        fun build(): Builder {
            return this
        }
    }

    interface IssueSupplier<T> {
        fun get(): T
    }

    interface LogFileUploader {
        fun upload(logFile: File?): Boolean
    }
}