package io.github.xanderwang.hook.core;

import java.lang.reflect.Member;

public interface IHookBridge {

  int TYPE_EPIC      = 0;
  int TYPE_SAND_HOOK = 1;
  int TYPE_FAST_HOOK = 2;

  /**
   * 是哪种 hook 方案，方便后续由于调试需求，需要根据不同的 hook 方案做不同的逻辑处理
   *
   * @return
   */
  int getHookType();

  /**
   * hook 指定的方法
   *
   * @param hookMethod 待 hook 的方法
   * @param callback   hook 成功后的回调
   */
  void hookMethod(Member hookMethod, MethodHook callback);
}
