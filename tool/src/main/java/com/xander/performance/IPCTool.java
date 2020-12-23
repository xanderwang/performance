package com.xander.performance;

import android.os.Binder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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
    setTransactListener(null);
    hookWithEpic();
  }

  private static TransactListenerHandler gTransactListenerHandler = new TransactListenerHandler();

  /**
   * hook transact 方法总是碰到各种问题，所以换一种方式
   */
  private static void setTransactListener(Object target) {
    try {
      Class binderProxy = Class.forName("android.os.BinderProxy");
      Class transactListener = Class.forName("android.os.Binder$ProxyTransactListener");
      Method setMethod = binderProxy.getDeclaredMethod("setTransactListener", transactListener);
      setMethod.setAccessible(true);
      gTransactListenerHandler.setTarget(target);
      Object proxyInstance = Proxy.newProxyInstance(
          Binder.class.getClassLoader(),
          new Class[]{transactListener},
          gTransactListenerHandler
      );
      setMethod.invoke(null, proxyInstance);
      // xLog.e(TAG, "invoke setTransactListener");
      Field listener = binderProxy.getDeclaredField("sTransactListener");
      listener.setAccessible(true);
      xLog.e(TAG, "android.os.BinderProxy.sTransactListener is:" + listener.get(null));
    } catch (Exception e) {
      xLog.e(TAG, "setTransactListener error", e);
    }
  }

  static class TransactListenerHandler implements InvocationHandler {

    private Object target;

    public TransactListenerHandler() {
    }

    public void setTarget(Object target) {
      this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String methodName = method.getName();
      if ("onTransactStarted".equals(methodName)) {
        Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list());
        ipcIssue.print();
      }
      if (null != target) {
        method.setAccessible(true);
        return method.invoke(target, args);
      }
      return null;
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
          new SetTransactListenerHook()
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

  static class SetTransactListenerHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      setTransactListener(param.args[0]);
    }
  }

}
