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
    }
  }

  private val myDemoServer = MyDemoServer()
  override fun onBind(intent: Intent): IBinder? {
    aLog.e(TAG, "DemoService:$this")
    aLog.e(TAG, "DemoService MyDemoServer:$myDemoServer")
    aLog.e(TAG, "DemoService MyDemoServer", IllegalAccessException())
    return myDemoServer
  }

  companion object {
    private const val TAG = "DemoService"
  }
}