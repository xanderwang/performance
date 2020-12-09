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

  private static HashMap<String, HandlerIssues> msgIssuesMap = new HashMap<>();

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
      HandlerIssues msgIssues = new HandlerIssues("HANDLER DISPATCH MESSAGE", StackTraceUtils.list());
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
      HandlerIssues msgIssues = msgIssuesMap.get(msgKey);
      msgIssues.costTime = costTime;
      msgIssues.print();
    }
    msgIssuesMap.remove(msgKey);
  }

  static class HandlerIssues extends Issues {

    protected long costTime;

    public HandlerIssues(String msg, Object data) {
      super(Issues.TYPE_HANDLER, msg, data);
    }

    @Override
    protected void printOther(String tag, StringBuilder sb) {
      log(tag, sb.append("cost: ").append(costTime).append(" ms").toString());
    }
  }

}
