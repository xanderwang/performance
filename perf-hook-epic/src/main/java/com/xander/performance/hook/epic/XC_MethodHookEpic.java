package com.xander.performance.hook.epic;

import com.xander.performance.hook.core.MethodHook;

import de.robv.android.xposed.XC_MethodHook;

public class XC_MethodHookEpic extends XC_MethodHook {
  private static final String TAG = "XC_MethodHookEpic";

  /**
   * 最外层的 hook 方法回调
   */
  MethodHook methodHook;

  /**
   * 实现外层的 hook param ，用来桥接 epic 的 MethodHookParam
   */
  MethodEpicParam paramEpic;

  public XC_MethodHookEpic(MethodHook methodHook) {
    super();
    this.methodHook = methodHook;
  }

  @Override
  protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    super.beforeHookedMethod(param);
    if (null == paramEpic) {
      paramEpic = new MethodEpicParam();
    }
    paramEpic.setMethodHookParam(param);
    methodHook.beforeHookedMethod(paramEpic);
  }

  @Override
  protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    super.afterHookedMethod(param);
    if (null == paramEpic) {
      paramEpic = new MethodEpicParam();
    }
    paramEpic.setMethodHookParam(param);
    methodHook.afterHookedMethod(paramEpic);
  }
}
