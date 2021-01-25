package com.xander.performance.hook.epic;

import com.xander.asu.aLog;
import com.xander.asu.aUtil;
import com.xander.performance.hook.core.IHookBridge;
import com.xander.performance.hook.core.MethodHook;

import java.lang.reflect.Member;

import de.robv.android.xposed.DexposedBridge;

/**
 * 提供对外的统一的 hook 方法
 */
public class HookEpic implements IHookBridge {
  private static final String TAG = "HookEpic";

  @Override
  public int getHookType() {
    return IHookBridge.TYPE_EPIC;
  }

  @Override
  public void hookMethod(Member hookMethod, MethodHook callback) {
    aLog.d(TAG, "hookMethod:%s", aUtil.memberToString(hookMethod));
    DexposedBridge.hookMethod(hookMethod, new XC_MethodHookEpic(callback));
  }
}
