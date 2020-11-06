package com.xander.performance;

import android.os.Parcel;
import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 利用 hook 方法， hook android.os.BinderProxy 类的 transact 方法，
 * 从而获取 ipc 调用链 这样可以知道系统的瓶颈在哪里
 */
public class IPCTool {

  private static final String TAG = pTool.TAG + "_IPCTool";

  static void start() {
    xLog.e(TAG, "ThreadTool init");
    hookWithEpic();
  }

  public static void hookWithEpic() {
    try {
      DexposedBridge.findAndHookMethod(
          Class.forName("android.os.BinderProxy"),
          "transact",
          int.class,
          Parcel.class,
          Parcel.class,
          int.class,
          new BinderProxyHook()
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class BinderProxyHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      StackTraceUtils.print(
          TAG,
          Thread.currentThread().getStackTrace(),
          "IPC",
          true,
          this.getClass().getName()
      );
      super.beforeHookedMethod(param);
    }
  }

}