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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.xander.asu.aLog
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  fun testThread(v: View) {
    aLog.e(TAG, "testThread thread name:${Thread.currentThread().name}")
    thread(name = "test-thread-10000", start = true) {
      Thread.sleep(10000)
      aLog.d(TAG, Thread.currentThread().name)
    }
    thread(name = "test-thread-3000", start = true) {
      Thread.sleep(3000)
      aLog.d(TAG, Thread.currentThread().name)
    }
  }

  fun testThreadPool(v: View) {
    aLog.e(TAG, "testThreadPool thread name:${Thread.currentThread().name}")
    // threadPool.submit {
    //   aLog.d(TAG, "testThreadPool")
    // }
    Executors.newSingleThreadExecutor().execute {
      aLog.d(TAG, "execute!!!")
    }
    // Executors.newFixedThreadPool(3, Executors.defaultThreadFactory()).execute {
    //   aLog.d(TAG, "execute!!!")
    // }
  }

  fun testANR(v: View) {
    aLog.d(TAG, "testANR thread name:${Thread.currentThread().name}")
    Thread.sleep(1000)
  }

  fun testIPC(v: View) {
    aLog.d(TAG, "testIPC thread name:${Thread.currentThread().name}")
    val ams: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
    ams.runningAppProcesses.forEach {
      aLog.d(TAG, "runningAppProcesses: ${it.processName}")
    }
  }

  fun testFps(v: View) {
    aLog.d(TAG, "testFps thread name:${Thread.currentThread().name}")
    Thread.sleep(200)
  }

  private val lazyHandler by lazy {
    Handler(Looper.getMainLooper())
  }

  private val handler = Handler()

  fun testHandler(v: View) {
    aLog.d(TAG, "testHandler thread name:${Thread.currentThread().name}")
    lazyHandler.post {
      aLog.d(TAG, "do lazyHandler post msg !!!")
      Thread.sleep(1000)
    }
    handler.post {
      aLog.d(TAG, "do handler post msg !!!")
      Thread.sleep(1000)
    }
  }

  var iDemoService: IDemoService? = null

  private val serviceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      aLog.e(TAG, "onServiceConnected:$service")
      aLog.e(TAG, "onServiceConnected", IllegalAccessException())
      iDemoService = IDemoService.Stub.asInterface(service)
      iDemoService?.demo()
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
    private const val TAG = "MainActivity"
  }

}