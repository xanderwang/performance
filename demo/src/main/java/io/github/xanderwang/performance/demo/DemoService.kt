package io.github.xanderwang.performance.demo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import io.github.xanderwang.asu.ALog

/**
 * demo service for test aidl
 * @author Xander Wang
 * Created on 2020/12/24.
 * @Description
 */
class DemoService : Service() {
    internal class MyDemoServer : IDemoService.Stub() {
        @Throws(RemoteException::class)
        override fun demo() {
            ALog.e(TAG, "DemoService demo thread:$${Thread.currentThread().name}")
            Thread.sleep(1000)
        }
    }

    private val myDemoServer = MyDemoServer()

    override fun onBind(intent: Intent): IBinder? {
        ALog.d(TAG, "DemoService onBind:$this")
        ALog.d(TAG, "DemoService MyDemoServer:$myDemoServer")
        ALog.d(TAG, "DemoService onBind thread:$${Thread.currentThread().name}")
        return myDemoServer
    }

    companion object {
        private const val TAG = "DemoService"
    }
}