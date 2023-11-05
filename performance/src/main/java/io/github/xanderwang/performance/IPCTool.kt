package io.github.xanderwang.performance

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.SystemClock
import io.github.xanderwang.asu.ALog.e
import io.github.xanderwang.hook.HookBridge
import io.github.xanderwang.hook.core.MethodHook
import io.github.xanderwang.hook.core.MethodParam
import java.lang.reflect.Method

/**
 * @author xanderwang
 * Created on 2020/11/4.
 *
 * 利用 hook 方法， hook android.os.BinderProxy 类的 transact 方法，
 * 从而获取 ipc 调用链 这样可以知道系统的瓶颈在哪里
 *
 */
internal object IPCTool {

    private const val TAG = "IPCTool"

    @Deprecated("")
    private var binderGetInterfaceDescriptor: Method? = null

    private val issueHashMap = HashMap<String, IPCIssue>(64)
    private fun startTransact(ipcInterface: Any?, methodToken: Any?) {
        if (null == ipcInterface) {
            val ipcIssue = Issue(Issue.TYPE_IPC, "IPC", Utils.list())
            ipcIssue.print()
        } else {
            val ipcToken = String.format("%s_%s", ipcInterface, methodToken)
            val ipcIssue = IPCIssue(ipcInterface, "IPC", null)
            ipcIssue.startTime = SystemClock.elapsedRealtime()
            issueHashMap[ipcToken] = ipcIssue
        }
    }

    private fun endTransact(ipcInterface: Any?, methodToken: Any?, startTrace: Array<StackTraceElement>) {
        ipcInterface ?: return
        val ipcToken = String.format("%s_%s", ipcInterface, methodToken)
        val ipcIssue = issueHashMap.remove(ipcToken)
        ipcIssue?.let {
            ipcIssue.costTime = SystemClock.elapsedRealtime() - ipcIssue.startTime
            if (ipcIssue.costTime >= Config.IPC_BLOCK_TIME) {
                ipcIssue.data = Utils.list(startTrace)
                ipcIssue.print()
            }
        }
    }

    private fun justCheckTransact(startTrace: Array<StackTraceElement>) {
        val ipcIssue = Issue(Issue.TYPE_IPC, "IPC", Utils.list(startTrace))
        ipcIssue.print()
    }

    @JvmStatic
    fun start() {
        e(TAG, "start")
        hookIPC()
    }

    @SuppressLint("PrivateApi")
    private fun hookIPC() {
        try {
            if (HookBridge.isSandHook) {
                // 这个方法  epic hook 的话会报错，很奇怪，理论上是一个比较好的 hook 点
                val binderProxyClass = Class.forName("android.os.BinderProxy")
                // binderGetInterfaceDescriptor = binderProxyClass.getDeclaredMethod("getInterfaceDescriptor")
                // binderGetInterfaceDescriptor?.isAccessible = true
                HookBridge.findAndHookMethod(
                    binderProxyClass, "transact", Int::class.javaPrimitiveType,
                    Parcel::class.java, Parcel::class.java, Int::class.javaPrimitiveType, BinderTransactProxyHook()
                )
            } else {
                // 除了每次调用 android.os.BinderProxy.transact 方法，
                // 观察 AIDL 生成的代码，发现每次 IPC 调用也会调用 Parcel.readException 方法
                // 故初步用这个方法作为切入点来监控系统的 IPC 调用情况
                HookBridge.findAndHookMethod(Parcel::class.java, "readException", ParcelReadExceptionHook())
            }
            e(TAG, "hookIPC")
        } catch (e: Exception) {
            e(TAG, "hookIPC", e)
        }
    }

    internal class BinderTransactProxyHook : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            // super.beforeHookedMethod(param);
            var ipcInterface: Any? = null
            binderGetInterfaceDescriptor?.let {
                ipcInterface = it.invoke(param?.thisObject)
            }
            startTransact(ipcInterface, param?.args?.get(0))
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            // super.afterHookedMethod(param);
            var ipcInterface: Any? = null
            binderGetInterfaceDescriptor?.let {
                ipcInterface = it.invoke(param?.thisObject)
            }
            endTransact(ipcInterface, param?.args?.get(0), Thread.currentThread().stackTrace)
        }
    }

    internal class ParcelReadExceptionHook : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            // super.beforeHookedMethod(param);
            justCheckTransact(Thread.currentThread().stackTrace)
        }
    }

    internal class IPCIssue(private var ipcInterface: Any?, msg: String, data: Any?) : Issue(TYPE_IPC, msg, data) {

        var startTime = 0L

        var costTime = 0L
        override fun formatExtraInfo(sb: StringBuilder?) {
            ipcInterface?.let {
                sb?.append("ipc interface: ")?.append(ipcInterface)?.append('\n')
            }
            if (costTime > 0) {
                sb?.append("cost time: ")?.append(costTime)?.append(" ms\n")
            }
        }
    }
}