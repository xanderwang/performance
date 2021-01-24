package com.xander.performance;

import android.text.TextUtils;

import com.xander.asu.aLog;
import com.xander.performance.hook.HookBridge;
import com.xander.performance.hook.core.MethodParam;
import com.xander.performance.hook.core.MethodHook;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @ProjectName: performance
 * @Package: com.xander.performance
 * @ClassName: ThreadTool
 * @Description: 通过 hook 类的方法来监听线程的创建、启动，线程池的创建和执行任务
 * <p>
 * 监听线程的创建、启动：主要原理就是通过 hook ，在调用线程构造方法和 start 方法的时候，保存调用栈。
 * 监听线程池的创建: 主要原理就是通过 hook，在线程池构造方法被调用的时候，保存调用栈。
 * 建立线程池和线程池创建的线程的关联：查阅源码，线程池创建线程，实际是通过其内部类 Worker 来创建的，
 * 由于是内部类，所以实际 Worker 类实例和线程池的实例可以建立一个关联，Worker 创建线程的时候，也有一个关联，
 * 最后通过 Worker 这个纽带，就可以把线程池和线程池创建的线程关联起来监控。
 * @Author: Xander
 * @CreateDate: 2020/4/13 22:30
 * @Version: 1.0
 */
class ThreadTool {

  private static final String TAG = "ThreadTool";

  static class ThreadIssue extends Issue {
    String       key; // 线程 key ，线程实例的 hashCode
    String       threadPoolKey; // 与之对应的线程池的 key ,如果不是线程池创建的线程，取值为 null
    boolean      lostCreateTrace = false; // 是否丢失创建实例调用栈，如果库初始化比较晚，可能会出现没有创建实例调用栈
    List<String> createTrace; // 创建实例调用栈
    List<String> startTrace;  // 启动线程调用栈

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
    String            key; // 线程池 key ，线程池实例的 hashCode
    boolean           lostCreateTrace = false; // 是否丢失创建线程池实例的调用栈，如果库初始化比较晚，可能会出现没有创建实例调用栈
    List<String>      createTrace; // 创建线程池实例的调用栈
    List<ThreadIssue> childThreadList = new ArrayList<>(); // 创建的线程，如果线程销毁后，会自动删除

    public ThreadPoolIssue(String msg) {
      super(Issue.TYPE_THREAD, msg, null);
    }

    @Override
    protected void buildOtherString(StringBuilder sb) {
      if (!lostCreateTrace) {
        sb.append("create trace:\n");
      } else {
        // 这种情况下，用某个线程的创建栈来代替，尽量输出一些信息
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

  static void init() {
    aLog.e(TAG, "init");
    hookWithEpic();
    //hookWithSandHook(); // sandhook 不是很好用，先注释
  }

  public static void hookWithEpic() {
    // hook 7 个参数的构造方法好像会报错，故 hook 指定参数数目的构造方法
    ThreadPoolExecutorConstructorHook constructorHook = new ThreadPoolExecutorConstructorHook();
    Constructor<?>[] constructors = ThreadPoolExecutor.class.getDeclaredConstructors();
    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length > 6) {
        continue;
      }
      HookBridge.hookMethod(constructors[i], constructorHook);
    }

    // java.util.concurrent.ThreadPoolExecutor$Worker 是一个内部类，
    // 所以构造方法第一参数就是 ThreadPoolExecutor, 所以构造方法可以将 Worker 和 线程池绑定
    Class<?> workerClass = null;
    try {
      workerClass = Class.forName("java.util.concurrent.ThreadPoolExecutor$Worker");
      HookBridge.hookAllConstructors(workerClass, new WorkerConstructorHook());
    } catch (ClassNotFoundException e) {
      aLog.ee(TAG, "java.util.concurrent.ThreadPoolExecutor$Worker", e);
    }

    // 根据构造方法里面的 runnable 是否为 Worker 可知是否为线程池创建的线程。
    HookBridge.hookAllConstructors(Thread.class, new ThreadConstructorHook());
    HookBridge.findAndHookMethod(Thread.class, "start", new ThreadStartHook());
    // run 方法执行完，表示线程执行完。可以考虑在里面做一些清理工作
    HookBridge.findAndHookMethod(Thread.class, "run", new ThreadRunHook());
  }

