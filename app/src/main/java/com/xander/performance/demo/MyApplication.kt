package com.xander.performance.demo

import android.app.Application
import android.content.Context
import com.xander.performance.pTool

class MyApplication : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    pTool.init(pTool.Builder()
        .globalTag("demo")
        .checkANR(true, 100)
        .checkThread(true)
        .checkFps(true)
        .checkIPC(false)
        .checkHandler(false, 500)
        .build()
    )
  }
}