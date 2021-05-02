package com.xander.performance;

import android.content.Context;

import androidx.startup.Initializer;

import io.github.xanderwang.asu.aLog;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xander Wang
 * Created on 2021/2/3.
 * @Description
 */
public class PerfInitializer implements Initializer {

  private static final String TAG = "PerfInitializer";

  @Override
  public Object create(Context context) {
    aLog.e(TAG, "PerfInitializer create");
    AppHelper.mAppContext = context.getApplicationContext();
    return null;
  }

  @Override
  public List<Class<? extends Initializer<?>>> dependencies() {
    aLog.e(TAG, "PerfInitializer dependencies");
    return new ArrayList<>();
  }
}
