package com.xander.performance;

import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

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
class ThreadTool {

  private static String TAG = PERF.TAG + "_ThreadTool";

  static class ThreadIssue extends Issue {
    String key;
    String threadPoolKey;
    boolean lostCreateTrace = false;
    List<String> createTrace;
    List<String> startTrace;

    public ThreadIssue(String msg) {
      super(Issue.TYPE_THREAD, msg, null);
    }

    @Override
    protected void buildOtherString(StringBuilder sb) {
      if (!lostCreateTrace) {
        sb.append("create trace:\n");
        buildListString(sb, createTrace);
      }
      sb.append("start trace:\n");
      buildListString(sb, startTrace);
    }
  }

  static class ThreadPoolIssue extends Issue {
    String key;
    boolean lostCreateTrace = false;
    List<String> createTrace;
    List<ThreadIssue> childThreadList = new ArrayList<>();

    public ThreadPoolIssue(String msg) {
      super(Issue.TYPE_THREAD, msg, null);
    }

    @Override
    protected void buildOtherString(StringBuilder sb) {
      if (!lostCreateTrace) {
        sb.append("create trace:\n");
      } else {
        sb.append("one thread create trace:\n");
      }
      buildListString(sb, createTrace);
    }

    void removeThreadInfo(ThreadIssue threadIssues) {
      synchronized (this) {
        childThreadList.remove(threadIssues);
      }
    }

    void addThreadInfo(ThreadIssue threadIssues) {
      synchronized (this) {
        childThreadList.add(threadIssues);
      }
    }

    boolean isEmpty() {
      return childThreadList.isEmpty();
    }
  }

  /**
   * thread 的信息，包括线程池里面创建的，可以通过 ThreadInfo.threadPoolInfoKey 字段判断是否为线程池创建
   */
  static ConcurrentHashMap<String, ThreadIssue> threadInfoMap = new ConcurrentHashMap<>(64);

  /**
   * 线程池信息
   */
  static ConcurrentHashMap<String, ThreadPoolIssue> threadPoolInfoMap = new ConcurrentHashMap<>(32);

  /**
   * worker 和 thread pool 的关联，用于绑定 thread 和 thread pool 的关联
   */
  static ConcurrentHashMap<String, String> workerThreadPoolMap = new ConcurrentHashMap<>(32);

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
      Constructor<?>[] constructors = ThreadPoolExecutor.class.getDeclaredConstructors();
      for (int i = 0; i < constructors.length; i++) {
        if (constructors[i].getParameterTypes().length > 6) {
          continue;
        }
        DexposedBridge.hookMethod(constructors[i], constructorHook);
      }

      // 根据构造方法里面的 runnable 是否为 Worker 可知是否为线程池创建的线程。
      DexposedBridge.hookAllConstructors(Thread.class, new ThreadConstructorHook());
      DexposedBridge.findAndHookMethod(Thread.class, "start", new ThreadStartHook());
      DexposedBridge.findAndHookMethod(Thread.class, "run", new ThreadRunHook());

