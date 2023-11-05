package io.github.xanderwang.hook.epic

import de.robv.android.xposed.DexposedBridge
import io.github.xanderwang.hook.core.IHookBridge
import io.github.xanderwang.hook.core.MethodHook
import java.lang.reflect.Member

/**
 * epic 的 hook 桥接实现
 */
class EpicBridge(override val hookType: Int = IHookBridge.TYPE_EPIC) : IHookBridge {
    /**
     * hook 指定的方法
     *
     * @param hookMethod 待 hook 的方法
     * @param callback   hook 成功后的回调
     */
    override fun hookMethod(hookMethod: Member?, callback: MethodHook?) {
        // aLog.d(TAG, "hookMethod: %s", hookMethod.string());
        DexposedBridge.hookMethod(hookMethod, XC_MethodHookEpic(callback))
    }

    companion object {
        /**
         *
         */
        private const val TAG = "EpicBridge"
    }
}