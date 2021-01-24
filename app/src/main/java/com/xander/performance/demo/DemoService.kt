package com.xander.performance.demo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * @author Xander Wang
 * Created on 2020/12/24.
 * @Description //TODO
 */
public class DemoService extends Service {

  static class MyDemoServer extends IDemoService.Stub {
    @Override
    public void demo() throws RemoteException {

    }
  }

  private MyDemoServer myDemoServer = new MyDemoServer();


  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    Log.e("xxx","DemoService:" + this);
    Log.e("xxx","DemoService MyDemoServer:" + myDemoServer);
    Log.e("xxx","DemoService MyDemoServer", new IllegalAccessException());
    return myDemoServer;
  }
}
