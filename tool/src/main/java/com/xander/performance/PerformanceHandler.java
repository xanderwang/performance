package com.xander.performance;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import java.util.HashMap;

import static com.xander.performance.PerformanceConfig.HANDLER_CHECK_TIME;

/**
 * @author Xander Wang Created on 2020/11/24.
 * @Description
 */
public class PerformanceHandler extends Handler {

  private static String TAG = pTool.TAG + "_PerformanceHandler";

  private static HashMap<String, Issues> msgIssuesMap = new HashMap<>();

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
      Issues msgIssues = new Issues(
          Issues.TYPE_HANDLER,
          "find handler issues",
          StackTraceUtils.list()
      );
      msgIssuesMap.put(msgKey, msgIssues);
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
    if (costTime > HANDLER_CHECK_TIME && msgIssuesMap.containsKey(msgKey)) {
      xLog.e(TAG, "costTime:" + costTime);
      Issues msgIssues = msgIssuesMap.get(msgKey);
      msgIssues.print();
    }
    msgIssuesMap.remove(msgKey);
  }

}
