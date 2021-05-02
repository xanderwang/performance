package com.xander.performance.hook.sandhook;

import com.xander.performance.hook.core.IHookBridge;
import com.xander.performance.hook.core.MethodHook;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;
import io.github.xanderwang.asu.aLog;
import io.github.xanderwang.asu.aUtil;


/**
 * 提供对外的统一的 hook 方法
 */
public class SandHookBridge implements IHookBridge {
  private static final String TAG = "SandHookBridge";

  @Override
  public int getHookType() {
    return IHookBridge.TYPE_SAND_HOOK;
  }

  @Override
  public void hookMethod(Member hookMethod, MethodHook callback) {
    aLog.d(TAG, "hookMethod: %s", aUtil.memberToString(hookMethod));
    XposedBridge.hookMethod(hookMethod, new XC_MethodSandHook(callback));
  }
}
