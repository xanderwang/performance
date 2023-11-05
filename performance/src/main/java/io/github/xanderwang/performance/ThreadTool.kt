package io.github.xanderwang.performance

import android.os.Handler
import android.os.Looper
import io.github.xanderwang.asu.ALog.d
import io.github.xanderwang.asu.ALog.e
import io.github.xanderwang.asu.ALog.w
import io.github.xanderwang.hook.HookBridge.findAndHookMethod
import io.github.xanderwang.hook.HookBridge.hookAllConstructors
import io.github.xanderwang.hook.HookBridge.hookMethod
import io.github.xanderwang.hook.core.MethodHook
import io.github.xanderwang.hook.core.MethodParam
import io.github.xanderwang.performance.AppHelper.perfLooper
import java.lang.ref.SoftReference
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadPoolExecutor

/**
 * @Author: Xander
 * @CreateDate: 2020/4/13 22:30
 * @Version: 1.0
 * @ProjectName: performance
 * @Package: com.xander.performance
 * @ClassName: ThreadTool
 * @Description: 通过 hook 类的方法来监听线程的创建、启动，线程池的创建和执行任务
 *
 *
 * 监听线程的创建、启动：
 * 通过 hook ，在调用线程构造方法和线程 start 方法的时候，保存调用栈。
 *
 * 监听线程池的创建:
 * 通过 hook，在线程池构造方法被调用的时候，保存调用栈。
 *
 * 建立线程池和线程池创建的线程的关联：
 * 查阅源码，线程池创建线程，实际是通过其内部类 Worker 来创建的，由于 Worker 是内部类，
 * 所以 Worker 类实例和线程池的实例可以建立一个关联，Worker 创建线程的时候，也有一个关联，
 * 最后通过 Worker 这个纽带，就可以把线程池和线程池创建的线程关联起来监控。
 *
 */
internal object ThreadTool {

    private const val TAG = "ThreadTool"

    /**
     * thread 的信息，包括线程池里面创建的，可以通过 ThreadInfo.threadPoolInfoKey 字段判断是否为线程池创建
     */
    private var threadMap = ConcurrentHashMap<String, ThreadIssue>(64)

    /**
     * 线程池信息
     */
    private var threadPoolMap = ConcurrentHashMap<String, ThreadPoolIssue>(32)

    /**
     * worker 和 thread pool 的关联，用于绑定 thread 和 thread pool 的关联
     */
    private var workerThreadPoolMap = ConcurrentHashMap<String, String>(32)

    /** thread 里面的 target 变量 */
    private var THREAD_TARGET: Field? = null

    /** java.util.concurrent.ThreadPoolExecutor$Worker 里面的 thread 变量 */
    var WORKER_THREAD: Field? = null

    /** 主线程阻塞的话，可能会导致实际执行时刻和期望执行时刻不同步 */
    private var threadTraceHandler: Handler? = null

    private var dumpTaskMap: MutableMap<String, DumpThreadTraceTask> = HashMap()

    /**
     * 保存线程池创建信息
     *
     * @param threadPoolKey
     * @param msg
     * @param createTrace
     */
    private fun saveThreadPoolCreateInfo(threadPoolKey: String, msg: String, createTrace: Array<StackTraceElement>) {
        if (threadPoolMap.containsKey(threadPoolKey)) {
            return
        }
        // 开始记录信息
        val threadPoolIssues = ThreadPoolIssue(msg)
        threadPoolIssues.key = threadPoolKey
        threadPoolIssues.createTrace = Utils.list(createTrace)
        threadPoolMap[threadPoolKey] = threadPoolIssues
        threadPoolIssues.print()
    }

