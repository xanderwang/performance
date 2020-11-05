package com.xander.performance;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 通过 hook 方法来
 */
public class HandlerTool {

  private static final String TAG = pTool.TAG + "_HandlerTool";

  static void start() {
    hookWithEpic();
  }

  private static void hookWithEpic() {
    try {
      DexposedBridge.findAndHookMethod(
          Handler.class,
          "sendMessageAtTime",
          Message.class,
          long.class,
          new HandlerSendMessageHook()
      );
      DexposedBridge.findAndHookMethod(
          Handler.class,
          "dispatchMessage",
          Message.class,
          new HandlerDispatchMessageHook()
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static HashMap<String, MethodStackInfo> methodTraceMap = new HashMap<>();

  static class MethodStackInfo {

    long startTime = 0;

    long costTime = 0;
    String stackTrace;

    String toJson() {
      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("cost_time", costTime);
        jsonObject.put("stack_trace", stackTrace);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return jsonObject.toString();
    }

  }

  // todo 有个问题就是, hook 后，好像 msg 会有些异常 囧
  static class HandlerSendMessageHook extends XC_MethodHook {

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      Object result = param.getResult();
      boolean resultIsBoolean = result instanceof Boolean;
      if (!resultIsBoolean) {
        return;
      }
      Boolean bResult = (Boolean) result;
      if (bResult) {
        MethodStackInfo methodStackInfo = new MethodStackInfo();
        // 保存调用栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        methodStackInfo.stackTrace = StackTraceUtils.string(
            stackTrace,
            true,
            this.getClass().getName()
        );
        methodTraceMap.put(Integer.toHexString(param.args[0].hashCode()), methodStackInfo);
      }
    }
  }

  static class HandlerDispatchMessageHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Object msg = param.args[0];
      String msgKey = Integer.toHexString(param.args[0].hashCode());
      if (msg instanceof Message) {
        MethodStackInfo methodStackInfo = methodTraceMap.get(msgKey);
        if (null != methodStackInfo) {
          // 保存执行时间
          methodStackInfo.startTime = SystemClock.elapsedRealtime();
        }
      }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      MethodStackInfo methodStackInfo = null;
      long startTime = 0;
      long endTime = SystemClock.elapsedRealtime();

      Object msg = param.args[0];
      String msgKey = Integer.toHexString(param.args[0].hashCode());

      if (msg instanceof Message) {
        methodStackInfo = methodTraceMap.get(msgKey);
        if (null != methodStackInfo) {
          startTime = methodStackInfo.startTime;
          methodStackInfo.costTime = endTime - startTime;
        }
      }
      if (Looper.myLooper() == Looper.getMainLooper() && startTime > 0
          && (methodStackInfo.costTime > PerformanceConfig.HANDLER_CHECK_TIME)) {
        // 需要打印
        xLog.e(TAG, methodStackInfo.toJson());
      }
      // 移除信息
      methodTraceMap.remove(msgKey);
    }
  }
}
