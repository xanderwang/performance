package com.xander.performance.demo

import android.app.Application
import android.content.Context
import com.xander.performance.tool.pTool

class MyApplication : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    pTool.startPerformance()
  }
}