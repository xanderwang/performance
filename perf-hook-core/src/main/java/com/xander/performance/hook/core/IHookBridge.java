package com.xander.performance.hook.core;

import java.lang.reflect.Member;

public interface IHookBridge {

  // void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback);

  void hookMethod(Member hookMethod, MethodHook callback);
}
