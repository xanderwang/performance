package com.xander.performance.hook.epic;

import com.xander.performance.hook.core.IMethodParam;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class MethodHookParamEpic implements IMethodParam {

  MethodHookParam methodHookParam;

  public MethodHookParamEpic() {
  }

  public void setMethodHookParam(MethodHookParam methodHookParam) {
    this.methodHookParam = methodHookParam;
  }

  @Override
  public Object getResult() {
    return methodHookParam.getResult();
  }

  @Override
  public void setResult(Object result) {
    methodHookParam.setResult(result);
  }

  @Override
  public Throwable getThrowable() {
    return methodHookParam.getThrowable();
  }

  @Override
  public boolean hasThrowable() {
    return methodHookParam.hasThrowable();
  }

  @Override
  public void setThrowable(Throwable throwable) {
    methodHookParam.setThrowable(throwable);
  }

  @Override
  public Object getResultOrThrowable() throws Throwable {
    return methodHookParam.getResultOrThrowable();
  }

  @Override
  public Object[] getArgs() {
    return methodHookParam.args;
  }

  @Override
  public Object getThisObject() {
    return methodHookParam.thisObject;
  }

  @Override
  public Member getMethod() {
    return methodHookParam.method;
  }
}
