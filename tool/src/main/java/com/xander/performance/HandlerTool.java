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

  static HashMap<Message, MethodStackInfo> methodTraceMap = new HashMap<>();

  static class MethodStackInfo {

    long startTime = 0;

    long costTime = 0;
    String stackTrace;

    String toJson() {
      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("cost_time",costTime);
        jsonObject.put("stack_trace",stackTrace);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return jsonObject.toString();
    }

  }

  static class HandlerSendMessageHook extends XC_MethodHook {

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      //xLog.e(TAG,"after send msg:" + param.args[0]);
      super.afterHookedMethod(param);
      Object result = param.getResult();
      boolean resultIsBoolean = result != null && (result instanceof Boolean || result.getClass() == boolean.class);
      if (!resultIsBoolean) {
        return;
      }
      //pTool.printThreadStackTrace(TAG,Thread.currentThread(),"handler",true,HandlerSendMessageHook.class.getName());
      Boolean bResult = (Boolean) result;
      if (bResult) {
        MethodStackInfo methodStackInfo = new MethodStackInfo();
        // 保存调用栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        boolean findClass = false;
        boolean needContinue = false;
        for (int i = 0; i < stackTrace.length; i++) {
          String className = stackTrace[i].getClassName();
          if (!findClass && !className.equals(this.getClass().getName())) {
            continue;
          }
          if (!findClass) {
            findClass = true;
            continue;
          }
          needContinue = false;
          for (int j = 0; j < pTool.LIB_PACKAGE_NAMES.length; j++) {
            if (className.contains(pTool.LIB_PACKAGE_NAMES[j])) {
              needContinue = true;
              break;
            }
          }
          if (needContinue) {
            continue;
          }
          stringBuilder
              .append(stackTrace[i].getClassName())
              .append('.')
              .append(stackTrace[i].getMethodName())
              .append("():")
              .append(stackTrace[i].getLineNumber());
          if (i < stackTrace.length - 1) {
            stringBuilder.append(" <- ");
          }
        }
        methodStackInfo.stackTrace = stringBuilder.toString();
        methodTraceMap.put((Message) param.args[0],methodStackInfo);
      }
    }
  }

  static class HandlerDispatchMessageHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      //xLog.d(TAG,"before dispatch msg:" + param.args[0]);
      super.beforeHookedMethod(param);
      Object msg = param.args[0];
      if (msg instanceof Message) {
        MethodStackInfo methodStackInfo = methodTraceMap.get((Message) msg);
        if (null != methodStackInfo) {
          // 保存执行时间
          methodStackInfo.startTime = SystemClock.elapsedRealtime();
        }
      }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      //xLog.d(TAG,"after dispatch msg:" + param.args[0]);
      super.afterHookedMethod(param);
      MethodStackInfo methodStackInfo = null;
      long startTime = 0;
      long endTime = SystemClock.elapsedRealtime();

      Object msg = param.args[0];

      if (msg instanceof Message) {
        methodStackInfo = methodTraceMap.get((Message) msg);
        if (null != methodStackInfo) {
          startTime = methodStackInfo.startTime;
          methodStackInfo.costTime = endTime - startTime;
        }
      }
      //xLog.d(TAG, methodStackInfo.toJson());
      if (Looper.myLooper() == Looper.getMainLooper() && startTime > 0
          && (methodStackInfo.costTime > PerformanceConfig.HANDLER_CHECK_TIME)) {
        // 需要打印
        xLog.e(TAG, methodStackInfo.toJson());
      }
      if( startTime > 0 ) {
        // 移除信息
        methodTraceMap.remove(msg);
      }
    }
  }
}
