package io.github.xanderwang.hook.core;

import java.lang.reflect.Member;

/**
 * hook 后，被调用的
 */
public interface MethodParam {

    /**
     * 方法调用结果
     *
     * @return 方法调用的结果
     */
    Object getResult();

    /**
     * 设置方法调用结果
     *
     * @param result 自定义的方法结果
     */
    void setResult(Object result);

    /**
     * 获取异常
     *
     * @return 方法 hook 后，被调用时的异常
     */
    Throwable getThrowable();

    /**
     * 方法 hook 后，被调用后，是否有异常
     *
     * @return true 表示方法 hook 后，被调用后，有异常
     */
    boolean hasThrowable();

    /**
     * 设置异常
     *
     * @param throwable
     */
    void setThrowable(Throwable throwable);

    /**
     * 获取调用后的结果或者异常
     *
     * @return
     * @throws Throwable
     */
    Object getResultOrThrowable() throws Throwable;

    /**
     * 获取方法入参实例
     *
     * @return 方法入参的实例
     */
    Object[] getArgs();

    /**
     * 获取运行时的实例
     *
     * @return
     */
    Object getThisObject();

    /**
     * 获取被 hook 的原始方法
     *
     * @return
     */
    Member getMethod();
}
