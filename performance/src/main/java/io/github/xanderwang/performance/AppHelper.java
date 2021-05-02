package io.github.xanderwang.performance;

import android.content.Context;

public class AppHelper {

  protected static Context mAppContext = null;

  protected static Context appContext() {
    return mAppContext;
  }

}
