package com.xander.performance.demo;

import android.app.Application;
import android.content.Context;
import com.xander.performance.tool.pTool;

public class MyApplication extends Application {

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    pTool.startPerformance();
  }
}
