package com.xander.performance.demo

import android.app.Application
import android.content.Context
import com.xander.performance.Issue.IssueSupplier
import com.xander.performance.pTool
import java.io.File

class MyApplication : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    pTool.init(pTool.Builder()
        .globalTag("demo")
        .checkUI(true, 100)
        .checkThread(false)
        .checkFps(false)
        .checkIPC(false)
        .issueSupplier(object : IssueSupplier {
          override fun maxCacheSize(): Int {
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