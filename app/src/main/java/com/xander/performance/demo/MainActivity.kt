package com.xander.performance.demo

import android.app.ActivityManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
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
    Log.e(TAG, "testThread thread name:${Thread.currentThread().name}")
    thread(name = "test-thread", start = true) {
      Log.d(TAG, Thread.currentThread().name)
      Thread.sleep(3000)
    }
  }

  fun testThreadPool(v: View) {
    Log.e(TAG, "testThreadPool thread name:${Thread.currentThread().name}")
    // threadPool.submit {
    //   Log.d(TAG, "testThreadPool")
    // }
    Executors.newSingleThreadExecutor().execute {
      Log.d(TAG, "execute!!!")
    }

    // Executors.newFixedThreadPool(3, Executors.defaultThreadFactory()).execute {
    //   Log.d(TAG, "execute!!!")
    // }
  }

  fun testANR(v: View) {
    Log.d(TAG, "testANR thread name:${Thread.currentThread().name}")
    Thread.sleep(1000)
  }

  fun testIPC(v: View) {
    Log.d(TAG, "testIPC thread name:${Thread.currentThread().name}")
    val ams: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
    ams.runningAppProcesses.forEach {
      Log.d(TAG, "runningAppProcesses: ${it.processName}")
    }
  }

  fun testFps(v: View) {
    Log.d(TAG, "testFps thread name:${Thread.currentThread().name}")
    Thread.sleep(200)
  }

  private val lazyHandler by lazy {
    Handler(Looper.getMainLooper())
  }

  private val handler = Handler()

  fun testHandler(v: View) {
    Log.d(TAG, "testHandler thread name:${Thread.currentThread().name}")
    lazyHandler.post {
      Log.d(TAG, "do lazyHandler post msg !!!")
      Thread.sleep(1000)
    }
    handler.post {
      Log.d(TAG, "do handler post msg !!!")
      Thread.sleep(1000)
    }
  }

  var iDemoService: IDemoService? = null

  private val serviceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      Log.e(TAG, "onServiceConnected:$service")
      Log.e(TAG, "onServiceConnected", IllegalAccessException())
      iDemoService = IDemoService.Stub.asInterface(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }
  }

  fun testBindService(v: View) {
    iDemoService?.let {
      unbindService(serviceConnection)
    }
    val i = Intent(this, DemoService::class.java)
    bindService(i, serviceConnection, Service.BIND_AUTO_CREATE)
  }

  companion object {
    private val TAG by lazy { "demo_" }
  }

}