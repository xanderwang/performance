由于本人工作需要，需要解决一些性能问题，虽然有 Profiler 、Systrace 等工具，
但是无法实时监控，多少有些不方便，于是计划写一个能实时监控性能的小工具，
经过学习大佬们的文章，最终完成了这个开源的性能实时检测库。初步能达到预期效果，
这里做个记录，算是小结了。

开源库的地址是:

> [https://github.com/XanderWang/performance](https://github.com/XanderWang/performance)

幸苦各位能给个小小的 star 鼓励下。

这个性能检测库，可以检测以下问题：

- [x] UI 线程 block 检测
- [x] App 的 FPS 检测
- [x] 线程的创建和启动监控以及线程池的创建监控
- [x] IPC(进程间通讯)监控

同时还实现了以下功能

- [x] 实时通过 logcat 打印检测到的问题
- [x] 保存检测到的信息到文件
- [x] 提供上报信息文件接口

# 接入指南

1 在 APP 工程目录下面的 build.gradle 添加如下内容

```groovy
dependencies {
  debugImplementation "com.xander.performance:perf:0.1.10"
  releaseImplementation "com.xander.performance:perf-noop:0.1.10"
}
```

2 APP 工程的 Application 类新增类似如下初始化代码

```java
    private void initPerformanceTool(Context context) {
        PERF.Builder builder = new PERF.Builder()
            .globalTag("perf") // 检测库的全局 logcat 日志 tag ，设置后可以快速过滤日志，默认是 PERF
            .checkUI(true, 100) // 检查 UI 线程, 超过指定时间还未结束，会被认为 ui 线程 block，时间单位为 ms
            .checkThread(true) // 检查线程和线程池的创建
            .checkFps(true) // 检查 App 的 FPS，还可以额外指定检测时间间隔，时间间隔单位为 ms
            .checkIPC(true) // 检查 IPC 调用
            .issueSupplier(new PERF.IssueSupplier() {
                @Override
                public long maxCacheSize() {
                    // issue 文件缓存的最大空间
                    return 1024 * 1024 * 20; 
                }

                @Override
                public File cacheRootDir() {
                    // issue 文件保存的根目录 
                    return getApplicationContext().getCacheDir(); 
                }

                @Override
                public boolean upLoad(File file) {
                    // 上传入口，返回 true 表示上传成功
                    return false;
                }
            }).build();
        PERF.init(builder);
    }
```

# 主要更新记录

- 0.1.10 FPS 的检测时间间隔从默认 2s 调整为 1s，同时支持自定义时间间隔。
- 0.1.9  优化线程池创建的监控。
- 0.1.8  初版发布，完成基本的功能。

不建议直接在线上使用这个库，在编写这个库，测试 hook 的时候，在不同的机器和 rom 上，会有不同的问题，
这里建议先只在线下自测使用这个检测库。

# 后期计划

- IPC 的监控目前只能监控调用，无法检测 IPC 调用耗时时间，后期计划按照耗时时间来检测。
- Issue 保存到文件的逻辑优化，目前写的自我感觉不是很好，后面计划优化下。
- 暂时发现不同的平台多少有些问题，故不建议线上环境直接上线，后期希望能优化下，做出可以上线的版本。

# 原理介绍

## UI 线程 block 检测原理

主要参考了 `AndroidPerformanceMonitor` 库的思路，对 UI 线程的 `Looper` 里面处理的 `Message` 过程进行监控。
在 `Looper` 开始处理 `Message` 前，在异步线程开启一个延时任务，用于后续收集信息。如果这个 `Message` 在指定的
时间段内完成了处理，那么在这个 `Message` 被处理完后，就取消之前的延时任务，说明 UI 线程没有 block 。如果在指定
的时间段内没有完成任务，说明 UI 线程有 block ，在判断发生 block 的同时，我们可以在异步线程执行刚才的延时任务，
如果我们在这个延时任务里面打印 UI 线程的方法调用栈，就可以知道 UI 线程在做什么了。

但是这个方案有一个缺点，就是无法处理 `InputManager` 的输入事件，比如 TV 端的遥控按键事件。通过按键事件的调用方法
链进行分析，最终每个按键事件都调用了 `DecorView` 类的 `dispatchKeyEvent` 方法，而非 `Looper` 的 loop Message
流程。所以 `AndroidPerformanceMonitor` 库是无法准确监控 TV 端应用的耗时情况。针对 TV 端应用按键处理，
需要找到一个新的切入点，这个切入点就是刚刚的 `DecorView` 类的 `dispatchKeyEvent` 方法。那如何介入 `DecorView` 
类的 `dispatchKeyEvent` 方法呢？我们通过 `epic` 库来 `hook` 这个方法的调用，`hook` 成功后，我们可以在 `DecorView` 类的
 `dispatchKeyEvent` 方法调用前后都接收到一个回调方法，在 `dispatchKeyEvent` 方法调用前我们可以在异步线程执行
一个延时任务，在 `dispatchKeyEvent` 方法调用后，取消这个延时任务。如果 `dispatchKeyEvent` 方法耗时时间小于
指定的时间阈值，可以认为没有 block ，此时移除了延时任务。如果 `dispatchKeyEvent` 方法耗时时间大于指定的时间阈值
说明此时 UI  线程是有 block 的，此时，就会执行这个延时任务来收集必要的信息。

以上就是 UI 线程 block 的检测原理了，目前做的还比较粗糙，后续可以考虑参考 `AndroidPerformanceMonitor` 打印 CPU 、内存等更多的信息。

最终终端 log 打印效果如下：
```
com.xander.performace.demo W/demo_Issue: =================================================
    type: UI BLOCK
    msg: UI BLOCK
    create time: 2021-01-13 11:24:41
    trace:
    	java.lang.Thread.sleep(Thread.java:-2)
    	java.lang.Thread.sleep(Thread.java:442)
    	java.lang.Thread.sleep(Thread.java:358)
    	com.xander.performance.demo.MainActivity.testANR(MainActivity.kt:49)
    	java.lang.reflect.Method.invoke(Method.java:-2)
    	androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener.onClick(AppCompatViewInflater.java:397)
    	android.view.View.performClick(View.java:7496)
    	android.view.View.performClickInternal(View.java:7473)
    	android.view.View.access$3600(View.java:831)
    	android.view.View$PerformClick.run(View.java:28641)
    	android.os.Handler.handleCallback(Handler.java:938)
    	android.os.Handler.dispatchMessage(Handler.java:99)
    	android.os.Looper.loop(Looper.java:236)
    	android.app.ActivityThread.main(ActivityThread.java:7876)
    	java.lang.reflect.Method.invoke(Method.java:-2)
    	com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:656)
    	com.android.internal.os.ZygoteInit.main(ZygoteInit.java:967)
```

## App 的 FPS 检测的原理

FPS 检测的原理，利用了 Android 的屏幕绘制原理。

这里简单说下 Android 的屏幕绘制原理。

系统每隔 16 ms 就会发送一个 `VSync` 信号，如果 App 注册了这个 `VSync` 信号，就会在 `VSync` 信号到来的时候，收到回调，
从而开始准备绘制，如果准备顺利，也就是 cpu 准备好数据， gpu 栅格化完成。如果这些任务在 16 ms 之内完成，那么下一个
 `VSync` 信号到来的时候就可以绘制这一帧界面了。这个准备好的画面就会被显示出来。如果没准备好，可能就需要 32 ms 后
或者更久的时间后，才能准备好，这个画面才能显示出来，这种情况下就发生了丢帧。

上面提到了 `VSync` 信号，当 `VSync` 信号到来的时候会通知应用开始准备绘制，具体的通知细节不做表述。大概的原理就是，
开始准备绘制前，往 `MessageQueue` 里面放一个同步屏障，这样 UI 线程就只会处理异步消息，直到同步屏障被移除，
然后 App 注册一个 `VSync` 信号监听，当 `VSync` 信号到达的时候，给 `MessageQueue` 里面放一个异步 `Message` 。
由于之前 `MessageQueue` 里有了一个同步屏障消息，所有后续 `UI` 线程会优先处理这个异步 `Message` 。
这个异步 `Message` 做的事情就是从  `ViewRootImpl` 开始了我们熟悉的 `measure` 、`layout` 和 `draw` 。

检测 FPS 的原理其实挺简单的，就是通过一段时间内，比如 1s，统计绘制了多少个画面，就可以计算出 FPS 了。

那如何知道应用 1s 内绘制了多少个界面呢？这个就要靠 `VSync` 信号监听了。我们通过 `Choreographer` 注册 `VSync` 信号监听。
16ms 后，我们收到了 `VSync` 的信号，给 `MessageQueue` 里面放一个同步消息，我们不做特别处理，只是做一个计数，
然后监听下一次的 VSync 信号，这样，我们就可以知道 1s 内我们监听到了多少个 VSync 信号，就可以得出帧率。

为什么监听到的 VSync 信号数量就是帧率呢？由于 `Looper` 处理 `Message` 是串行的，就是一次只处理一个 `Message` ，处理
完了这个 `Message` 才会处理下一个 `Message` 。而绘制的时候，绘制任务 `Message` 是异步消息，会优先执行，绘制任务 `Message`
执行完成后，就会执行上面说的 `VSync` 信号计数的任务，所以最后统计到的 `VSync` 信号数量可以认为是某段时间内绘制的帧数。
然后就可以通过这段时间的长度和 `VSync` 信号数量来计算帧率了。

最终终端 log 打印效果如下：
```
com.xander.performace.demo W/demo_FPSTool: APP FPS is: 54 Hz
com.xander.performace.demo W/demo_FPSTool: APP FPS is: 60 Hz
com.xander.performace.demo W/demo_FPSTool: APP FPS is: 60 Hz
```

## 线程的创建和启动监控以及线程池的创建监控

线程和线程池的监控，主要是监控线程和线程池在哪里创建和执行的，如果我们可以知道这些信息，
我们就可以比较清楚线程和线程池的创建和启动时机是否合理。从而得出优化方案。

一个比较容易想到的方法就是，应用代码里面的所有线程和线程池继承同一个线程基类和线程池基类。
然后在构造函数和启动函数里面打印方法调用栈，这样我们就知道哪里创建和执行了线程或者线程池。

让应用所有的线程和线程池继承同一个基类，可以通过编译插件来实现，定制一个特殊的 `Transform` ，
通过 `ASM` 编辑生成的字节码来改变继承关系。但是，这个方法有一定的上手难度，不太适合新手。

除了这个方法，我们还有另外一种方法，就是 `hook` 。通过 `hook` 线程或者线程池的构造方法和启动方法，
我们就可以在线程或者线程池的构造方法和启动方法的前后做一些切片处理，比如打印当前方法调用栈等。
这个也就是线程和线程池监控的基本原理。

线程池的监控没有太大难度，一般都是 `ThreadPoolExecutor` 的子类，所以我们 `hook` 一下 `ThreadPoolExecutor` 的
构造方法就可以监控线程池的创建了。线程池的执行主要就是 `hook` 住 `ThreadPoolExecutor` 类的 `execute` 方法。

线程的创建和执行的监控方法就稍微要费些脑筋了，因为线程池里面会创建线程，所以这个线程的创建和执行应该和线程池
绑定的。需要找到线程和线程池的联系，之前看到一个库，好像是通过线程和线程池的 `ThreadGroup` 来建立关联的，本来
我也计划按照这个关系来写代码的，但是我发现，我们有的小伙伴写的线程池的 `ThreadFactory` 里面创建线程并没有传入
`ThreadGroup` ，这个就尴尬了，就建立不了联系了。经过查阅相关源码发现了一个关键的类，`ThreadPoolExecutor` 的内部类
`Worker` ，由于这个类是内部类，所以这个类实际的构造方法里面会传入一个外部类的实例，也就是 `ThreadPoolExecutor` 实例。
同时， `Worker` 这个类还是一个 `Runnable` 实现，在 `Worker` 类通过 `ThreadFactory` 创建线程的时候，会把自己作为一个
`Runnable` 传给 `Thread` 所以，我们通过这个关系，就可以知道 `Worker` 和 `Thread` 的关联了。这样，我们通过
`ThreadPoolExecutor` 和 `Worker` 的关联，以及 `Worker` 和 `Thread` 的关联，就可以得到 `ThreadPoolExecutor` 和
它创建的 `Thread` 的关联了。这个也就是线程和线程池的监控原理了。

最终终端 log 打印效果如下：
```
com.xander.performace.demo W/demo_Issue: =================================================
    type: THREAD
    msg: THREAD POOL CREATE
    create time: 2021-01-13 11:23:47
    create trace:
    	com.xander.performance.StackTraceUtils.list(StackTraceUtils.java:39)
    	com.xander.performance.ThreadTool$ThreadPoolExecutorConstructorHook.afterHookedMethod(ThreadTool.java:158)
    	de.robv.android.xposed.DexposedBridge.handleHookedArtMethod(DexposedBridge.java:265)
    	me.weishu.epic.art.entry.Entry64.onHookObject(Entry64.java:64)
    	me.weishu.epic.art.entry.Entry64.referenceBridge(Entry64.java:239)
    	java.util.concurrent.Executors.newSingleThreadExecutor(Executors.java:179)
    	com.xander.performance.demo.MainActivity.testThreadPool(MainActivity.kt:38)
    	java.lang.reflect.Method.invoke(Method.java:-2)
    	androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener.onClick(AppCompatViewInflater.java:397)
    	android.view.View.performClick(View.java:7496)
    	android.view.View.performClickInternal(View.java:7473)
    	android.view.View.access$3600(View.java:831)
    	android.view.View$PerformClick.run(View.java:28641)
    	android.os.Handler.handleCallback(Handler.java:938)
    	android.os.Handler.dispatchMessage(Handler.java:99)
    	android.os.Looper.loop(Looper.java:236)
    	android.app.ActivityThread.main(ActivityThread.java:7876)
    	java.lang.reflect.Method.invoke(Method.java:-2)
    	com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:656)
    	com.android.internal.os.ZygoteInit.main(ZygoteInit.java:967)
```


## IPC(进程间通讯)监控的原理

进程间通讯的具体原理，也就是 `Binder` 机制，这里不做详细的说明，也不是这个框架库的原理。

检测进程间通讯的方法和前面检测线程的方法类似，就是找到所有的进程间通讯的方法的共同点，然后
对共同点做一些修改或者说切片，让应用在进行进程间通讯的时候，打印一下调用栈，然后继续做原来
的事情。就达到了 IPC 监控的目的。

那如何找到共同点，或者说切片，就是本节的重点。

进程间通讯离不开 `Binder` ，需要从 `Binder` 入手。

写一个 `AIDL` demo 后发现，自动生成的代码里面，接口 `A` 继承自 `IInterface` 接口，然后接口里面有个
内部抽象类 `Stub` 类，继承自 `Binder` ，同时实现了接口 `A` 。这个 `Stub` 类里面还有一个内部类 `Proxy` ，
实现了接口 `A` ，并持有一个 `IBinder` 实例。

我们在使用 `AIDL` 的时候，会用到 `Stub` 类的 `asInterFace` 的方法，这个方法会新建一个 `Proxy` 实例，
并给这个 `Proxy` 实例传入 `IBinder` , 或者如果传入的 `IBinder` 实例如果是接口 `A` 的话，就强制转化为接口 A 实例。
一般而言，这个 `IBinder` 实例是 `ServiceConnection` 的回调方法里面的实例，是 `BinderProxy` 的实例。
所以 `Stub` 类的 `asInterFace` 一般会创建一个 `Proxy` 实例，查看这个 `Proxy` 接口的实现方法，
发现最终都会调用 `BinderProxy` 的 `transact` 方法，所以 `BinderProxy` 的 `transact` 方法是一个很好的切入点。

本来我也是计划通过 `hook` 住 `BinderProxy` 类的 `transact` 方法来做 IPC 的检测的。但是 `epic` 库在 `hook` 
含有 `Parcel` 类型参数的方法的时候，不稳定，会有异常。由于暂时还没能力解决这个异常，只能重新找切入点。
最后发现 `AIDL` demo 生成的代码里面，除了调用了 调用 `BinderProxy` 的 `transact` 方法外，
还调用了 `Parcel` 的 `readException` 方法，于是决定 `hook` 这个方法来切入 `IPC` 调用流程，
从而达到 `IPC` 监控的目的。

最终终端 log 打印效果如下：
```
com.xander.performace.demo W/demo_Issue: =================================================
    type: IPC
    msg: IPC
    create time: 2021-01-13 11:25:04
    trace:
    	com.xander.performance.StackTraceUtils.list(StackTraceUtils.java:39)
    	com.xander.performance.IPCTool$ParcelReadExceptionHook.beforeHookedMethod(IPCTool.java:96)
    	de.robv.android.xposed.DexposedBridge.handleHookedArtMethod(DexposedBridge.java:229)
    	me.weishu.epic.art.entry.Entry64.onHookVoid(Entry64.java:68)
    	me.weishu.epic.art.entry.Entry64.referenceBridge(Entry64.java:220)
    	me.weishu.epic.art.entry.Entry64.voidBridge(Entry64.java:82)
    	android.app.IActivityManager$Stub$Proxy.getRunningAppProcesses(IActivityManager.java:7285)
    	android.app.ActivityManager.getRunningAppProcesses(ActivityManager.java:3684)
    	com.xander.performance.demo.MainActivity.testIPC(MainActivity.kt:55)
    	java.lang.reflect.Method.invoke(Method.java:-2)
    	androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener.onClick(AppCompatViewInflater.java:397)
    	android.view.View.performClick(View.java:7496)
    	android.view.View.performClickInternal(View.java:7473)
    	android.view.View.access$3600(View.java:831)
    	android.view.View$PerformClick.run(View.java:28641)
    	android.os.Handler.handleCallback(Handler.java:938)
    	android.os.Handler.dispatchMessage(Handler.java:99)
    	android.os.Looper.loop(Looper.java:236)
    	android.app.ActivityThread.main(ActivityThread.java:7876)
    	java.lang.reflect.Method.invoke(Method.java:-2)
    	com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:656)
    	com.android.internal.os.ZygoteInit.main(ZygoteInit.java:967)
```

参考资料:
1. [epic](https://github.com/tiann/epic)
2. [AndroidPerformanceMonitor](https://github.com/markzhai/AndroidPerformanceMonitor)
3. [面试官：如何监测应用的 FPS ？](https://juejin.cn/post/6890407553457963022)
4. [深入探索Android卡顿优化（下）](https://juejin.cn/post/6844904066259091469)

