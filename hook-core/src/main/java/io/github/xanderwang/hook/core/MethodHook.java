package io.github.xanderwang.hook.core;

/**
 * 方法被 hook 的回调
 */
public abstract class MethodHook {

    /**
     * 被 hook 的方法被调用前的回调，
     *
     * @param param 被 hook 的方法的信息，包括原始方法和方法的实例以及方法参数等
     * @throws Throwable
     */
    public void beforeHookedMethod(MethodParam param) throws Throwable {
    }

    /**
     * 被 hook 的方法被调用后的回调，
     *
     * @param param 被 hook 的方法的信息，包括原始方法和方法的实例以及方法参数等
     * @throws Throwable
     */
    public void afterHookedMethod(MethodParam param) throws Throwable {
    }
}
