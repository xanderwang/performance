package com.xander.performance;

import android.os.Binder;
import android.os.Parcel;
import android.os.SystemClock;

import com.xander.asu.aLog;
import com.xander.performance.hook.HookBridge;
import com.xander.performance.hook.core.MethodParam;
import com.xander.performance.hook.core.MethodHook;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;


/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 利用 hook 方法， hook android.os.BinderProxy 类的 transact 方法，
 * 从而获取 ipc 调用链 这样可以知道系统的瓶颈在哪里
 */
class IPCTool {

  private static final String TAG = "IPCTool";

  private static Method binderInterfaceDescriptor = null;

  private static HashMap<String, IPCIssue> issueHashMap = new HashMap<>(64);

  static void start() {
    aLog.e(TAG, "start");
    hookIPC();
  }

  private static void hookIPC() {
    try {
      if (HookBridge.isSandHook()) {
        // 这个方法  epic hook 的话会报错，很奇怪，理论上是一个比较好的 hook 点
        Class<?> binderProxyClass = Class.forName("android.os.BinderProxy");
        if (null != binderProxyClass) {
          binderInterfaceDescriptor = binderProxyClass.getDeclaredMethod("getInterfaceDescriptor");
          if (null != binderInterfaceDescriptor) {
            binderInterfaceDescriptor.setAccessible(true);
          }
        }
        HookBridge.findAndHookMethod(
            binderProxyClass,
            "transact",
            int.class,
            Parcel.class,
            Parcel.class,
            int.class,
            new BinderTransactProxyHook()
        );
      } else {
        // 除了每次调用 android.os.BinderProxy.transact 方法，
        // 观察 AIDL 生成的代码，发现每次 IPC 调用也会调用 Parcel.readException 方法
        // 故初步用这个方法作为切入点来监控系统的 IPC 调用情况
        HookBridge.findAndHookMethod(Parcel.class, "readException", new ParcelReadExceptionHook());
      }
      aLog.e(TAG, "hookIPC");
    } catch (Exception e) {
      aLog.e(TAG, "hookIPC", e);
    }
  }

  static class BinderTransactProxyHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      super.beforeHookedMethod(param);
      String ipcInterface = null;
      if (null != binderInterfaceDescriptor) {
        ipcInterface = (String) binderInterfaceDescriptor.invoke(param.getThisObject());
      }
      if (null != ipcInterface) {
        String issueToken = ipcInterface + param.getArgs()[0];
        IPCIssue ipcIssue = new IPCIssue(ipcInterface, "IPC", null);
        ipcIssue.startTime = SystemClock.elapsedRealtime();
        issueHashMap.put(issueToken, ipcIssue);
      } else {
        Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list());
        ipcIssue.print();
      }
    }

    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      super.afterHookedMethod(param);
      String ipcInterface = null;
      if (null != binderInterfaceDescriptor) {
        ipcInterface = (String) binderInterfaceDescriptor.invoke(param.getThisObject());
      }
      if (null != ipcInterface) {
        String issueToken = ipcInterface + param.getArgs()[0];
        IPCIssue ipcIssue = issueHashMap.remove(issueToken);
        if (null != ipcIssue) {
          ipcIssue.costTime = SystemClock.elapsedRealtime() - ipcIssue.startTime;
          if (ipcIssue.costTime >= Config.UI_BLOCK_INTERVAL_TIME) {
            ipcIssue.setData(StackTraceUtils.list());
            ipcIssue.print();
          }
        }
      }
    }
  }

  static class ParcelReadExceptionHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list());
      ipcIssue.print();
    }
  }

  @Deprecated
  static class ParcelWriteInterfaceTokenHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      super.beforeHookedMethod(param);
      aLog.e(TAG, "WriteInterfaceTokenHook:" + param.getArgs()[0]);
      // aLog.e(TAG, "WriteInterfaceTokenHook:", new Throwable());
      IPCIssue ipcIssue = new IPCIssue(param.getArgs()[0], "IPC", StackTraceUtils.list());
      ipcIssue.print();
    }
  }

  @Deprecated
  private static TransactListenerHandler gTransactListenerHandler = null;

  @Deprecated
  private static void hookTransactListener() {
    setTransactListener(null);
    try {
      HookBridge.findAndHookMethod(
          Class.forName("android.os.BinderProxy"),
          "setTransactListener",
          Class.forName("android.os.Binder$ProxyTransactListener"),
          new SetTransactListenerHook()
      );
      aLog.e(TAG, "hookTransactListener");
    } catch (Exception e) {
      aLog.e(TAG, "hookTransactListener", e);
    }
  }

  /**
   * hook transact 方法总是碰到各种问题，所以换一种方式，但是这个只有 Android 10 以上的版本
   */
  @Deprecated
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
      // aLog.e(TAG, "invoke setTransactListener");
      Field listener = binderProxy.getDeclaredField("sTransactListener");
      listener.setAccessible(true);
      aLog.e(TAG, "android.os.BinderProxy.sTransactListener is:" + listener.get(null));
    } catch (Exception e) {
      aLog.e(TAG, "setTransactListener error", e);
    }
  }

  @Deprecated
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

  @Deprecated
  static class SetTransactListenerHook extends MethodHook {
    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      super.afterHookedMethod(param);
      setTransactListener(param.getArgs()[0]);
    }
  }

  static class IPCIssue extends Issue {

    Object ipcInterface;

    long startTime = 0L;
    long costTime  = 0L;

    public IPCIssue(Object ipcInterface, String msg, Object data) {
      super(Issue.TYPE_IPC, msg, data);
      this.ipcInterface = ipcInterface;
    }

    @Override
    protected void buildOtherString(StringBuilder sb) {
      if (null != ipcInterface) {
        sb.append("ipc interface: ").append(ipcInterface).append('\n');
      }
      if (costTime > 0) {
        sb.append("ipc cost time: ").append(costTime).append(" ms\n");
      }
    }
  }

}
