package com.xander.performance.demo

import android.app.Application
import android.content.Context
import com.xander.performance.PERF

class MyApplication : Application() {
  override fun attachBaseContext(base: Context) {
    initPERF(base)
  }

  private fun initPERF(context: Context) {
    PERF.init(PERF.Builder()
        .checkUI(true, 100)// 检查 ui lock
        .checkIPC(true) // 检查 ipc 调用
        .checkFps(true, 1000) // 检查 fps
        .checkThread(true)// 检查线程和线程池
        .globalTag("test_perf")// 全局 logcat tag ,方便过滤
        .cacheDirSupplier { context.cacheDir } // issue 文件保存目录
        .maxCacheSizeSupplier { 10 * 1024 * 1024 } // issue 文件最大占用存储空间
        .uploaderSupplier { PERF.LogFileUploader { false } } // issue 文件上传接口
        .build()
    )
  }
}