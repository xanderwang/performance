package com.xander.performance.demo

import android.app.Application
import android.content.Context
import com.xander.performance.pTool

class MyApplication : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    pTool.init(pTool.Builder()
        .globalTag("demo")
        .checkUIThread(true, 500)
        .checkThread(false)
        .checkFps(true)
        .checkIPC(false)
        .checkHandlerCostTime(200)
        .appContext(this)
        .build()
    )
  }
}