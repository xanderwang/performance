package com.xander.performance;

import android.os.Parcel;
import android.os.SystemClock;

import com.xander.performance.hook.HookBridge;
import com.xander.performance.hook.core.MethodHook;
import com.xander.performance.hook.core.MethodParam;

import java.lang.reflect.Method;
import java.util.HashMap;

import io.github.xanderwang.asu.aLog;


/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 利用 hook 方法， hook android.os.BinderProxy 类的 transact 方法，
 * 从而获取 ipc 调用链 这样可以知道系统的瓶颈在哪里
 */
class IPCTool {

  private static final String TAG = "IPCTool";

  private static Method binderInterfaceDescriptor = null;

  private static HashMap<String, IPCIssue> issueHashMap = new HashMap<>(64);

  private static void startTransact(Object ipcInterface, Object methodToken) {
    if (null == ipcInterface) {
      Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list());
      ipcIssue.print();
      return;
    }
    String ipcToken = String.format("%s_%s", ipcInterface, methodToken);
    IPCIssue ipcIssue = new IPCIssue(ipcInterface, "IPC", null);
    ipcIssue.startTime = SystemClock.elapsedRealtime();
    issueHashMap.put(ipcToken, ipcIssue);
  }

  private static void endTransact(Object ipcInterface, Object methodToken,
      StackTraceElement[] startTrace) {
    if (null == ipcInterface) {
      return;
    }
    String ipcToken = String.format("%s_%s", ipcInterface, methodToken);
    IPCIssue ipcIssue = issueHashMap.remove(ipcToken);
    if (null != ipcIssue) {
      ipcIssue.costTime = SystemClock.elapsedRealtime() - ipcIssue.startTime;
      if (ipcIssue.costTime >= Config.UI_BLOCK_TIME) {
        ipcIssue.setData(StackTraceUtils.list(startTrace));
        ipcIssue.print();
      }
    } else {
      aLog.e(TAG, "can not find ipc info when end transact!!!");
    }
  }

  private static void justCheckTransact(StackTraceElement[] startTrace) {
    Issue ipcIssue = new Issue(Issue.TYPE_IPC, "IPC", StackTraceUtils.list(startTrace));
    ipcIssue.print();
  }

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
      // super.beforeHookedMethod(param);
      Object ipcInterface = null;
      if (null != binderInterfaceDescriptor) {
        ipcInterface = binderInterfaceDescriptor.invoke(param.getThisObject());
      }
      startTransact(ipcInterface, param.getArgs()[0]);
    }

    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      // super.afterHookedMethod(param);
      Object ipcInterface = null;
      if (null != binderInterfaceDescriptor) {
        ipcInterface = binderInterfaceDescriptor.invoke(param.getThisObject());
      }
      endTransact(ipcInterface, param.getArgs()[0], Thread.currentThread().getStackTrace());
    }
  }

  static class ParcelReadExceptionHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      // super.beforeHookedMethod(param);
      justCheckTransact(Thread.currentThread().getStackTrace());
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
        sb.append("cost time: ").append(costTime).append(" ms\n");
      }
    }
  }

}
