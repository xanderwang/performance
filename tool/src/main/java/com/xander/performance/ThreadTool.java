package com.xander.performance;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ProjectName: performance
 * @Package: com.xander.performance
 * @ClassName: ThreadTool
 * @Description:
 * 通过 hook 类的方法来监听线程的创建、启动，线程池的创建和执行任务
 *
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

  static class ThreadInfo {

    String key;
    String threadPoolInfoKey;
    String createStackTrace;
    String startStackTrace;
  }

  static class ThreadPoolInfo {

    String key;
    String createStackTrace;
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

  static Map<String, ThreadInfo> threadInfoMap = new HashMap<>();

  static Map<String, ThreadPoolInfo> threadPoolInfoMap = new HashMap<>();

  static Map<String, ThreadPoolInfo> runnableThreadPoolMap = new HashMap<>(32);

  static Map<String, String> workerRunnableMap = new HashMap<>(32);


  static void init() {
    TAG = pTool.TAG + "_ThreadTool";
    xLog.e(TAG, "init");
    hookWithEpic();
    //hookWithSandHook(); // sandhook 不是很好用，先注释
  }

  public static void hookWithEpic() {
    try {

      // 7 个参数的方法 hook 好像会报错
      ThreadPoolExecutorConstructorHook constructorHook = new ThreadPoolExecutorConstructorHook();
      Member threadPoolExecutorConstructor = ThreadPoolExecutor.class.getDeclaredConstructor(
          int.class,
          int.class,
          long.class,
          TimeUnit.class,
          BlockingQueue.class
      );
      DexposedBridge.hookMethod(
          threadPoolExecutorConstructor,
          constructorHook
      );
      threadPoolExecutorConstructor = ThreadPoolExecutor.class.getDeclaredConstructor(
          int.class,
          int.class,
          long.class,
          TimeUnit.class,
          BlockingQueue.class,
          ThreadFactory.class
      );
      DexposedBridge.hookMethod(
          threadPoolExecutorConstructor,
          constructorHook
      );

      DexposedBridge.findAndHookMethod(
          ThreadPoolExecutor.class,
          "execute",
          Runnable.class,
          new ThreadPoolExecuteHook()
      );

      // java.util.concurrent.ThreadPoolExecutor$Worker 构造方法可以将 worker 和线程池绑定
      // run 方法执行完，表示线程池里面的一个线程执行完。
      DexposedBridge.hookAllConstructors(
          Class.forName("java.util.concurrent.ThreadPoolExecutor$Worker"),
          new WorkerConstructorHook()
      );

      // 根据构造方法里面的 runnable 是否为 Worker 可知是否为线程池创建的线程。
      DexposedBridge.hookAllConstructors(
          Thread.class,
          new ThreadConstructorHook()
      );

      DexposedBridge.findAndHookMethod(
          Thread.class,
          "start",
          new ThreadStartHook()
      );

      DexposedBridge.findAndHookMethod(
          Thread.class,
          "run",
          new ThreadRunHook()
      );

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class ThreadPoolExecutorConstructorHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      xLog.e(TAG, "Thread pool constructor: " + Arrays.toString(param.args));
      //if (param.args.length == 7) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        // 开始记录信息
        String threadPoolInfoKey = Integer.toHexString(param.thisObject.hashCode());
        ThreadPoolInfo threadPoolInfo = new ThreadPoolInfo();
        threadPoolInfo.createStackTrace = StackTraceUtils.string(
            stackTraceElements,
            true,
            this.getClass().getName()
        );
        threadPoolInfo.key = threadPoolInfoKey;
        threadPoolInfoMap.put(threadPoolInfoKey, threadPoolInfo);
        StackTraceUtils.print(
            TAG,
            stackTraceElements,
            "THREAD POOL CREATE",
            true,
            this.getClass().getName()
        );
      //}
    }
  }

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
  }

  static class WorkerConstructorHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      //xLog.e(TAG, "WorkerConstructorHook: " + Arrays.toString(param.args));
      String workerKey = Integer.toHexString(param.thisObject.hashCode());
      //xLog.e(TAG, "WorkerConstructorHook workerKey:" + workerKey);
      if( param.args[1] instanceof Runnable ) { // 内部类，第一个参数是外部类
        // link worker and runnable
        String runnableKey = Integer.toHexString(param.args[1].hashCode());
        //xLog.e(TAG, "WorkerConstructorHook runnableKey:" + runnableKey);
        workerRunnableMap.put(workerKey, runnableKey);
      }
    }
  }

  static class ThreadConstructorHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      //xLog.e(TAG, "Thread constructor: " + Arrays.toString(param.args));
      StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
      ThreadInfo threadInfo = new ThreadInfo();

      String threadKey = Integer.toHexString(param.thisObject.hashCode());
      threadInfo.createStackTrace = StackTraceUtils.string(
          stackTraceElements,
          true,
          this.getClass().getName()
      );
      threadInfoMap.put(threadKey, threadInfo);

      boolean hasRunnable = (param.args.length == 1 && param.args[0] instanceof Runnable) ||
          (param.args.length > 1 && param.args[1] instanceof Runnable);
      //xLog.e(TAG, "ThreadConstructorHook hasRunnable:" + hasRunnable);
      // 获取 runnable
      String workerKey = "";
      if (hasRunnable) {
        Object runnable = param.args[0] instanceof Runnable ? param.args[0] : param.args[1];
        workerKey = Integer.toHexString(runnable.hashCode());
      }
      //xLog.e(TAG, "ThreadConstructorHook workerKey:" + workerKey);
      if (workerRunnableMap.containsKey(workerKey)) {
        // 需要和 thread pool 绑定
        String runnableKey = workerRunnableMap.get(workerKey);
        ThreadPoolInfo threadPoolInfo = runnableThreadPoolMap.get(runnableKey);
        threadInfo.threadPoolInfoKey = threadPoolInfo.key;
        // 建立 thread 和 thread pool 的关联后，断开 runnable 和 thread pool 的关联
        runnableThreadPoolMap.remove(runnableKey);
        workerRunnableMap.remove(workerKey);
        threadPoolInfo.addThreadInfo(threadInfo);
      } else {
        // 和线程池没有关联的 thread ,打印线程池的创建调用链
        StackTraceUtils.print(
            TAG,
            stackTraceElements,
            "CREATE THREAD",
            true,
            this.getClass().getName()
        );
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
      threadInfo.startStackTrace = StackTraceUtils.string(
          stackTraceElements,
          true,
          this.getClass().getName()
      );
      if (null == threadInfo.threadPoolInfoKey) {
        // 说明和 thread pool 没有关联，直接打印
        StackTraceUtils.print(
            TAG,
            stackTraceElements,
            "THREAD START",
            true,
            this.getClass().getName()
        );
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
      threadInfoMap.remove(threadKey);

      String threadPoolInfoKey = threadInfo.threadPoolInfoKey;
      ThreadPoolInfo threadPoolInfo = threadPoolInfoMap.get(threadPoolInfoKey);
      if (null != threadPoolInfo) {
        threadPoolInfo.removeThreadInfo(threadInfo);
        if (threadPoolInfo.isEmpty()) {
          threadPoolInfoMap.remove(threadPoolInfoKey);
        }
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
