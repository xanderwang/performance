package io.github.xanderwang.hook.epic;

import io.github.xanderwang.hook.core.IHookBridge;
import io.github.xanderwang.hook.core.MethodHook;

import java.lang.reflect.Member;

import de.robv.android.xposed.DexposedBridge;
import io.github.xanderwang.asu.aLog;
import io.github.xanderwang.asu.aUtil;

/**
 * 提供对外的统一的 hook 方法
 */
public class EpicBridge implements IHookBridge {
  private static final String TAG = "EpicBridge";

  @Override
  public int getHookType() {
    return IHookBridge.TYPE_EPIC;
  }

  @Override
  public void hookMethod(Member hookMethod, MethodHook callback) {
    aLog.d(TAG, "hookMethod: %s", aUtil.memberToString(hookMethod));
    DexposedBridge.hookMethod(hookMethod, new XC_MethodHookEpic(callback));
  }
}
