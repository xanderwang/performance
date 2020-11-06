package com.xander.performance;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @ProjectName: performance
 * @Package: com.xander.performance.tool
 * @ClassName: ThreadTool
 * @Description: 通过 hook 手续来监听自定义线程的创建，打印创建线程的堆栈
 * 主要原理就是通过 hook ，在调用 线程 start 方法的时候，保存调用栈
 * 然后需要处理线程池，线程池需要注意构造方法和 thread factory 这是后续关联线程池和线程的关键方法
 * @Author: Xander
 * @CreateDate: 2020/4/13 22:30
 * @Version: 1.0
 */
public class ThreadTool {

  private static final String TAG = pTool.TAG + "_ThreadTool";

  static class ThreadInfo {

    int id;
    String name;
    String startStackTrace;
  }

  static class ThreadPoolInfo {

    String name;
    String createStackTrace;
    String threadFactoryKey;
    List<ThreadInfo> childThreadsInfo = new ArrayList<>();
  }

  static Map<String, ThreadInfo> threadInfoMap = new HashMap<>();

  static Map<String, ThreadPoolInfo> threadPoolInfoMap = new HashMap<>();


  static void init() {
    xLog.e(TAG, "ThreadTool init");
    hookWithEpic();
    //hookWithSandHook(); // sandhook 不是很好用，先注释
  }

  public static void hookWithEpic() {
    try {
      //DexposedBridge.hookAllConstructors(
      //    Thread.class,
      //    new ThreadConstructorHook()
      //);

      DexposedBridge.hookAllConstructors(
          ThreadPoolExecutor.class,
          new ThreadPoolExecutorConstructorHook()
      );

      DexposedBridge.findAndHookMethod(
          ThreadFactory.class,
          "newThread" ,
          Runnable.class,
          new ThreadConstructorHook()
      );

      //DexposedBridge.hookAllConstructors(Thread.class, new ThreadConstructorHook());
      DexposedBridge.findAndHookMethod(
          Thread.class,
          "start",
          new ThreadStartHook()
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class ThreadPoolExecutorConstructorHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      if (param.args.length == 7) {
        // 开始记忆
        String key = Integer.toHexString(param.thisObject.hashCode());
        ThreadPoolInfo threadPoolInfo = new ThreadPoolInfo();
        threadPoolInfo.createStackTrace = StackTraceUtils.string(
            Thread.currentThread().getStackTrace(),
            true,
            this.getClass().getName()
        );
        threadPoolInfo.name = key;
        threadPoolInfo.threadFactoryKey = Integer.toHexString(param.args[5].hashCode());
        threadPoolInfoMap.put(key, threadPoolInfo);
      }
    }
  }

  static class ThreadFactoryHook extends XC_MethodHook {

    // 建立  runnable 和  factory 的关系，通过 factory 关系可以关联到 thread pool
    // 然后 thread 在创建的时候，根据 runnable 可以关联到 thread pool

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      // 这里一般是一个线程创建的
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
