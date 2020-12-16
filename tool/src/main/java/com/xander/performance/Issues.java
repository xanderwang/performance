package com.xander.performance;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xander Wang Created on 2020/12/8.
 * @Description //TODO
 */
public class Issues {

  /**
   * 检测 ANR
   */
  public static final int TYPE_ANR     = 0;
  /**
   * 检测 FPS
   */
  public static final int TYPE_FPS     = 1;
  /**
   * 检测 IPC，进程间通讯
   */
  public static final int TYPE_IPC     = 2;
  /**
   * 检测线程的创建
   */
  public static final int TYPE_THREAD  = 3;
  /**
   * 检测主线程耗时任务，和 ANR 的检测有些区别
   */
  public static final int TYPE_HANDLER = 4;

  protected static SparseArray<List<Issues>> issuesMap = new SparseArray<>(8);

  /**
   * 类型
   */
  protected int    type = -1;
  /**
   * 消息
   */
  protected String msg  = "";
  /**
   * 数据
   */
  protected Object data;

  public Issues(int type, String msg, Object data) {
    this.type = type;
    this.msg = msg;
    this.data = data;
    insertToList();
  }

  public int getType() {
    return type;
  }

  public String getMsg() {
    return msg;
  }

  public Object getData() {
    return data;
  }

  protected void insertToList() {
    List<Issues> issuesArray = issuesMap.get(type);
    if (null == issuesArray || issuesArray.size() >= 30) {
      synchronized (Issues.class) {
        if (null == issuesArray || issuesArray.size() >= 30) {
          // 双重确认后，才可以继续
          if (null != issuesArray) {
            // todo 保存到文件 待后续实现
            // issuesArray = null;
          }
          if (null == issuesArray) {
            issuesArray = new ArrayList<>(32);
            issuesMap.put(type, issuesArray);
          }
        }
      }
    }
    issuesArray.add(this);
  }

  protected String typeToString() {
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
    StringBuilder sb = new StringBuilder();
    String tag = sb.append(pTool.TAG).append("_Issues").toString();
    log(tag, "start --------------------------------------------------------");
    sb.setLength(0);
    sb.append("type: ").append(typeToString());
    log(tag, sb.toString());
    sb.setLength(0);
    sb.append("msg: ").append(msg);
    log(tag, sb.toString());
    sb.setLength(0);
    printOther(tag, sb);
    if (null == data) {
      log(tag, "end ----------------------------------------------------------");
      return;
    }
    sb.setLength(0);
    if (data instanceof List) {
      printData(tag, sb, (List) data);
    }
    log(tag, "end ----------------------------------------------------------");
  }

  protected void printOther(String tag, StringBuilder sb) {

  }

  protected void printData(String tag, StringBuilder sb, List dataList) {
    for (int i = 0, len = dataList.size(); i < len; i++) {
      sb.setLength(0);
      Object item = dataList.get(i);
      sb.append('\t').append(item);
      log(tag, sb.toString());
    }
  }

  protected void log(String tag, String msg) {
    xLog.w(tag, msg);
  }
}
