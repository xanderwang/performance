package com.xander.performance;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Xander Wang Created on 2020/11/4.
 * @Description 通过 hook Handler 的 sendMessageAtTime 方法
 * 和 dispatchMessage 方法来达到监测的目前。
 *
 * sendMessageAtTime 的时候，可以保存当前的
 */
@Deprecated
public class HandlerTool {

  private static String TAG = pTool.TAG + "_HandlerTool";

  static void resetTag(String tag) {
    TAG = tag + "_HandlerTool";
  }

  static void start() {
    xLog.e(TAG, "start");
    //hookWithEpic();
  }

  private static void hookWithEpic() {
    try {
      DexposedBridge.findAndHookMethod(
          Message.class,
          "obtain",
          new MessageObtainHook()
      );
      DexposedBridge.findAndHookMethod(
          Handler.class,
          "dispatchMessage",
          Message.class,
          new HandlerDispatchMessageHook()
      );
      DexposedBridge.findAndHookMethod(
          Handler.class,
          "sendMessageAtTime",
          Message.class,
          long.class,
          new HandlerSendMessageHook()
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static HashMap<String, MethodStackInfo> methodTraceMap = new HashMap<>();

  static class MethodStackInfo {

    long startTime = 0;

    long costTime = 0;

    String sendMsgStackTrace;

    String disPatchMsgStackTrace;

    String toJson() {
      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("costTime", costTime);
        jsonObject.put("sendMsgStackTrace", sendMsgStackTrace);
        jsonObject.put("disPatchMsgStackTrace", disPatchMsgStackTrace);
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
      //super.afterHookedMethod(param);
      Object result = param.getResult();
      boolean resultIsBoolean = result instanceof Boolean;
      if (!resultIsBoolean) {
        return;
      }
      Boolean bResult = (Boolean) result;
      if (bResult) {
        String msgKey = Integer.toHexString(param.args[0].hashCode());
        MethodStackInfo methodStackInfo = new MethodStackInfo();
        // 保存调用栈
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        methodStackInfo.sendMsgStackTrace = StackTraceUtils.string(
            stackTrace,
            true,
            this.getClass().getName()
        );
        methodTraceMap.put(msgKey, methodStackInfo);
        Method isInUse = Message.class.getDeclaredMethod("isInUse");
        isInUse.setAccessible(true);
        if ((Boolean) isInUse.invoke(param.args[0])) {
          xLog.e(TAG,"HandlerSendMessageHook msg is in use!!! msg key:" + msgKey);
          xLog.e(TAG,"HandlerSendMessageHook msg is in use!!! msg send trace:" + methodStackInfo.sendMsgStackTrace);
          Field flags = Message.class.getDeclaredField("flags");
          flags.setAccessible(true);
          flags.setInt(param.args[0], 0);
          if ((Boolean) isInUse.invoke(param.args[0])) {
            StackTraceUtils.print(
                TAG,
                stackTrace,
                "msg is in use when sendMessageAtTime",
                true,
                this.getClass().getName()
            );
          }
        }
      }
    }
  }

  static class MessageObtainHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      //super.afterHookedMethod(param);
      Message msg = (Message) param.getResult();
      String msgKey = Integer.toHexString(msg.hashCode());
      Field flag = Message.class.getDeclaredField("flags");
      flag.setAccessible(true);
      String traceStr = StackTraceUtils.string(
          Thread.currentThread().getStackTrace(),
          true,
          this.getClass().getName()
      );
      xLog.e(TAG, "after obtain msg, obtain trace" + traceStr);
      if (flag.getInt(msg) != 0) {
        xLog.e(TAG, "!!!!error for msg flags:" + flag.getInt(msg));
        flag.setInt(msg, 0);
        StackTraceUtils.print(
            TAG,
            Thread.currentThread().getStackTrace(),
            "obtain msg",
            true,
            this.getClass().getName()
        );
      }
    }
  }

  static class HandlerDispatchMessageHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      //super.beforeHookedMethod(param);
      Object msg = param.args[0];
      String msgKey = Integer.toHexString(param.args[0].hashCode());
      if (msg instanceof Message) {
        MethodStackInfo methodStackInfo = methodTraceMap.get(msgKey);
        if (null != methodStackInfo) {
        } else {
          methodStackInfo = new MethodStackInfo();
          methodStackInfo.disPatchMsgStackTrace = StackTraceUtils.string(
              Thread.currentThread().getStackTrace(),
              true,
              this.getClass().getName()
          );
          methodTraceMap.put(msgKey, methodStackInfo);
        }
        // 保存执行时间
        methodStackInfo.startTime = SystemClock.elapsedRealtime();
      }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      //super.afterHookedMethod(param);
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
        xLog.e(TAG, "````````````````````````````````");
        xLog.e(TAG, methodStackInfo.toJson());
        xLog.e(TAG, "--------------------------------");
      }
      // 移除信息
      methodTraceMap.remove(msgKey);
    }
  }
}
