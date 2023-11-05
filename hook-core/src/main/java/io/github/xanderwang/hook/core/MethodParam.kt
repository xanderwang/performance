package io.github.xanderwang.hook.core

import java.lang.reflect.Member

/**
 * hook 方法的一些参数，比如可以获取/设置被 hook 方法的结果等
 */
interface MethodParam {
    /**
     * 方法调用结果
     */
    var result: Any?

    /**
     * 调用的异常
     */
    var throwable: Throwable?

    /**
     * 方法 hook 后，被调用后，是否有异常
     *
     * @return true 表示方法 hook 后，被调用后，有异常
     */
    fun hasThrowable(): Boolean

    /**
     * 获取调用后的结果或者异常
     * @throws Throwable
     */
    @get:Throws(Throwable::class)
    val resultOrThrowable: Any?

    /**
     * 获取方法入参实例
     */
    var args: Array<Any?>?

    /**
     * 获取运行时的实例
     */
    var thisObject: Any?

    /**
     * 获取被 hook 的原始方法
     */
    var method: Member?
}