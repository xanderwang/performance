package com.xander.performance.tool;

import android.util.Log;

import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.MethodParams;
import com.swift.sandhook.annotation.ThisObject;
import com.swift.sandhook.wrapper.HookErrorException;
import com.swift.sandhook.wrapper.HookWrapper;

import java.lang.reflect.Method;
import java.security.AccessControlContext;

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
    try {
      SandHook.addHookClass(ThreadHooker.class);
    } catch (HookErrorException e) {
      e.printStackTrace();
    }
  }

  @HookClass(Thread.class)
  static class ThreadHooker {

    @HookMethodBackup("start")
    static Method startOrigin;

    @HookMethodBackup("start")
    static HookWrapper.HookEntity startBackUp;

    @HookMethod("start")
    public static void startMethodHooked(@ThisObject Thread thread) {
      pTool.printThreadStackTrace(TAG, Thread.currentThread());
      try {
        startBackUp.callOrigin(thread);
        //SandHook.callOriginByBackup(startOrigin, thread);
        //startOrigin.invoke(thread);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
}
