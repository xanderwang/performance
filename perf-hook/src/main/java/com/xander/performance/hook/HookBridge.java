package com.xander.performance.hook;

import com.xander.asu.aLog;
import com.xander.performance.hook.core.IHookBridge;
import com.xander.performance.hook.core.MethodHook;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 提供对外的统一的 hook 方法
 */
public class HookBridge {

  private static final String TAG = "HookBridge";

  static IHookBridge iHookBridge = null;

  static {
    ServiceLoader<IHookBridge> iHookBridgeLoader = ServiceLoader.load(IHookBridge.class);
    Iterator<IHookBridge> iterator = iHookBridgeLoader.iterator();
    while (iterator.hasNext()) {
      iHookBridge = iterator.next();
    }
  }

  public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
    Class<?>[] parameterTypes = null;

    if (null != parameterTypesAndCallback && parameterTypesAndCallback.length > 1) {
      int len = parameterTypesAndCallback.length - 1;
      parameterTypes = new Class<?>[len];
      for (int i = 0; i < len; i++) {
        parameterTypes[i] = (Class<?>) parameterTypesAndCallback[i];
      }
    }

    Object callback = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
    assertNotNullOrNotEmpty(callback);
    MethodHook methodCallback = (MethodHook) callback;

    List<Method> methodList = findMethodList(clazz, methodName, parameterTypes);
    assertNotNullOrNotEmpty(methodList);
    for (Method method : methodList) {
      hookMethod(method, methodCallback);
    }

  }

  public static void hookMethod(Member hookMethod, MethodHook callback) {
    assertNotNullOrNotEmpty(iHookBridge);
    iHookBridge.hookMethod(hookMethod, callback);
  }

  private static List<Method> findMethodList(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
    List<Method> list = new ArrayList<>();
    boolean justCheckName = false;
    if (null == parameterTypes || parameterTypes.length == 0) {
      justCheckName = true;
    }
    if (justCheckName) {
      Method[] methods = clazz.getDeclaredMethods();
      if (null != methods) {
        for (Method method : methods) {
          if (method.getName().equals(methodName)) {
            list.add(method);
            aLog.d(TAG, "find a method: %s", method.toString());
          }
        }
      }
    } else {
      try {
        Method m = clazz.getDeclaredMethod(methodName, parameterTypes);
        list.add(m);
        aLog.d(TAG, "find a method: %s", m.toString());
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
    return list;
  }


  private static void assertNotNullOrNotEmpty(Object object) {
    if (null == object) {
      throw new IllegalArgumentException("null object!!!");
    }
    if (object instanceof List) {
      if (((List) object).isEmpty()) {
        throw new IllegalArgumentException("empty list!!!");
      }
    }
  }
}
