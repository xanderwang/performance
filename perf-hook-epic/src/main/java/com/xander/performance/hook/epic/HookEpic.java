package com.xander.performance.hook.epic;

import com.xander.asu.aLog;
import com.xander.performance.hook.core.IHookBridge;
import com.xander.performance.hook.core.MethodHook;

import java.lang.reflect.Member;

import de.robv.android.xposed.DexposedBridge;

/**
 * 提供对外的统一的 hook 方法
 */
public class HookEpic implements IHookBridge {
  private static final String TAG = "HookEpic";

  // @Override
  // public void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
  //   // aLog.d("xxxx", "clazz:%s", clazz);
  //   // aLog.d("xxxx", "methodName:%S", methodName);
  //   Object methodHook = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
  //   if (!(methodHook instanceof MethodHook)) {
  //     throw new IllegalArgumentException("lost MethodHook!!!");
  //   }
  //   XC_MethodHookEpic newHookCallback = new XC_MethodHookEpic((MethodHook) methodHook);
  //   parameterTypesAndCallback[parameterTypesAndCallback.length - 1] = newHookCallback;
  //   // aLog.d("xxxx", "parameterTypesAndCallback:%s", Arrays.toString(parameterTypesAndCallback));
  //   DexposedBridge.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
  // }

  @Override
  public void hookMethod(Member hookMethod, MethodHook callback) {
    aLog.d(TAG, "hookMethod:%s.%s", hookMethod.getDeclaringClass(), hookMethod.getName());
    DexposedBridge.hookMethod(hookMethod, new XC_MethodHookEpic(callback));
  }
}
