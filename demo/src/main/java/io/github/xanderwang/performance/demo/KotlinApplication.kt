package io.github.xanderwang.performance.demo

import android.app.Application
import android.content.Context
import io.github.xanderwang.performance.PERF
import java.io.File

class KotlinApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initPERF(this)
    }

    private fun doUpload(log: File?): Boolean {
        return false
    }

    private fun initPERF(context: Context) {
        PERF.init(
            PERF.Builder().checkUI(false, 100) // 检查 ui lock
                .checkIPC(false) // 检查 ipc 调用
                .checkFps(false, 1000) // 检查 fps
                .checkThread(false) // 检查线程和线程池
                .checkBitmap(false) // 检测 Bitmap 的创建
                .globalTag("kotlin_demo_performance") // 全局 logcat tag ,方便过滤
                // .cacheDirSupplier ( ()-> {context.cacheDir} ) // issue 文件保存目录
                .maxCacheSizeSupplier(object : PERF.IssueSupplier<Int> { // issue 文件最大占用存储空间
                    override fun get(): Int {
                        return 10 * 1024 * 1024
                    }
                })
                .uploaderSupplier(object : PERF.LogFileUploader {
                    override fun upload(logFile: File?): Boolean {
                        return doUpload(logFile)
                    }
                })
                .build()
        )
    }
}