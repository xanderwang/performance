package com.xander.performance;

import android.os.Binder;
import android.os.Parcel;

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
class IPCTool {

  private static String TAG = PERF.TAG + "_IPCTool";


  static void resetTag(String tag) {
    TAG = tag + "_IPCTool";
  }

  static void start() {
    xLog.e(TAG, "start");
    // if (Build.VERSION.SDK_INT >= 29) {
    //   hookTransactListener();
    // } else {
    //   hookWithEpic();
    // }
    hookWithEpic();
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
      // 除了每次调用 android.os.BinderProxy.transact 方法，
      // Parcel.writeInterfaceToken 方法也会被调用，暂时用这个方法来判断是否有 IPC 调用
      // 这个实际发现也会报错，在不同的平台上，好像涉及到了 Parcel 实例的数据读写貌似就会出问题
      // DexposedBridge.findAndHookMethod(
      //     Parcel.class,
      //     "writeInterfaceToken",
      //     String.class,
      //     new ParcelWriteInterfaceTokenHook()
      // );
      // 观察 aidl 生成的代码，发现每次 ipc 调用也会调用 Parcel.readException 方法
      // 但是感觉会误报  囧
      DexposedBridge.findAndHookMethod(
          Parcel.class,
          "readException",
          new ParcelReadExceptionHook()
      );
      xLog.e(TAG, "hookWithEpic");
    } catch (Exception e) {
      xLog.e(TAG, "hookWithEpic", e);
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

  static class ParcelWriteInterfaceTokenHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      xLog.e(TAG, "WriteInterfaceTokenHook:" + param.args[0]);
      // xLog.e(TAG, "WriteInterfaceTokenHook:", new Throwable());
      IPCIssue ipcIssue = new IPCIssue(param.args[0], "IPC", StackTraceUtils.list());
      ipcIssue.print();
    }
  }

  static class ParcelReadExceptionHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list());
      ipcIssue.print();
    }
  }

  private static TransactListenerHandler gTransactListenerHandler = null;

  private static void hookTransactListener() {
    setTransactListener(null);
    try {
      DexposedBridge.findAndHookMethod(
          Class.forName("android.os.BinderProxy"),
          "setTransactListener",
          Class.forName("android.os.Binder$ProxyTransactListener"),
          new SetTransactListenerHook()
      );
      xLog.e(TAG, "hookTransactListener");
    } catch (Exception e) {
      xLog.e(TAG, "hookTransactListener", e);
    }
  }

  /**
   * hook transact 方法总是碰到各种问题，所以换一种方式，但是这个只有 Android 10 以上的版本
   */
  private static void setTransactListener(Object target) {
    try {
      if (null == gTransactListenerHandler) {
        synchronized (IPCTool.class) {
          if (null == gTransactListenerHandler) {
            gTransactListenerHandler = new TransactListenerHandler();
          }
        }
      }
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

  static class SetTransactListenerHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      setTransactListener(param.args[0]);
    }
  }

  static class IPCIssue extends Issue {

    Object ipcInterface;

    public IPCIssue(Object ipcInterface, String msg, Object data) {
      super(Issue.TYPE_IPC, msg, data);
      this.ipcInterface = ipcInterface;
    }

    @Override
    protected void buildOtherString(StringBuilder sb) {
      sb.append("ipc interface: ").append(ipcInterface).append('\n');
    }
  }

}
