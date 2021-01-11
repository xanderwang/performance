package com.xander.performance;

import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

/**
 * @author Xander Wang
 * Created on 2021/1/4.
 * @Description //TODO
 */
@Deprecated
class HandlerTool {
  private static String TAG = PERF.TAG + "_HandlerTool";

  protected static HashMap<Integer, HandlerIssue> issueHashMap = new HashMap<>(32);

  static void resetTag(String tag) {
    TAG = tag + "_HandlerTool";
  }

  static void start() {
    xLog.e(TAG, "start");
    // hookWithEpic();
    hookDebugMethod();
  }

  private static void hookWithEpic() {
    try {
      DexposedBridge.findAndHookMethod(
          Handler.class,
          "sendMessageAtTime",
          Message.class,
          long.class,
          new SendMsgAtTimeHook()
      );
      DexposedBridge.findAndHookMethod(
          Handler.class,
          "dispatchMessage",
          Message.class,
          new DispatchMsgHook()
      );
    } catch (Exception e) {
      xLog.e(TAG, "HandlerTool", e);
    }
  }

  private static void hookDebugMethod() {
    try {
      // DexposedBridge.findAndHookMethod(
      //     Message.class,
      //     "markInUse",
      //     new MsgMarkInUse()
      // );
      // DexposedBridge.findAndHookMethod(
      //     Message.class,
      //     "recycleUnchecked",
      //     new MsgRecycleUnchecked()
      // );
      // DexposedBridge.findAndHookMethod(
      //     Message.class,
      //     "isInUse",
      //     new MsgIsInUse()
      // );
      // DexposedBridge.findAndHookMethod(
      //     Message.class,
      //     "obtain",
      //     new MsgObtain()
      // );
      DexposedBridge.findAndHookMethod(
          MessageQueue.class,
          "enqueueMessage",
          Message.class,
          long.class,
          new EnqueueMessageHook()
      );
    } catch (Exception e) {
      xLog.e(TAG, "HandlerTool", e);
    }
  }

  private static class SendMsgAtTimeHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      // Issue issue = new Issue(Issue.TYPE_HANDLER, "", StackTraceUtils.list());
      // issue.print();
      xLog.e(TAG, "SendMsgAtTimeHook beforeHookedMethod");
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      // HandlerIssue issue = new HandlerIssue("HANDLER", StackTraceUtils.list());
      // Integer msgKey = param.args[0].hashCode();
      // issueHashMap.put(msgKey, issue);
      xLog.e(TAG, "SendMsgAtTimeHook afterHookedMethod");
    }
  }

  private static class DispatchMsgHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      // Integer msgKey = param.args[0].hashCode();
      // if (issueHashMap.containsKey(msgKey)) {
      //   HandlerIssue issue = issueHashMap.get(msgKey);
      //   issue.startTime = SystemClock.elapsedRealtime();
      // } else {
      //   // xLog.e(TAG, "DispatchMsgHook", new Throwable());
      // }
      xLog.e(TAG, "DispatchMsgHook beforeHookedMethod");
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      // Integer msgKey = param.args[0].hashCode();
      // if (issueHashMap.containsKey(msgKey)) {
      //   HandlerIssue issue = issueHashMap.remove(msgKey);
      //   issue.costTime = SystemClock.elapsedRealtime() - issue.startTime;
      //   if (issue.costTime >= MAX_HANDLER_DISPATCH_MSG_TIME) {
      //     issue.print();
      //   }
      // } else {
      //   // xLog.e(TAG, "DispatchMsgHook", new Throwable());
      // }
      xLog.e(TAG, "DispatchMsgHook afterHookedMethod");
    }
  }

  private static class MsgMarkInUse extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      boolean result = isInUse(param.thisObject);
      xLog.e(TAG, "before MsgMarkInUse: " + param.thisObject + ",result:" + result);
      if (result) {
        xLog.e(TAG, "MsgMarkInUse", new Throwable());
      }
      // StackTraceUtils.print(TAG);
    }
  }

  private static class MsgRecycleUnchecked extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      boolean result = isInUse(param.thisObject);
      xLog.e(TAG, "before MsgRecycleUnchecked:" + param.thisObject + ",result:" + result);
      if (result) {
        xLog.e(TAG, "MsgRecycleUnchecked", new Throwable());
      }
      // StackTraceUtils.print(TAG);
    }
  }

  private static class MsgIsInUse extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      boolean result = isInUse(param.thisObject);
      xLog.e(TAG, "MsgIsInUse: " + param.thisObject + ",result:" + result);
      if (result) {
        xLog.e(TAG, "MsgIsInUse", new Throwable());
      }
      // StackTraceUtils.print(TAG);
    }
  }

  private static class MsgObtain extends XC_MethodHook {

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
      super.afterHookedMethod(param);
      boolean result = isInUse(param.getResultOrThrowable());
      xLog.e(TAG, "after MsgObtain:" + param.getResultOrThrowable() + ",result:" + result);
      if (result) {
        xLog.e(TAG, "MsgObtain", new Throwable());
      }
      // StackTraceUtils.print(TAG);
    }
  }

  private static class EnqueueMessageHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
      super.beforeHookedMethod(param);
      boolean result = isInUse(param.args[0]);
      xLog.e(TAG, "before MessageQueueHook: " + param.args[0] + ",result:" + result);
      if (result) {
        xLog.e(TAG, "MessageQueueHook", new Throwable());
        // clearFlag(param.args[0]);
        // param.args[0] = Message.obtain((Message) param.args[0]);
      }
    }
  }

  static volatile Method method = null;
  static volatile boolean initMethod = false;

  private static Boolean isInUse(Object o) {
    xLog.e(TAG, "isInUse initMethod:" + initMethod + ",method:" + method + ",o:" + o);
    if (initMethod && null == method) {
      return false;
    }
    if (!(o instanceof Message)) {
      return false;
    }
    try {
      if (null == method) {
        initMethod = true;
        method = Message.class.getDeclaredMethod("isInUse");
        method.setAccessible(true);
      }
      return (Boolean) method.invoke(o);
    } catch (Exception e) {
      // e.printStackTrace();
      xLog.e(TAG, "isInUse", e);
    }
    return false;
  }

  private static Boolean clearFlag(Object o) {
    xLog.e(TAG, "clearFlag o:" + o);
    if (!(o instanceof Message)) {
      return false;
    }
    try {
      Field flags = Message.class.getDeclaredField("flags");
      flags.setAccessible(true);
      flags.setInt(o, 0);
    } catch (Exception e) {
      // e.printStackTrace();
      xLog.e(TAG, "clearFlag", e);
    }
    return false;
  }

  private static class HandlerIssue extends Issue {

    protected long costTime;

    protected long startTime = 0;

    public HandlerIssue(String msg, Object data) {
      super(Issue.TYPE_HANDLER, msg, data);
    }

    @Override
    protected void buildOtherString(StringBuilder sb) {
      sb.append("cost time: ").append(costTime).append(" ms").append('\n');
    }
  }

}