    /**
     * 线程的创建信息，这个暂时可以不用关系，因为后续会关心 start
     *
     * @param threadKey
     * @param workerKey
     * @param createTrace
     */
    @Deprecated("")
    private fun saveThreadCreateInfo(threadKey: String, workerKey: String, createTrace: Array<StackTraceElement>) {
        val threadInfo = ThreadIssue("THREAD CREATE")
        threadInfo.key = threadKey
        threadInfo.createTrace = Utils.list(createTrace)
        val isInThreadPool = workerThreadPoolMap.containsKey(workerKey)
        w(TAG, "saveThreadCreateInfo: is in thread pool:%s", isInThreadPool)
        // aLog.d(TAG, "saveThreadCreateInfo: createTrace:%s", StackTraceUtils.list(createTrace));
        if (isInThreadPool) {
            // 线程池创建的线程
            val threadPoolKey = workerThreadPoolMap[workerKey]
            var threadPoolInfo = threadPoolMap[threadPoolKey]
            if (threadPoolInfo == null && threadPoolKey?.isNotEmpty() == true) {
                // 部分情况下，比如库初始化的比较晚，部分线程池已经创建了，就会出现线程创建时没有对应的线程池
                // 这里用线程的创建调用链来代替线程池的创建调用链，多少有些参考价值
                threadPoolInfo = ThreadPoolIssue("THREAD POOL LOST CREATE INFO!!!")
                threadPoolInfo.key = threadPoolKey
                threadPoolInfo.lostCreateTrace = true
                threadPoolInfo.createTrace = Utils.list(createTrace)
                threadPoolMap[threadPoolKey] = threadPoolInfo
                threadPoolInfo.print()
            }
            threadInfo.threadPoolKey = threadPoolKey
            threadPoolInfo?.addThreadInfo(threadInfo)
            // 建立 thread 和 thread pool 的关联后，断开 worker 和 thread pool 的关联
            // 因为，Worker 只创建一个 Thread
            workerThreadPoolMap.remove(workerKey)
        }
        // 需要？
        threadMap[threadKey] = threadInfo
    }

    /**
     * 线程启动了
     *
     * @param threadKey
     * @param threadName
     * @param startTrace
     */
    private fun saveStartThreadInfo(threadKey: String, threadName: String, startTrace: Array<StackTraceElement>) {
        var threadInfo = threadMap[threadKey]
        if (null == threadInfo) {
            e(TAG, "can not find thread info when thread start !!!!!!")
            threadInfo = ThreadIssue("THREAD CREATE LOST CREATE INFO")
            threadInfo.key = threadKey
            threadInfo.lostCreateTrace = true
            threadMap[threadKey] = threadInfo
        }
        threadInfo.threadName = threadName
        if (threadInfo.threadPoolKey?.isEmpty() == true) {
            // 非线程池创建的线程才打印启动堆栈
            threadInfo.startTrace = Utils.list(startTrace)
            threadInfo.print()
        } else {
            // 线程池创建的线程，这里暂时不做任何事情
            w(TAG, "a thread pool created thread start !!!!!!")
        }
    }

    /**
     * 关联线程池和 worker
     *
     * @param threadPoolKey
     * @param workerKey
     */
    private fun linkThreadPoolAndWorker(threadPoolKey: String, workerKey: String) {
        workerThreadPoolMap[workerKey] = threadPoolKey
    }

