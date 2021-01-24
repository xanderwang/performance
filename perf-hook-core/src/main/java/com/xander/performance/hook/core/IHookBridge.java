package com.xander.performance.hook.core;

import java.lang.reflect.Member;

public interface IHookBridge {

  void hookMethod(Member hookMethod, MethodHook callback);
}
