package com.xander.performance.demo

import android.app.Application
import android.content.Context
import com.xander.performance.pTool

class MyApplication : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    pTool.init(pTool.Builder()
        .globalTag("demo")
        .checkUIThread(false, 500)
        .checkThread(false)
        .checkFps(false)
        .checkIPC(false)
        .checkHandlerCostTime(200)
        .build()
    )
  }
}