    /**
     * 关联线程池和线程池创建的线程
     *
     * @param threadKey
     * @param workerKey
     * @param createTrace
     */
    private fun linkThreadAndThreadPool(threadKey: String, workerKey: String, createTrace: Array<StackTraceElement>?) {
        val threadInfo = ThreadIssue("THREAD CREATE")
        threadInfo.lostCreateTrace = true
        threadInfo.key = threadKey
        val isInThreadPool = workerThreadPoolMap.containsKey(workerKey)
        w(TAG, "linkThreadAndThreadPool: thread is in thread pool: $isInThreadPool")
        if (isInThreadPool) {
            // 线程池创建的线程
            val threadPoolKey = workerThreadPoolMap[workerKey]
            var threadPoolInfo = threadPoolMap[threadPoolKey]
            if (threadPoolInfo == null && threadPoolKey?.isNotEmpty() == true) {
                // 部分情况下，比如库初始化的比较晚，部分线程池已经创建了，就会出现线程创建时没有对应的线程池
                // 这里用线程的创建调用链来代替线程池的创建调用链，多少有些参考价值
                threadPoolInfo = ThreadPoolIssue("THREAD POOL LOST CREATE INFO!!!")
                threadPoolInfo.key = threadPoolKey
                threadPoolInfo.lostCreateTrace = true
                if (null != createTrace) {
                    threadPoolInfo.createTrace = Utils.list(createTrace)
                }
                threadPoolMap[threadPoolKey] = threadPoolInfo
                threadPoolInfo.print()
            }
            threadInfo.threadPoolKey = threadPoolKey
            threadPoolInfo?.addThreadInfo(threadInfo)
            // 建立 thread 和 thread pool 的关联后，断开 worker 和 thread pool 的关联
            // 因为，Worker 只创建一个 Thread
            workerThreadPoolMap.remove(workerKey)
        }
        threadMap[threadKey] = threadInfo
    }

    /**
     * 清除一些信息
     *
     * @param threadKey
     */
    private fun clearInfoWhenExitThread(threadKey: String) {
        d(TAG, "clear info when exit thread, threadKey:%s", threadKey)
        val threadInfo = threadMap.remove(threadKey)
        if (null == threadInfo) {
            e(TAG, "can not find thread info when exit thread!!!")
            return
        }
        d(TAG, "clear info when exit thread, thread name:%s", threadInfo.threadName)
        d(TAG, "clear info when exit thread, running thread count:%s", threadMap.size)
        if (threadInfo.threadPoolKey?.isEmpty() == true) {
            // 非线程池创建的线程
            return
        }
        val threadPool = threadPoolMap[threadInfo.threadPoolKey]
        if (null == threadPool) {
            // 是线程池创建的线程，但是找不到线程池的信息
            e(TAG, "can not find thread pool info when exit thread!!!")
            return
        }
        threadPool.removeThreadInfo(threadInfo)
        if (threadPool.isEmpty) {
            threadPoolMap.remove(threadInfo.threadPoolKey)
        }
        d(TAG, "clear info when exit thread, thread pool count:%s", threadPoolMap.size)
    }

    /**
     * thread run 方法开始执行
     * 会执行一个延时的 runnable ，如果长时间还没有执行完 run 方法，表示这个线程可能 block
     * @param thread
     */
    private fun threadRunStart(thread: Thread?) {
        thread ?: return
        val threadKey = Integer.toHexString(thread.hashCode())
        if (dumpTaskMap.containsKey(threadKey)) {
            // 这种情况会在 kt 里面创建线程的时候发生，目前发现到的是 kt 代码创建的线程貌似会缓存起来。
            // kt 创建的线程的 `run 的方法块`执行完后，不会立即结束 thread 的 run 方法。 kt 会缓存线程下来。
            // 所以如果是 kt 的线程`run 的方法块`执行了，就移除之前的信息，保证准确性。
            val oldDumpTask = dumpTaskMap.remove(threadKey)
            oldDumpTask?.let { threadTraceHandler?.removeCallbacks(it) }
        }
        // val dumpTask = DumpThreadTraceTask(Thread.currentThread())
        val dumpTask = DumpThreadTraceTask(thread)
        dumpTaskMap[threadKey] = dumpTask
        threadTraceHandler?.postDelayed(dumpTask, Config.THREAD_BLOCK_TIME)
        e(TAG, "threadRunStart:dumpTaskMap size:%s,delayTime:%s", dumpTaskMap.size, Config.THREAD_BLOCK_TIME)
    }

    /**
     * thread run 方法执行结束
     * 清理 threadRunStart 里面的 block 任务
     *
     * @param thread
     */
    private fun threadRunEnd(thread: Thread?) {
        thread ?: return
        val threadKey = Integer.toHexString(thread.hashCode())
        val dumpTask = dumpTaskMap.remove(threadKey)
        dumpTask?.let { threadTraceHandler?.removeCallbacks(it) }
        if (null == dumpTask) {
            e(TAG, "RunnableRunHook afterHookedMethod null task!!!", Throwable())
        }
    }

