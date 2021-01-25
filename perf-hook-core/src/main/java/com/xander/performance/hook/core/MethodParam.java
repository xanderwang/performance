package com.xander.performance.hook.core;

import java.lang.reflect.Member;

/**
 * hook 后，被调用的
 */
public interface MethodParam {

  Object getResult();

  void setResult(Object result);

  Throwable getThrowable();

  boolean hasThrowable();

  void setThrowable(Throwable throwable);

  Object getResultOrThrowable() throws Throwable;

  Object[] getArgs();

  Object getThisObject();

  Member getMethod();
}
