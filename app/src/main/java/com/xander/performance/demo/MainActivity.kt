package com.xander.performance.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  fun testANR(v: View) {
    Thread.sleep(10000)
  }

  fun startThread(v: View) {
    thread(name = "test-thread", start = true) {
      Log.d("pTool ThreadTool", Thread.currentThread().name)
      Thread.sleep(3000)
    }
  }

}