    /**
     * 线程优先级改变
     */
    private fun threadPriorityChanged() {
        // Priority
        val traceIssue = ThreadTraceIssue("THREAD PRIORITY CHANGED TRACE", Utils.list(Thread.currentThread()))
        traceIssue.threadName = Thread.currentThread().name
        traceIssue.print()
    }

    fun init() {
        e(TAG, "init")
        threadTraceHandler = Handler(perfLooper!!)
        hookThread()
    }

    private fun hookThread() {
        // ThreadPoolExecutor
        // hook 7 个参数的构造方法好像会报错，故 hook 指定参数数目的构造方法
        val threadPoolConstructorCallback = ThreadPoolExecutorConstructorCallback()
        val threadPoolConstructors = ThreadPoolExecutor::class.java.declaredConstructors
        threadPoolConstructors.forEach { constructor ->
            if (constructor.parameterTypes.size <= 6) {
                // 7 个参数的构造方法，貌似 hook 有问题，暂时不 hook。但是这里还是有些问题的 fixme
                hookMethod(constructor, threadPoolConstructorCallback)
            }
        }
        // java.util.concurrent.ThreadPoolExecutor$Worker
        try {
            // java.util.concurrent.ThreadPoolExecutor$Worker 是一个内部类，
            // 所以构造方法第一参数就是 ThreadPoolExecutor, 所以构造方法可以将 Worker 和 线程池绑定
            // java.util.concurrent.ThreadPoolExecutor$Worker
            val workerClass = Class.forName("java.util.concurrent.ThreadPoolExecutor\$Worker")
            WORKER_THREAD = workerClass.getDeclaredField("thread")
            WORKER_THREAD?.isAccessible = true
            hookAllConstructors(workerClass, WorkerConstructorCallback())
        } catch (e: Exception) {
            e(TAG, "java.util.concurrent.ThreadPoolExecutor\$Worker", e)
        }

        // 根据构造方法里面的 runnable 是否为 Worker 可知是否为线程池创建的线程。
        //    HookBridge.hookAllConstructors(Thread.class, new ThreadConstructorHook());
        //    HookBridge.findAndHookMethod(Thread.class, "init", new ThreadConstructorHook());
        //    aLog.e(TAG, "findAndHookMethod Thread.init");
        try {
            THREAD_TARGET = Thread::class.java.getDeclaredField("target")
        } catch (e: Exception) {
            e(TAG, "THREAD_TARGET error", e)
        }

        // Thread start 方法
        findAndHookMethod(Thread::class.java, "start", ThreadStartCallback())

        // Thread run 方法执行完，表示线程执行完。可以考虑在里面做一些清理工作，目前发现还是有问题
        findAndHookMethod(Thread::class.java, "run", ThreadRunCallback())
        // kotlin 的 ThreadRunnable
        try {
            // kotlin 的线程执行和 java 的不一致，这里需要做个区分
            val ktThread = Class.forName("kotlin.concurrent.ThreadsKt\$thread\$thread$1")
            findAndHookMethod(ktThread, "run", KtThreadRunCallback())
        } catch (e: ClassNotFoundException) {
            e(TAG, "kotlin.concurrent.ThreadsKt\$thread\$thread$1", e)
        }

        // Thread setPriority
        findAndHookMethod(Thread::class.java, "setPriority", Int::class.javaPrimitiveType, ThreadSetPriorityCallback())
    }

    /**
     * 线程创建或者 start issue
     */
    internal class ThreadIssue(msg: String) : Issue(TYPE_THREAD, msg, null) {
        /** 线程 key ，线程实例的 hashCode */
        var key: String? = null

        /** 与之对应的线程池的 key ,如果不是线程池创建的线程，取值为 null */
        var threadPoolKey: String? = null

