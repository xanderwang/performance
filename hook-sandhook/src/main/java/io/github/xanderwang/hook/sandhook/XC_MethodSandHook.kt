package io.github.xanderwang.hook.sandhook

import de.robv.android.xposed.XC_MethodHook
import io.github.xanderwang.hook.core.MethodHook

/**
 * @param hookCallback 最外层的 hook 方法回调
 */
class XC_MethodSandHook(var hookCallback: MethodHook?) : XC_MethodHook() {
    /**
     * 实现外层的 hook param ，用来桥接 epic 的 MethodHookParam
     */
    private val methodSandHookParam by lazy { MethodSandHookParam() }

    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        super.beforeHookedMethod(param)
        methodSandHookParam.methodHookParam = param
        hookCallback?.beforeHookedMethod(methodSandHookParam)
    }

    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        methodSandHookParam.methodHookParam = param
        hookCallback?.afterHookedMethod(methodSandHookParam)
        super.afterHookedMethod(param)
    }

    companion object {
        private const val TAG = "XC_MethodSandHook"
    }
}