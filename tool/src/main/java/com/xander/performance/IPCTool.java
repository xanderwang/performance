package com.xander.performance;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import java.lang.reflect.Method;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 利用 hook 方法， hook android.os.BinderProxy 类的 transact 方法，
 * 从而获取 ipc 调用链 这样可以知道系统的瓶颈在哪里
 */
public class IPCTool {

  private static String TAG = pTool.TAG + "_IPCTool";

  static void resetTag(String tag) {
    TAG = tag + "_IPCTool";
  }

  static void start() {
    xLog.e(TAG, "start");
    hookWithEpic();
    test();
  }

  private static void test() {
    try {
      Class binderProxy = Class.forName("android.os.BinderProxy");
      Class listener = Class.forName("android.os.Binder$ProxyTransactListener");
      Method method = binderProxy.getMethod("setTransactListener", listener);
      Class iListener = Class.forName("android.os.Binder$PropagateWorkSourceTransactListener");
      method.invoke(null, iListener.newInstance());
      xLog.e(TAG, "invoke setTransactListener");
    } catch (Exception e) {
      xLog.e(TAG, "setTransactListener error", e);
    }
  }

  private static void hookWithEpic() {
    try {
      // 这个 hook 会报错，很奇怪
      // DexposedBridge.findAndHookMethod(
      //     Class.forName("android.os.BinderProxy"),
      //     "transact",
      //     int.class,
      //     Parcel.class,
      //     Parcel.class,
      //     int.class,
      //     new BinderTransactProxyHook()
      // );
      DexposedBridge.findAndHookMethod(
          Class.forName("android.os.BinderProxy"),
          "setTransactListener",
          Class.forName("android.os.Binder$ProxyTransactListener"),
          new ListenerHook()
      );
      DexposedBridge.findAndHookMethod(
          Binder.class,
          "checkParcel",
          IBinder.class,
          int.class,
          Parcel.class,
          String.class,
          new CheckParcelHook()
      );
      xLog.e(TAG, "hookWithEpic");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class BinderTransactProxyHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list());
      ipcIssue.print();
      super.beforeHookedMethod(param);
    }
  }

  static class ListenerHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list());
      ipcIssue.print();
      super.beforeHookedMethod(param);
    }
  }

  static class CheckParcelHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list());
      ipcIssue.print();
    }
  }

}
