package com.xander.performance;

import static com.xander.performance.PerformanceConfig.HANDLER_CHECK_TIME;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;


import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Xander Wang Created on 2020/11/24.
 * @Description //TODO
 */
public class PerformanceHandler extends Handler {

  private static String TAG = pTool.TAG + "_PerformanceHandler";

  private static HashMap<String, MsgStackInfo> msgStackInfoMap = new HashMap<>();

  static void resetTag(String tag) {
    TAG = tag + "_PerformanceHandler";
  }

  @Override
  public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
    // 记录下来调用栈
    boolean result = super.sendMessageAtTime(msg, uptimeMillis);
    if (result) {
      String msgKey = Integer.toHexString(msg.hashCode());
      // xLog.e(TAG, "sendMessageAtTime msgKey:" + msgKey);
      MsgStackInfo msgStackInfo = new MsgStackInfo();
      msgStackInfo.sendMsgStackTrace = StackTraceUtils.string(
          Thread.currentThread().getStackTrace(),
          true,
          this.getClass().getName()
      );
      msgStackInfoMap.put(msgKey, msgStackInfo);
      // StackTraceUtils.print(
      //     TAG,
      //     Thread.currentThread().getStackTrace(),
      //     "SEND MSG",
      //     true,
      //     this.getClass().getName()
      // );
    }
    return result;
  }

  @Override
  public void dispatchMessage(Message msg) {
    String msgKey = Integer.toHexString(msg.hashCode());
    // xLog.e(TAG, "dispatchMessage msgKey:" + msgKey);
    long startTime = SystemClock.elapsedRealtime();
    super.dispatchMessage(msg);
    long costTime = SystemClock.elapsedRealtime() - startTime;
    if (costTime > HANDLER_CHECK_TIME && msgStackInfoMap.containsKey(msgKey)) {
      MsgStackInfo msgStackInfo = msgStackInfoMap.get(msgKey);
      msgStackInfo.costTime = costTime;
      // 打印
      xLog.e(TAG, msgStackInfo.toJson());
    }
    msgStackInfoMap.remove(msgKey);
  }


  static class MsgStackInfo {

    long costTime = 0;

    String sendMsgStackTrace;


    String toJson() {
      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("costTime", costTime);
        jsonObject.put("sendMsgStackTrace", sendMsgStackTrace);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return jsonObject.toString();
    }

  }

}
