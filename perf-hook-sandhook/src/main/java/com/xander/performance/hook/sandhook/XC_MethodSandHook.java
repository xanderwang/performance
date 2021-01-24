package com.xander.performance.hook.sandhook;

import com.xander.performance.hook.core.MethodHook;

import de.robv.android.xposed.XC_MethodHook;

public class XC_MethodSandHook extends XC_MethodHook {
  private static final String TAG = "XC_MethodSandHook";

  /**
   * 最外层的 hook 方法回调
   */
  MethodHook methodHook;

  /**
   * 实现外层的 hook param ，用来桥接 epic 的 MethodHookParam
   */
  MethodSandHookParam methodSandHookParam;

  public XC_MethodSandHook(MethodHook methodHook) {
    super();
    this.methodHook = methodHook;
  }

  @Override
  protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    super.beforeHookedMethod(param);
    if (null == methodSandHookParam) {
      methodSandHookParam = new MethodSandHookParam();
    }
    methodSandHookParam.setMethodHookParam(param);
    methodHook.beforeHookedMethod(methodSandHookParam);
  }

  @Override
  protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    super.afterHookedMethod(param);
    if (null == methodSandHookParam) {
      methodSandHookParam = new MethodSandHookParam();
    }
    methodSandHookParam.setMethodHookParam(param);
    methodHook.afterHookedMethod(methodSandHookParam);
  }
}
