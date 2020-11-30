package com.xander.performance;

import static com.xander.performance.PerformanceConfig.HANDLER_CHECK_TIME;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Xander Wang Created on 2020/11/24.
 * @Description //TODO
 */
public class PerformanceHandler extends Handler {

  private static final String TAG = pTool.TAG + "PerformanceHandler";

  private static HashMap<String, MsgStackInfo> msgStackInfoMap = new HashMap<>();

  @Override
  public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
    // 记录下来调用栈
    boolean result = super.sendMessageAtTime(msg, uptimeMillis);
    if (result) {
      String msgKey = Integer.toHexString(msg.hashCode());
      MsgStackInfo msgStackInfo = new MsgStackInfo();
      msgStackInfo.sendMsgStackTrace = StackTraceUtils.string(
          Thread.currentThread().getStackTrace(),
          true,
          this.getClass().getName()
      );
      msgStackInfoMap.put(msgKey, msgStackInfo);
      StackTraceUtils.print(
          TAG,
          Thread.currentThread().getStackTrace(),
          "SEND MSG",
          true,
          this.getClass().getName()
      );
    }
    return result;
  }

  @Override
  public void dispatchMessage(@NonNull Message msg) {
    long startTime = SystemClock.elapsedRealtime();
    super.dispatchMessage(msg);
    long costTime = SystemClock.elapsedRealtime() - startTime;
    String msgKey = Integer.toHexString(msg.hashCode());
    if (costTime > HANDLER_CHECK_TIME && msgStackInfoMap.containsKey(msgKey)) {
      // 打印
      xLog.e(TAG, msgStackInfoMap.get(msgKey).sendMsgStackTrace);
    }
  }


  static class MsgStackInfo {

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

}
