package com.xander.performance;

import java.util.List;

/**
 * @author Xander Wang Created on 2020/12/8.
 * @Description //TODO
 */
public class Issues {

  /**
   * 检测 ANR
   */
  public static final int TYPE_ANR = 0;
  /**
   * 检测 FPS
   */
  public static final int TYPE_FPS = 1;
  /**
   * 检测 IPC，进程间通讯
   */
  public static final int TYPE_IPC = 2;
  /**
   * 检测线程的创建
   */
  public static final int TYPE_THREAD = 3;
  /**
   * 检测主线程耗时任务
   */
  public static final int TYPE_HANDLER = 4;

  /**
   * 类型
   */
  private int type = -1;
  /**
   * 消息
   */
  private String msg = "";

  private Object data;

  public Issues() {
  }

  public Issues(int type, String msg, Object data) {
    this.type = type;
    this.msg = msg;
    this.data = data;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  private String typeToString() {
    String str = null;
    switch (type) {
      case TYPE_ANR:
        str = "ANR";
        break;
      case TYPE_FPS:
        str = "FPS";
        break;
      case TYPE_IPC:
        str = "IPC";
        break;
      case TYPE_THREAD:
        str = "THREAD";
        break;
      case TYPE_HANDLER:
        str = "HANDLER";
        break;
      default:
        str = "NONE";
    }
    return str;
  }

  public void print() {
    String tag = pTool.TAG + "_Issues";
    xLog.w(tag, "start --------------------------------------------------------");
    StringBuilder sb = new StringBuilder();
    sb.append("type:").append(typeToString());
    xLog.w(tag, sb.toString());
    sb.setLength(0);
    sb.append("msg:").append(msg);
    xLog.w(tag, sb.toString());
    sb.setLength(0);
    if (null == data) {
      xLog.w(tag, "end ----------------------------------------------------------");
      return;
    }
    if (data instanceof List) {
      List<Object> dataList = (List<Object>) data;
      for (int i = 0, len = dataList.size(); i < len; i++) {
        sb.setLength(0);
        Object item = dataList.get(i);
        sb.append('\t').append(item);
        xLog.w(tag, sb.toString());
      }
    }
    xLog.w(tag, "end ----------------------------------------------------------");
  }
}