  static class ThreadPoolExecutorConstructorHook extends MethodHook {
    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      // aLog.e(TAG, "Thread pool constructor: " + Arrays.toString(param.args));
      String threadPoolInfoKey = Integer.toHexString(param.getThisObject().hashCode());
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

  static class WorkerConstructorHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      aLog.e(TAG, "WorkerConstructorHook: " + Arrays.toString(param.getArgs()));
      String workerKey = Integer.toHexString(param.getThisObject().hashCode());
      String threadPoolKey = Integer.toHexString(param.getArgs()[0].hashCode());
      // aLog.e(TAG, "WorkerConstructorHook workerKey:" + workerKey);
      // aLog.e(TAG, "WorkerConstructorHook threadPoolKey:" + threadPoolKey);
      workerThreadPoolMap.put(workerKey, threadPoolKey);
    }
  }

  static class ThreadConstructorHook extends MethodHook {

    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      aLog.e(TAG, "Thread constructor: " + Arrays.toString(param.getArgs()));
      ThreadIssue threadIssues = new ThreadIssue("THREAD CREATE");
      String threadKey = Integer.toHexString(param.getThisObject().hashCode());
      threadIssues.key = threadKey;
      threadIssues.createTrace = StackTraceUtils.list();

      boolean hasRunnable = (param.getArgs().length == 1 && param.getArgs()[0] instanceof Runnable) ||
          (param.getArgs().length > 1 && param.getArgs()[1] instanceof Runnable);

      aLog.e(TAG, "ThreadConstructorHook hasRunnable:" + hasRunnable);
      // 获取 runnable
      String workerKey = "";
      if (hasRunnable) {
        Object runnable = param.getArgs()[0] instanceof Runnable ? param.getArgs()[0] : param.getArgs()[1];
        aLog.e(TAG, "ThreadConstructorHook runnable class:" + runnable.getClass().getName());
        if ("java.util.concurrent.ThreadPoolExecutor$Worker".equals(runnable.getClass().getName())) {
          workerKey = Integer.toHexString(runnable.hashCode());
        }
      }
      aLog.e(TAG, "ThreadConstructorHook workerKey:" + workerKey);
      aLog.e(
          TAG,
          "ThreadConstructorHook workerThreadPoolMap.containsKey(workerKey):" +
              workerThreadPoolMap.containsKey(workerKey)
      );
      if (workerThreadPoolMap.containsKey(workerKey)) {
        // 线程池创建的线程
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

  static class ThreadStartHook extends MethodHook {

    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      //super.beforeHookedMethod(param);
      //aLog.e(TAG, "ThreadStartHook:" + Arrays.toString(param.args));
      String threadKey = Integer.toHexString(param.getThisObject().hashCode());
      ThreadIssue threadIssues = threadInfoMap.get(threadKey);
      if (null == threadIssues) {
        aLog.e(TAG, "can not find thread info when thread start !!!!!!");
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

  static class ThreadRunHook extends MethodHook {

    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      super.afterHookedMethod(param);
      // 线程 run 方法走完，线程即将被销毁，删除 thread info 相关的记录。
      String threadKey = Integer.toHexString(param.getThisObject().hashCode());
      ThreadIssue threadIssues = threadInfoMap.get(threadKey);
      if (null == threadIssues) {
        aLog.e(TAG, "can not find thread info after thread run !!!!!!");
        return;
      }
      threadInfoMap.remove(threadKey);
      aLog.e(TAG, "threadInfoMap size:" + threadInfoMap.size());
      if (TextUtils.isEmpty(threadIssues.threadPoolKey)) {
        return;
      }
      ThreadPoolIssue threadPoolIssues = threadPoolInfoMap.get(threadIssues.threadPoolKey);
      if (null == threadPoolIssues) {
        aLog.e(TAG, "can not find thread pool info after thread run  !!!!!!");
        return;
      }
      threadPoolIssues.removeThreadInfo(threadIssues);
      if (threadPoolIssues.isEmpty()) {
        threadPoolInfoMap.remove(threadIssues.threadPoolKey);
        aLog.e(TAG, "threadPoolInfoMap size:" + threadPoolInfoMap.size());
      }
    }
  }

}
