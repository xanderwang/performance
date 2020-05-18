package com.xander.performance.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    testThread()
  }

  private fun testThread() {
    thread(name = "demo-thread",start = true) {
      Log.d("ThreadTool",Thread.currentThread().name)
      Thread.sleep(3000)
    }
  }


}