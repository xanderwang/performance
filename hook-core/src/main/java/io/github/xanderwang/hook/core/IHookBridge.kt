package io.github.xanderwang.hook.core

import java.lang.reflect.Member

/**
 * hook 桥接接口，用于封装其他 hook 库，统一接口
 */
interface IHookBridge {
    /**
     * 是哪种 hook 方案，方便后续由于调试需求，需要根据不同的 hook 方案做不同的逻辑处理
     *
     * @return
     */
    val hookType: Int

    /**
     * hook 指定的方法
     *
     * @param hookMethod 待 hook 的方法
     * @param callback   hook 成功后的回调
     */
    fun hookMethod(hookMethod: Member?, callback: MethodHook?)

    companion object {
        /**
         * epic 库
         */
        const val TYPE_EPIC = 0

        /**
         * SandHook 库
         */
        const val TYPE_SAND_HOOK = 1

        /**
         * FastHook 库
         */
        const val TYPE_FAST_HOOK = 2
    }
}