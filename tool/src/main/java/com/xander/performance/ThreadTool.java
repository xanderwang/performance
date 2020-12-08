package com.xander.performance;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @ProjectName: performance
 * @Package: com.xander.performance
 * @ClassName: ThreadTool
 * @Description: 通过 hook 类的方法来监听线程的创建、启动，线程池的创建和执行任务
 * <p>
 * 监听线程的创建、启动：主要原理就是通过 hook ，在调用线程 start 方法的时候，保存调用栈。
 * 监听线程池的创建、启动: 主要原理就是通过 hook，在线程池构造方法里保存调用栈，然后保存调用栈，
 * 在线程池执行任务的时候，保存任务和线程池的关联，任务和 Worker 的关联，如果线程池需要创建线程了，
 * 根据线程和任务的关系，从而获取线程和线程池的关联。
 * @Author: Xander
 * @CreateDate: 2020/4/13 22:30
 * @Version: 1.0
 */
public class ThreadTool {

  private static String TAG = pTool.TAG + "_ThreadTool";

  static class ThreadPoolInfo {
    String           key;
    List<String>     createTrace;
    List<ThreadInfo> childThreadsInfo = new ArrayList<>();

    void removeThreadInfo(ThreadInfo threadInfo) {
      synchronized (ThreadPoolInfo.class) {
        childThreadsInfo.remove(threadInfo);
      }
    }

    void addThreadInfo(ThreadInfo threadInfo) {
      synchronized (ThreadPoolInfo.class) {
        childThreadsInfo.add(threadInfo);
      }
    }

    boolean isEmpty() {
      return childThreadsInfo.isEmpty();
    }
  }

  static class ThreadInfo {
    String       key;
    String       threadPoolInfoKey;
    List<String> createTrace;
  }


  static Map<String, ThreadInfo> threadInfoMap = new HashMap<>();

  static Map<String, ThreadPoolInfo> threadPoolInfoMap = new HashMap<>();

  /**
   * worker 和 thread pool 的关联，用于绑定 thread 和 thread pool 的关联
   */
  static Map<String, String> workerThreadPoolMap = new HashMap<>(32);

  @Deprecated
  static Map<String, ThreadPoolInfo> runnableThreadPoolMap = new HashMap<>(32);

  static void resetTag(String tag) {
    TAG = tag + "_ThreadTool";
  }

  static void init() {
    xLog.e(TAG, "init");
    hookWithEpic();
    //hookWithSandHook(); // sandhook 不是很好用，先注释
  }

  public static void hookWithEpic() {
    try {
      // hook 7 个参数的构造方法好像会报错，故 hook 指定参数数目的构造方法
      ThreadPoolExecutorConstructorHook constructorHook = new ThreadPoolExecutorConstructorHook();
      Constructor[] constructors = ThreadPoolExecutor.class.getDeclaredConstructors();
      for (int i = 0; i < constructors.length; i++) {
        if (constructors[i].getParameterTypes().length > 6) {
          continue;
        }
        DexposedBridge.hookMethod(constructors[i], constructorHook);
      }

      /*DexposedBridge.findAndHookMethod(
          ThreadPoolExecutor.class,
          "execute",
          Runnable.class,
          new ThreadPoolExecuteHook()
      );*/

      // java.util.concurrent.ThreadPoolExecutor$Worker 是一个内部类，
      // 所以构造方法第一参数就是 ThreadPoolExecutor, 所以构造方法可以将 worker 和 线程池绑定
      // 同时，run 方法执行完，表示线程池里面的一个线程执行完。可以考虑在里面做一些清理工作
      DexposedBridge.hookAllConstructors(Class.forName("java.util.concurrent.ThreadPoolExecutor$Worker"),
          new WorkerConstructorHook()
      );

      // 根据构造方法里面的 runnable 是否为 Worker 可知是否为线程池创建的线程。
      DexposedBridge.hookAllConstructors(Thread.class, new ThreadConstructorHook());

      DexposedBridge.findAndHookMethod(Thread.class, "start", new ThreadStartHook());

      DexposedBridge.findAndHookMethod(Thread.class, "run", new ThreadRunHook());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class ThreadPoolExecutorConstructorHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      xLog.e(TAG, "Thread pool constructor: " + Arrays.toString(param.args));
      String threadPoolInfoKey = Integer.toHexString(param.thisObject.hashCode());
      if (threadPoolInfoMap.containsKey(threadPoolInfoKey)) {
        return;
      }
      // 开始记录信息
      ThreadPoolInfo threadPoolInfo = new ThreadPoolInfo();
      threadPoolInfo.createTrace = StackTraceUtils.list();
      threadPoolInfo.key = threadPoolInfoKey;
      threadPoolInfoMap.put(threadPoolInfoKey, threadPoolInfo);
      new Issues(Issues.TYPE_THREAD, "find thread pool create", threadPoolInfo.createTrace).print();
    }
  }

