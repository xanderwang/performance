package com.xander.performance.tool;

import android.util.Log;
import com.taobao.android.dexposed.DexposedBridge;
import com.taobao.android.dexposed.XC_MethodHook;
import java.lang.annotation.Target;

/**
 * @ProjectName: performance
 * @Package: com.xander.performance.tool
 * @ClassName: ThreadTool
 * @Description: 用来监听自定义线程的创建，打印创建线程的堆栈 主要原理就是通过反射，在调用构造方法的时候，
 * @Author: Xander
 * @CreateDate: 2020/4/13 22:30
 * @Version: 1.0
 */
public class ThreadTool {

  private static final String TAG = "ThreadTool";

  public static void init() {
    DexposedBridge.hookAllConstructors(Thread.class, new ThreadConstructorHook());
    DexposedBridge.findAndHookMethod(Thread.class, "start", new ThreadStartHook());
  }

  static class ThreadConstructorHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Thread t = (Thread) param.thisObject;
      Log.i(TAG, "ThreadConstructorHook thread:" + t + ", started..");
//      pTool.printThreadStackTrace(TAG, Thread.currentThread(), false, this.getClass().getName());
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      Thread t = (Thread) param.thisObject;
      Log.i(TAG, "ThreadConstructorHook thread:" + t + ", exit..");
//      pTool.printThreadStackTrace(TAG, Thread.currentThread(), false, this.getClass().getName());
    }
  }


  static class ThreadStartHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Thread t = (Thread) param.thisObject;
      Log.i(TAG, "ThreadStartHook thread:" + t + ", started..");
      pTool.printThreadStackTrace(TAG, Thread.currentThread(), false, ThreadStartHook.class.getName());
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      Thread t = (Thread) param.thisObject;
      Log.i(TAG, "ThreadStartHook thread:" + t + ", exit..");
//      pTool.printThreadStackTrace(TAG, Thread.currentThread(), false, this.getClass().getName());
    }
  }

}
