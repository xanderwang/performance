package io.github.xanderwang.hook.epic

import de.robv.android.xposed.XC_MethodHook
import io.github.xanderwang.hook.core.MethodHook

/**
 * @param methodHookCallback 最外层的 hook 方法回调
 */
class XC_MethodHookEpic(var methodHookCallback: MethodHook?) : XC_MethodHook() {
    /**
     * 实现外层的 hook param ，用来桥接 epic 的 MethodHookParam
     */
    private val paramEpic by lazy { MethodEpicParam() }

    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        super.beforeHookedMethod(param)
        paramEpic.methodHookParam = param
        methodHookCallback?.beforeHookedMethod(paramEpic)
    }

    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        paramEpic.methodHookParam = param
        methodHookCallback?.afterHookedMethod(paramEpic)
        super.afterHookedMethod(param)
    }

    companion object {
        /* log tag */
        private const val TAG = "XC_MethodHookEpic"
    }
}