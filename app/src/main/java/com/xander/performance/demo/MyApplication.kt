package com.xander.performance.demo

import android.app.Application
import android.content.Context
import com.xander.performance.PERF.IssueSupplier
import com.xander.performance.PERF
import java.io.File

class MyApplication : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    PERF.init(PERF.Builder()
        .globalTag("test_perf")
        .checkUI(true, 100)
        .checkThread(true)
        .checkFps(true)
        .checkIPC(true)
        .issueSupplier(object : IssueSupplier {
          override fun maxCacheSize(): Long {
            return 1024 * 1204 * 20
          }

          override fun cacheRootDir(): File {
            return cacheDir
          }

          override fun upLoad(zipLogFile: File?): Boolean {
            return false
          }
        })
        .build()
    )
  }
}