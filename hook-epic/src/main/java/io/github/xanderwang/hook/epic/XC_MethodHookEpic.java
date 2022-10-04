package io.github.xanderwang.hook.epic;

import io.github.xanderwang.hook.core.MethodHook;

import de.robv.android.xposed.XC_MethodHook;

public class XC_MethodHookEpic extends XC_MethodHook {

    /* log tag */
    private static final String TAG = "XC_MethodHookEpic";

    /**
     * 最外层的 hook 方法回调
     */
    MethodHook methodHookCallback;

    /**
     * 实现外层的 hook param ，用来桥接 epic 的 MethodHookParam
     */
    MethodEpicParam paramEpic;

    public XC_MethodHookEpic(MethodHook callback) {
        super();
        this.methodHookCallback = callback;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        super.beforeHookedMethod(param);
        if (null == paramEpic) {
            paramEpic = new MethodEpicParam();
        }
        paramEpic.setMethodHookParam(param);
        methodHookCallback.beforeHookedMethod(paramEpic);
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);
        if (null == paramEpic) {
            paramEpic = new MethodEpicParam();
        }
        paramEpic.setMethodHookParam(param);
        methodHookCallback.afterHookedMethod(paramEpic);
    }
}