  /*@Deprecated
  static class ThreadPoolExecuteHook extends XC_MethodHook {

    // execute(Runnable command)
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      //xLog.e(TAG, "ThreadPoolExecuteHook: " + Arrays.toString(param.args));
      // 保存 runnable 和 thread pool 的关系
      String runnableKey = Integer.toHexString(param.args[0].hashCode());
      String threadPoolInfoKey = Integer.toHexString(param.thisObject.hashCode());
      //xLog.e(TAG, "ThreadPoolExecuteHook runnableKey:" + runnableKey);
      //xLog.e(TAG, "ThreadPoolExecuteHook threadPoolInfoKey:" + threadPoolInfoKey);
      ThreadPoolInfo threadPoolInfo = threadPoolInfoMap.get(threadPoolInfoKey);
      runnableThreadPoolMap.put(runnableKey, threadPoolInfo);
    }
  }*/

  static class WorkerConstructorHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      //xLog.e(TAG, "WorkerConstructorHook: " + Arrays.toString(param.args));
      String workerKey = Integer.toHexString(param.thisObject.hashCode());
      String threadPoolKey = Integer.toHexString(param.args[0].hashCode());
      //xLog.e(TAG, "WorkerConstructorHook workerKey:" + workerKey);
      //xLog.e(TAG, "WorkerConstructorHook threadPoolKey:" + threadPoolKey);
      workerThreadPoolMap.put(workerKey, threadPoolKey);
    }
  }

  static class ThreadConstructorHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      // xLog.e(TAG, "Thread constructor: " + Arrays.toString(param.args));
      StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

      ThreadInfo threadInfo = new ThreadInfo();
      String threadKey = Integer.toHexString(param.thisObject.hashCode());
      threadInfo.key = threadKey;
      threadInfo.createTrace = StackTraceUtils.list();

      boolean hasRunnable =
          (param.args.length == 1 && param.args[0] instanceof Runnable) || (param.args.length > 1 && param.args[1] instanceof Runnable);
      // xLog.e(TAG, "ThreadConstructorHook hasRunnable:" + hasRunnable);
      // 获取 runnable
      String workerKey = "";
      if (hasRunnable) {
        Object runnable = param.args[0] instanceof Runnable ? param.args[0] : param.args[1];
        workerKey = Integer.toHexString(runnable.hashCode());
      }
      //xLog.e(TAG, "ThreadConstructorHook workerKey:" + workerKey);
      if (workerThreadPoolMap.containsKey(workerKey)) {
        // 需要和 thread pool 绑定
        String threadPoolKey = workerThreadPoolMap.get(workerKey);
        ThreadPoolInfo threadPoolInfo = threadPoolInfoMap.get(threadPoolKey);
        threadInfo.threadPoolInfoKey = threadPoolKey;
        threadPoolInfo.addThreadInfo(threadInfo);
        // 建立 thread 和 thread pool 的关联后，断开 worker 和 thread pool 的关联
        workerThreadPoolMap.remove(workerKey);
      } else {
        // 不是线程池创建的才加入
        threadInfoMap.put(threadKey, threadInfo);
        // 和线程池没有关联的 thread , 打印线程池的创建调用链
        new Issues(Issues.TYPE_THREAD, "find thread create", threadInfo.createTrace).print();
      }
    }
  }

  static class ThreadStartHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      //super.beforeHookedMethod(param);
      //xLog.e(TAG, "ThreadStartHook:" + Arrays.toString(param.args));
      StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
      String threadKey = Integer.toHexString(param.thisObject.hashCode());
      ThreadInfo threadInfo = threadInfoMap.get(threadKey);
      if (null == threadInfo) {
        xLog.e(TAG, "can not find thread info !!!!!!");
        return;
      }
      // threadInfo.startStackTrace = StackTraceUtils.string(stackTraceElements, true, this.getClass().getName());
      if (null == threadInfo.threadPoolInfoKey) {
        // 说明和 thread pool 没有关联，直接打印
        StackTraceUtils.print(TAG, stackTraceElements, "THREAD START", true, this.getClass().getName());
      }
    }
  }

  static class ThreadRunHook extends XC_MethodHook {

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      // 线程 run 方法走完，线程即将被销毁，去除 thread info 相关的记录
      String threadKey = Integer.toHexString(param.thisObject.hashCode());
      ThreadInfo threadInfo = threadInfoMap.get(threadKey);
      if (null == threadInfo) {
        xLog.e(TAG, "can not find thread info !!!!!!");
        return;
      }
      threadInfoMap.remove(threadKey);
      if (null == threadInfo.threadPoolInfoKey) {
        return;
      }
      ThreadPoolInfo threadPoolInfo = threadPoolInfoMap.get(threadInfo.threadPoolInfoKey);
      if (null == threadPoolInfo) {
        xLog.e(TAG, "can not find thread pool info !!!!!!");
        return;
      }
      threadPoolInfo.removeThreadInfo(threadInfo);
      if (threadPoolInfo.isEmpty()) {
        threadPoolInfoMap.remove(threadInfo.threadPoolInfoKey);
      }
    }
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