        /** 是否丢失创建实例调用栈，如果库初始化比较晚，可能会出现没有创建实例调用栈 */
        var lostCreateTrace = false

        /**  */
        var threadName: String? = null

        /** 创建实例调用栈 */
        var createTrace: List<String?>? = null

        /** 启动线程调用栈 */
        var startTrace: List<String?>? = null

        override fun formatExtraInfo(sb: StringBuilder?) {
            sb?.append("thread name: ")?.append(threadName)?.append("\n")
            if (!lostCreateTrace) {
                sb?.append("thread create trace:\n")
                formatDataList(sb, createTrace)
            }
            sb?.append("thread start trace:\n")
            formatDataList(sb, startTrace)
        }
    }

    /**
     * 线程池创建或者 start issue
     */
    internal class ThreadPoolIssue(msg: String) : Issue(TYPE_THREAD, msg, null) {
        /** 线程池 key ，线程池实例的 hashCode */
        var key: String? = null

        /** 是否丢失创建线程池实例的调用栈，如果库初始化比较晚，可能会出现没有创建实例调用栈 */
        var lostCreateTrace = false

        /** 创建线程池实例的调用栈 */
        var createTrace: List<String?>? = null

        /** 创建的线程，如果线程销毁后，会自动删除 */
        var childThreadList: MutableList<ThreadIssue> = ArrayList()

        override fun formatExtraInfo(sb: StringBuilder?) {
            if (!lostCreateTrace) {
                sb?.append("thread pool create trace:\n")
            } else {
                // 这种情况下，用某个线程的创建栈来代替，尽量输出一些信息
                sb?.append("one thread create trace:\n")
            }
            formatDataList(sb, createTrace)
        }

        fun removeThreadInfo(threadIssues: ThreadIssue) {
            synchronized(this) { childThreadList.remove(threadIssues) }
        }

        fun addThreadInfo(threadIssues: ThreadIssue) {
            synchronized(this) { childThreadList.add(threadIssues) }
        }

        val isEmpty: Boolean
            get() = childThreadList.isEmpty()
    }

    /**
     * 线程 block、调整优先级等时候的 issue
     */
    internal class ThreadTraceIssue(msg: String, data: Any?) : Issue(TYPE_THREAD, msg, data) {

        var threadName: String? = null
        override fun formatExtraInfo(sb: StringBuilder?) {
            sb?.append("thread name: ")?.append(threadName)?.append("\n")
        }
    }

    /**
     * 打印某个线程当前调用栈
     */
    internal class DumpThreadTraceTask(thread: Thread) : Runnable {

        private var threadRef: SoftReference<Thread> = SoftReference(thread)

        override fun run() {
            val thread = threadRef.get() ?: return
            val traceIssue = ThreadTraceIssue("THREAD RUN BLOCK TRACE", Utils.list(thread))
            traceIssue.threadName = thread.name
            traceIssue.print()
            threadRef.clear()
        }
    }

