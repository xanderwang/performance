package com.xander.performance;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Xander Wang Created on 2020/12/8.
 * @Description
 */
public class Issue {

  private static final String TAG = "_Issues";
  private static String tag = "_Issues";
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
   * 检测主线程耗时任务，和 ANR 的检测有些区别
   */
  public static final int TYPE_HANDLER = 4;

  private static ExecutorService saveService = Executors.newSingleThreadExecutor();

  public static int MAX_ISSUE_COUNT = 30;
  public static int ISSUE_COUNT = 32;

  /**
   * 类型
   */
  protected int type = -1;
  /**
   * 消息
   */
  protected String msg = "";
  /**
   * 数据
   */
  protected Object data;
  /**
   * byte 数据，用来保持
   */
  protected byte[] dataBuffer;

  public Issue(int type, String msg, Object data) {
    this.type = type;
    this.msg = msg;
    this.data = data;
    // insertToList();
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

  private void printIssues() {
    log(tag, "start --------------------------------------------------------");
    String dataString = null;
    if (null == dataBuffer) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n=================================================\n");
      sb.append("type: ").append(typeToString()).append('\n');
      sb.append("msg: ").append(msg).append('\n');
      if (data instanceof List) {
        sb.append("data:\n");
        printList(sb, (List) data);
      } else if (null != data) {
        sb.append("data: ").append(data).append('\n');
      }
      printOther(sb);
      dataString = sb.toString();
      dataBuffer = dataString.getBytes();
      data = null; // 释放，节省内存
      log(tag, dataString);
    }
    log(tag, "end ----------------------------------------------------------");
  }

  protected void printOther(StringBuilder sb) {

  }

  protected void printList(StringBuilder sb, List dataList) {
    for (int i = 0, len = dataList.size(); i < len; i++) {
      Object item = dataList.get(i);
      sb.append('\t').append(item).append('\n');
    }
  }

  public void print() {
    printIssues();
    saveService.execute(new SaveIssueTask(this));
  }


  protected void log(String tag, String msg) {
    xLog.w(tag, msg);
  }

  static class SaveIssueTask implements Runnable {

    Issue issue;

    public SaveIssueTask(Issue issue) {
      this.issue = issue;
    }

    @Override
    public void run() {
      MappedByteBuffer buffer = gMappedByteBuffer();
      if (buffer.remaining() < issue.dataBuffer.length) {
        byte[] space = new byte[buffer.remaining()];
        buffer.put(space);
        zipLogFile();
        createMappedByteBuffer();
        buffer = gMappedByteBuffer();
      }
      buffer.put(issue.dataBuffer);
      issue.dataBuffer = null;
    }
  }

  protected static final int BUFFER_SIZE = 1 * 1024 * 1024;

  private static MappedByteBuffer buffer;

  protected static MappedByteBuffer gMappedByteBuffer() {
    if (null == buffer) {
      createMappedByteBuffer();
    }
    return buffer;
  }

  protected static void createMappedByteBuffer() {
    // 先遍历保存文件目录，是否有没有写完的 log 文件，
    // 如果有，就载入，
    // 如果没有，就新建
    if (null != buffer) {
      buffer.flip();
      buffer = null;
    }
    String fileName = "issues_" + SystemClock.uptimeMillis() + ".log";
    File issueFile = new File(ISSUES_ROOT_DIR, fileName);
    if (issueFile.exists()) {
      issueFile.delete();
    }
    Log.e(tag, "issues save in:" + issueFile.getAbsolutePath());
    RandomAccessFile accessFile = null;
    try {
      issueFile.createNewFile();
      accessFile = new RandomAccessFile(issueFile.getAbsolutePath(), "rw");
      buffer = accessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, BUFFER_SIZE);
      accessFile.close();
    } catch (IOException e) {
      Log.e(pTool.TAG + "_issues", "IOException", e);
    }
  }

  protected static void zipLogFile() {

  }


  protected static String ISSUES_ROOT_DIR_NAME = "issues";
  protected static File ISSUES_ROOT_DIR;

  protected static void init(Context context) {
    tag = pTool.TAG + TAG;
    ISSUES_ROOT_DIR = new File(appSaveFileRootDir(context), ISSUES_ROOT_DIR_NAME);
    ISSUES_ROOT_DIR.mkdirs();
    xLog.e(tag, "issues save in:" + ISSUES_ROOT_DIR.getAbsolutePath());
  }

  private static File appSaveFileRootDir(Context context) {
    File saveFileDir = null;
    if (null == context) {
      saveFileDir = Environment.getExternalStorageDirectory();
    } else {
      saveFileDir = context.getCacheDir();
    }
    return saveFileDir;
  }
}
