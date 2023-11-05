package io.github.xanderwang.hook.sandhook

import de.robv.android.xposed.XposedBridge
import io.github.xanderwang.asu.ALog.d
import io.github.xanderwang.asu.string
import io.github.xanderwang.hook.core.IHookBridge
import io.github.xanderwang.hook.core.MethodHook
import java.lang.reflect.Member

/**
 * 提供对外的统一的 hook 方法
 */
class SandHookBridge(override val hookType: Int = IHookBridge.TYPE_SAND_HOOK) : IHookBridge {

    override fun hookMethod(hookMethod: Member?, callback: MethodHook?) {
        d(TAG, "hookMethod: ${hookMethod.string()}")
        XposedBridge.hookMethod(hookMethod, XC_MethodSandHook(callback))
    }

    companion object {
        private const val TAG = "SandHookBridge"
    }
}