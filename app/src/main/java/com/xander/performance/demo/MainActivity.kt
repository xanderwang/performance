package com.xander.performance.demo

import android.app.ActivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    Thread.sleep(4000)
  }

  fun testFps(v: View) {
    Thread.sleep(200)
  }

  fun testIPC(v: View) {
    val ams: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
    ams.runningAppProcesses.forEach {
      Log.d("testIPC", it.processName)
    }
  }

  fun testThread(v: View) {
    thread(name = "test-thread", start = true) {
      Log.d("pTool ThreadTool", Thread.currentThread().name)
      Thread.sleep(3000)
    }
  }

  private val h = lazy { Handler(Looper.getMainLooper())}

  fun testHandler(v: View) {
    h.value.post {
      Thread.sleep(300)
    }
  }

}