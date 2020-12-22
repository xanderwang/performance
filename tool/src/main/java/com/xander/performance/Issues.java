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
 * @Description //TODO
 */
public class Issues {

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

  protected static SparseArray<List<Issues>> issuesMap = new SparseArray<>(5);

  static ExecutorService service = Executors.newSingleThreadExecutor();

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

  protected byte[] dataBuffer;
  /**
   * 工作模式 0 表示打印， 1 表示保存到文件
   */
  protected int workMode = 0;

  public Issues(int type, String msg, Object data) {
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

  protected void insertToList() {
    List<Issues> issuesArray = issuesMap.get(type);
    if (null == issuesArray || issuesArray.size() >= MAX_ISSUE_COUNT) {
      synchronized (Issues.class) {
        if (null == issuesArray || issuesArray.size() >= MAX_ISSUE_COUNT) {
          // 双重确认后，才可以继续
          if (null != issuesArray) {
            // 保存到文件 待后续实现
            saveIssue(type, issuesArray);
          }
          issuesArray = new ArrayList<>(ISSUE_COUNT);
          issuesMap.put(type, issuesArray);
        }
      }
    }
    issuesArray.add(this);
  }

  private static void saveIssue(int type, List<Issues> list) {
    // 根据 type 打开文件，然后写入文件
    // 需要注意的是，不同的 type 文件写入需要加锁，如果某个 type 经常需要保存
    // 需要调大这个类型的缓存数量
    service.submit(new SaveIssuesTask(type, list));
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
      StringBuilder sb = null;
      sb.append('\n');
      sb.append("type: ").append(typeToString()).append('\n');
      sb.append("msg: ").append(msg).append('\n');
      if (data instanceof List) {
        sb.append("data:\n");
        printList(tag, sb, (List) data);
      } else if (null != data) {
        sb.append("data: ").append(data).append('\n');
      }
      printOther(tag, sb);
      dataString = sb.toString();
      dataBuffer = dataString.getBytes();
    }
    log(tag, dataString);
    log(tag, "end ----------------------------------------------------------");
  }

  protected void printOther(String tag, StringBuilder sb) {

  }

  protected void printList(String tag, StringBuilder sb, List dataList) {
    for (int i = 0, len = dataList.size(); i < len; i++) {
      // sb.setLength(0);
      Object item = dataList.get(i);
      sb.append('\t').append(item).append('\n');
      // log(tag, sb.toString());
    }
  }

  public void print() {
    workMode = 0;
    printIssues();
  }

  private void save(MappedByteBuffer buffer) {
    workMode = 1;
    mappedByteBuffer = buffer;
    printIssues();
    mappedByteBuffer = null;
  }

  MappedByteBuffer mappedByteBuffer;

  protected void log(String tag, String msg) {
    if (workMode == 0) {
      xLog.w(tag, msg);
    } else {
      // save to file
      mappedByteBuffer.put(msg.getBytes());
      mappedByteBuffer.position();
    }
  }

  static class SaveIssuesTask implements Runnable {

    int type;
    List<Issues> issuesList;

    public SaveIssuesTask(int type, List<Issues> issuesList) {
      this.type = type;
      this.issuesList = issuesList;
    }

    @Override
    public void run() {
      // 根据 type 获取 文件
      String fileName = "issues_" + type + "_" + SystemClock.uptimeMillis() + ".log";
      File issueFile = new File(ISSUES_ROOT_DIR, fileName);
      if (issueFile.exists()) {
        issueFile.delete();
      }
      Log.e(pTool.TAG + "_issues", "save file:" + issueFile.getAbsolutePath());
      RandomAccessFile accessFile = null;
      try {
        issueFile.createNewFile();
        accessFile = new RandomAccessFile(issueFile.getAbsolutePath(), "rw");
        MappedByteBuffer mappedByteBuffer = accessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024 * 1);
        for (int i = 0, len = issuesList.size(); i < len; i++) {
          issuesList.get(i).save(mappedByteBuffer);
        }
        mappedByteBuffer.flip();
        accessFile.close();
      } catch (IOException e) {
        Log.e(pTool.TAG + "_issues", "IOException", e);
      }
    }
  }

  protected static String ISSUES_ROOT_DIR_NAME = "issues";
  protected static File ISSUES_ROOT_DIR;

  protected static void init(Context context) {
    tag = pTool.TAG + "_issue";
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