      // java.util.concurrent.ThreadPoolExecutor$Worker 是一个内部类，
      // 所以构造方法第一参数就是 ThreadPoolExecutor, 所以构造方法可以将 worker 和 线程池绑定
      // 同时，run 方法执行完，表示线程池里面的一个线程执行完。可以考虑在里面做一些清理工作
      DexposedBridge.hookAllConstructors(
          Class.forName("java.util.concurrent.ThreadPoolExecutor$Worker"),
          new WorkerConstructorHook()
      );

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class ThreadPoolExecutorConstructorHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      // xLog.e(TAG, "Thread pool constructor: " + Arrays.toString(param.args));
      String threadPoolInfoKey = Integer.toHexString(param.thisObject.hashCode());
      if (threadPoolInfoMap.containsKey(threadPoolInfoKey)) {
        return;
      }
      // 开始记录信息
      ThreadPoolIssue threadPoolIssues = new ThreadPoolIssue("THREAD POOL CREATE");
      threadPoolIssues.key = threadPoolInfoKey;
      threadPoolIssues.createTrace = StackTraceUtils.list();
      threadPoolInfoMap.put(threadPoolInfoKey, threadPoolIssues);
      threadPoolIssues.print();
    }
  }

  static class WorkerConstructorHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      // xLog.e(TAG, "WorkerConstructorHook: " + Arrays.toString(param.args));
      String workerKey = Integer.toHexString(param.thisObject.hashCode());
      String threadPoolKey = Integer.toHexString(param.args[0].hashCode());
      // xLog.e(TAG, "WorkerConstructorHook workerKey:" + workerKey);
      // xLog.e(TAG, "WorkerConstructorHook threadPoolKey:" + threadPoolKey);
      workerThreadPoolMap.put(workerKey, threadPoolKey);
    }
  }

  static class ThreadConstructorHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      // xLog.e(TAG, "Thread constructor: " + Arrays.toString(param.args));
      ThreadIssue threadIssues = new ThreadIssue("THREAD CREATE");
      String threadKey = Integer.toHexString(param.thisObject.hashCode());
      threadIssues.key = threadKey;
      threadIssues.createTrace = StackTraceUtils.list();

      boolean hasRunnable = (param.args.length == 1 && param.args[0] instanceof Runnable)
          || (param.args.length > 1 && param.args[1] instanceof Runnable);

      // xLog.e(TAG, "ThreadConstructorHook hasRunnable:" + hasRunnable);
      // 获取 runnable
      String workerKey = "";
      if (hasRunnable) {
        Object runnable = param.args[0] instanceof Runnable ? param.args[0] : param.args[1];
        // xLog.e(TAG, "ThreadConstructorHook hasRunnable:" + runnable.getClass().getName());
        if ("java.util.concurrent.ThreadPoolExecutor$Worker".equals(runnable.getClass().getName())) {
          workerKey = Integer.toHexString(runnable.hashCode());
        }
      }
      // xLog.e(TAG, "ThreadConstructorHook workerKey:" + workerKey);
      // xLog.e(TAG, "ThreadConstructorHook workerThreadPoolMap.containsKey(workerKey):" + workerThreadPoolMap.containsKey(workerKey));
      if (workerThreadPoolMap.containsKey(workerKey)) {
        // 线程创建的线程
        String threadPoolKey = workerThreadPoolMap.get(workerKey);
        ThreadPoolIssue threadPoolIssues = threadPoolInfoMap.get(threadPoolKey);
        if (threadPoolIssues == null) {
          // 部分情况下，比如库初始化的比较晚，部分线程池已经创建了，就会出现线程创建时没有对应的线程池
          // 这里用线程的创建调用链来代替线程池的创建调用链，多少有些参考价值
          threadPoolIssues = new ThreadPoolIssue("THREAD POOL CREATE");
          threadPoolIssues.key = threadPoolKey;
          threadPoolIssues.lostCreateTrace = true;
          threadPoolIssues.createTrace = StackTraceUtils.list();
          threadPoolInfoMap.put(threadPoolKey, threadPoolIssues);
          threadPoolIssues.print();
        }
        threadIssues.threadPoolKey = threadPoolKey;
        threadPoolIssues.addThreadInfo(threadIssues);
        // 建立 thread 和 thread pool 的关联后，断开 worker 和 thread pool 的关联
        // 因为，Worker 只创建一个 Thread
        workerThreadPoolMap.remove(workerKey);
      }
      threadInfoMap.put(threadKey, threadIssues);
    }
  }

  static class ThreadStartHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      //super.beforeHookedMethod(param);
      //xLog.e(TAG, "ThreadStartHook:" + Arrays.toString(param.args));
      String threadKey = Integer.toHexString(param.thisObject.hashCode());
      ThreadIssue threadIssues = threadInfoMap.get(threadKey);
      if (null == threadIssues) {
        xLog.e(TAG, "can not find thread info when thread start !!!!!!");
        threadIssues = new ThreadIssue("THREAD CREATE");
        threadIssues.key = threadKey;
        threadIssues.lostCreateTrace = true;
        threadInfoMap.put(threadKey, threadIssues);
      }
      if (TextUtils.isEmpty(threadIssues.threadPoolKey)) {
        // 非线程池创建的线程
        threadIssues.startTrace = StackTraceUtils.list();
        threadIssues.print();
      }
    }
  }

  static class ThreadRunHook extends XC_MethodHook {

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      // 线程 run 方法走完，线程即将被销毁，删除 thread info 相关的记录。
      String threadKey = Integer.toHexString(param.thisObject.hashCode());
      ThreadIssue threadIssues = threadInfoMap.get(threadKey);
      if (null == threadIssues) {
        xLog.e(TAG, "can not find thread info after thread run !!!!!!");
        return;
      }
      threadInfoMap.remove(threadKey);
      xLog.e(TAG, "threadInfoMap size:" + threadInfoMap.size());
      if (TextUtils.isEmpty(threadIssues.threadPoolKey)) {
        return;
      }
      ThreadPoolIssue threadPoolIssues = threadPoolInfoMap.get(threadIssues.threadPoolKey);
      if (null == threadPoolIssues) {
        xLog.e(TAG, "can not find thread pool info after thread run  !!!!!!");
        return;
      }
      threadPoolIssues.removeThreadInfo(threadIssues);
      if (threadPoolIssues.isEmpty()) {
        threadPoolInfoMap.remove(threadIssues.threadPoolKey);
        xLog.e(TAG, "threadPoolInfoMap size:" + threadPoolInfoMap.size());
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
