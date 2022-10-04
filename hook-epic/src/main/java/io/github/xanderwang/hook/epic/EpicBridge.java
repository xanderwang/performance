package io.github.xanderwang.hook.epic;

import io.github.xanderwang.hook.core.IHookBridge;
import io.github.xanderwang.hook.core.MethodHook;

import java.lang.reflect.Member;

import de.robv.android.xposed.DexposedBridge;
import io.github.xanderwang.asu.aLog;
import io.github.xanderwang.asu.aUtil;

/**
 * epic 的 hook 桥接实现
 */
public class EpicBridge implements IHookBridge {

    /**
     *
     */
    private static final String TAG = "EpicBridge";

    /**
     * 是哪种 hook 方案，方便后续由于调试需求，需要根据不同的 hook 方案做不同的逻辑处理
     *
     * @return epic 库
     */
    @Override
    public int getHookType() {
        return IHookBridge.TYPE_EPIC;
    }

    /**
     * hook 指定的方法
     *
     * @param hookMethod 待 hook 的方法
     * @param callback   hook 成功后的回调
     */
    @Override
    public void hookMethod(Member hookMethod, MethodHook callback) {
        aLog.d(TAG, "hookMethod: %s", aUtil.memberToString(hookMethod));
        DexposedBridge.hookMethod(hookMethod, new XC_MethodHookEpic(callback));
    }
}
