package com.xander.performance.demo

import android.app.ActivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  fun testThread(v: View) {
    thread(name = "test-thread", start = true) {
      Log.d("pTool ThreadTool", Thread.currentThread().name)
      Thread.sleep(3000)
    }
  }

  private val threadPool by lazy {
    Log.e(TAG, "newSingleThreadExecutor")
    Executors.newSingleThreadExecutor()
  }

  fun testThreadPool(v: View) {
    Log.e(TAG, "testThreadPool")
//    threadPool.submit {
//      Log.d(TAG,"testThreadPool")
//    }
    Executors.newSingleThreadExecutor().execute {
      Log.d(TAG, "execute!!!")
    }

//    Executors.newFixedThreadPool(3, Executors.defaultThreadFactory()).execute {
//      Log.d(TAG, "execute!!!")
//    }
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

  private val h by lazy { Handler(Looper.getMainLooper())}

  fun testHandler(v: View) {
    h.post {
      Thread.sleep(500)
    }
  }

  companion object {
    private val TAG by lazy { "demo_" }
  }


}