package com.xander.performance;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

/**
 * @ProjectName: performance
 * @Package: com.xander.performance.tool
 * @ClassName: ThreadTool
 * @Description: 通过 hook 手续来监听自定义线程的创建，打印创建线程的堆栈 主要原理就是通过反射，在调用构造方法的时候，
 * @Author: Xander
 * @CreateDate: 2020/4/13 22:30
 * @Version: 1.0
 */
public class ThreadTool {

  private static final String TAG = pTool.TAG + "_ThreadTool";

  static void init() {
    xLog.e(TAG, "ThreadTool init");
    hookWithEpic();
    //hookWithSandHook(); // sandhook 不是很好用，先注释
  }

  public static void hookWithEpic() {
    try {
      DexposedBridge.hookAllConstructors(Thread.class, new ThreadConstructorHook());
      DexposedBridge.findAndHookMethod(Thread.class, "start", new ThreadStartHook());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class ThreadConstructorHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Thread cThread = (Thread) param.thisObject;
      //xLog.e(TAG, "Thread ConstructorHook thread:" + cThread.getName() + ", started..");
      StackTraceUtils.print(
          TAG,
          Thread.currentThread().getStackTrace(),
          "create thread",
          true,
          this.getClass().getName()
      );
    }

    /*@Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      Thread cThread = (Thread) param.thisObject;
      //xLog.e(TAG, "Thread ConstructorHook thread:" + cThread.getName() + ", exit..");
      //pTool.printThreadStackTrace(TAG, Thread.currentThread(), false, this.getClass().getName());
    }*/
  }

  static class ThreadStartHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Thread cThread = (Thread) param.thisObject;
      //xLog.e(TAG, "Thread StartHook thread:" + cThread.getName() + ", started..");
      StackTraceUtils.print(
          TAG,
          Thread.currentThread().getStackTrace(),
          "thread start",
          true,
          this.getClass().getName()
      );
    }

    /*@Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      Thread cThread = (Thread) param.thisObject;
      //xLog.e(TAG, "Thread StartHook thread:" + cThread.getName() + ", exit..");
      //pTool.printThreadStackTrace(TAG, Thread.currentThread(), false, this.getClass().getName());
    }*/
  }

  /*public static void hookWithSandHook() {
    //first set debuggable
    SandHookConfig.DEBUG = BuildConfig.DEBUG;
    //add hookers
    try {
      xLog.e(TAG, "SandHook hookWithSandHook");
      SandHook.addHookClass(ThreadHooker.class);
      SandHook.addHookClass(ActivityHooker.class);
    } catch (HookErrorException e) {
      e.printStackTrace();
    }
  }*/

  /*@HookClass(Thread.class)
  public static class ThreadHooker {

    private static final String TAG = "ThreadHooker";

    @HookMethodBackup("start")
    static Method startBackup;

    @HookMethod("start")
    public static void start(Thread thiz) throws Throwable {
      xLog.e(TAG, "hooked start success " + thiz);
      SandHook.callOriginByBackup(startBackup, thiz);
      pTool.printThreadStackTrace(TAG, Thread.currentThread(), false, ThreadHooker.class.getName());
    }

  }*/

  /*@HookClass(Activity.class)
  //@HookReflectClass("android.app.Activity")
  public static class ActivityHooker {

    private static final String TAG = "ActivityHooker";

    @HookMethodBackup("onCreate")
    @MethodParams(Bundle.class)
    static Method onCreateBackup;

    @HookMethodBackup("onPause")
    static HookWrapper.HookEntity onPauseBackup;

    @HookMethod("onCreate")
    @MethodParams(Bundle.class)
    public static void onCreate(Activity thiz, Bundle bundle) throws Throwable {
      xLog.e(TAG, "hooked onCreate success " + thiz);
      SandHook.callOriginByBackup(onCreateBackup, thiz, bundle);
    }

    @HookMethod("onPause")
    public static void onPause(@ThisObject Activity thiz) throws Throwable {
      xLog.e(TAG, "hooked onPause success " + thiz);
      onPauseBackup.callOrigin(thiz);
    }
  }*/

}