    /**
     * 线程池的构造方法 hook callback
     */
    internal class ThreadPoolExecutorConstructorCallback : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            val threadPool = param?.thisObject ?: return
            val threadPoolInfoKey = Integer.toHexString(threadPool.hashCode())
            d(TAG, "ThreadPoolExecutorConstructor beforeHookedMethod:%s", Arrays.toString(param.args))
            d(TAG, "ThreadPoolExecutorConstructor beforeHookedMethod threadPoolInfoKey: $threadPoolInfoKey")
            saveThreadPoolCreateInfo(threadPoolInfoKey, "THREAD POOL CREATE", Throwable().stackTrace)
        }
    }

    /**
     * 线程池的 Worker 构造方法 hook callback
     */
    internal class WorkerConstructorCallback : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            val workerKey = Integer.toHexString(param?.thisObject.hashCode())
            val threadPoolKey = Integer.toHexString(param?.args?.get(0).hashCode())
            d(TAG, "Worker Constructor workerKey:$workerKey ,threadPoolKey:$threadPoolKey")
            if (workerKey.isNotEmpty() && threadPoolKey.isNotEmpty()) {
                linkThreadPoolAndWorker(threadPoolKey, workerKey)
            }
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            // 关联 thread 和 worker
            WORKER_THREAD?.let { workerThread ->
                workerThread.isAccessible = true
                workerThread.get(param?.thisObject)?.let { thread ->
                    val threadKey = Integer.toHexString(thread.hashCode())
                    val workerKey = Integer.toHexString(param?.thisObject.hashCode())
                    linkThreadAndThreadPool(threadKey, workerKey, null)
                }
            }
            if (null == WORKER_THREAD) {
                e(TAG, "WorkerConstructorHook afterHookedMethod WORKER_THREAD is null!!!")
            }
        }
    }

    /**
     * 线程的构造方法 hook callback
     */
    @Deprecated("")
    internal class ThreadConstructorCallback : MethodHook() {
        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            d(TAG, "ThreadConstructorHook afterHookedMethod:%s", Arrays.toString(param!!.args))
            val cThread = param.thisObject as Thread?
            val threadKey = Integer.toHexString(cThread.hashCode())
            // 获取 workerKey
            var workerKey = ""
            val args = param.args
            for (i in args!!.indices) {
                val argClassName = args[i]!!.javaClass.name
                //         aLog.e(TAG, "ThreadConstructorHook afterHookedMethod arg class name:%s", argClassName);
                if ("java.util.concurrent.ThreadPoolExecutor\$Worker" == argClassName) {
                    w(TAG, "ThreadConstructorHook afterHookedMethod find worker")
                    workerKey = Integer.toHexString(args[i].hashCode())
                }
            }
            w(TAG, "ThreadConstructorHook afterHookedMethod workerKey:%s", workerKey)
            saveThreadCreateInfo(threadKey, workerKey, Throwable().stackTrace)
        }
    }

    /**
     * Thread.start() 方法 hook callback
     */
    internal class ThreadStartCallback : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            val cThread = param?.thisObject as Thread?
            // Thread cThread = Thread.currentThread();
            val threadKey = Integer.toHexString(cThread.hashCode())
            cThread?.name?.let { saveStartThreadInfo(threadKey, it, Throwable().stackTrace) }
        }
    }

    /**
     * Thread.run() 方法 hook callback
     */
    internal class ThreadRunCallback : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            val cThread = param?.thisObject as Thread?
            // val cThread = Thread.currentThread()
            if (cThread === Looper.getMainLooper().thread) {
                d(TAG, "ThreadRunHook beforeHookedMethod in main thread")
                return
            }
            threadRunStart(cThread)
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            d(TAG, "ThreadRunHook afterHookedMethod")
            val cThread = param?.thisObject as Thread?
            threadRunEnd(cThread)
            val threadKey = Integer.toHexString(cThread.hashCode())
            clearInfoWhenExitThread(threadKey)
        }
    }

    /**
     * Runnable.run() 方法 hook callback
     */
    internal class KtThreadRunCallback : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            val cThread = Thread.currentThread()
            if (cThread === Looper.getMainLooper().thread) {
                d(TAG, "RunnableRunHook beforeHookedMethod in main thread")
                return
            }
            threadRunStart(cThread)
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            val cThread = Thread.currentThread()
            if (cThread === Looper.getMainLooper().thread) {
                d(TAG, "RunnableRunHook afterHookedMethod in main thread")
                return
            }
            threadRunEnd(cThread)
        }
    }

    //
    /**
     * java.lang.Thread.setPriority
     */
    internal class ThreadSetPriorityCallback : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            // super.beforeHookedMethod(param)
            threadPriorityChanged()
        }
    }

    /**
     * android.os.Process.setThreadPriority
     */
    internal class ProcessSetThreadPriorityCallback : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            // super.beforeHookedMethod(param)
            threadPriorityChanged()
        }
    }
}