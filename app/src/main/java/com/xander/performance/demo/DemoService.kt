package com.xander.performance.demo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import com.xander.asu.aLog

/**
 * @author Xander Wang
 * Created on 2020/12/24.
 * @Description //TODO
 */
class DemoService : Service() {
  internal class MyDemoServer : IDemoService.Stub() {
    @Throws(RemoteException::class)
    override fun demo() {
      aLog.e(TAG, "DemoService demo thread:$${Thread.currentThread().name}")
      Thread.sleep(1000)
    }
  }

  private val myDemoServer = MyDemoServer()

  override fun onBind(intent: Intent): IBinder? {
    aLog.d(TAG, "DemoService onBind:$this")
    aLog.d(TAG, "DemoService MyDemoServer:$myDemoServer")
    aLog.d(TAG, "DemoService onBind thread:$${Thread.currentThread().name}")
    return myDemoServer
  }

  companion object {
    private const val TAG = "DemoService"
  }
}