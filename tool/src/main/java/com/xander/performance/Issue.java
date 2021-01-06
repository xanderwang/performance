package com.xander.performance;

import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Xander Wang Created on 2020/12/8.
 * @Description
 */
public class Issue {

  private static String TAG = "_Issue";
  /**
   * 检测 UI block
   */
  public static final int TYPE_UI_BLOCK = 0;
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
  @Deprecated
  public static final int TYPE_HANDLER = 4;

  private static ExecutorService saveService = null;

  /**
   * 虽然 SimpleDateFormat 是线程不安全的，但是这里只在单线程池里面使用，
   *
   * 所以这样写没有太大的问题
   */
  private static SimpleDateFormat dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss",
      Locale.US
  );

  /**
   * 类型
   */
  protected int type = -1;
  /**
   * 消息
   */
  protected String msg = "";
  /**
   * 发生的时间
   */
  protected String createTime = "";
  /**
   * 数据
   */
  protected Object data;
  /**
   * byte 数据，用来缓存数据的 string 数组
   */
  protected byte[] dataBytes;

  public Issue(int type, String msg, Object data) {
    this.type = type;
    this.msg = msg;
    createTime = dateFormat.format(new Date());
    this.data = data;
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
      case TYPE_UI_BLOCK:
        str = "UI BLOCK";
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
      default:
        str = "NONE";
    }
    return str;
  }

  private void buildIssueString() {
    String dataString = null;
    if (null == dataBytes) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n=================================================\n");
      sb.append("type: ").append(typeToString()).append('\n');
      sb.append("msg: ").append(msg).append('\n');
      sb.append("create time: ").append(createTime).append('\n');
      buildOtherString(sb);
      if (data instanceof List) {
        sb.append("trace:\n");
        buildListString(sb, (List) data);
      } else if (null != data) {
        sb.append("data: ").append(data).append('\n');
      }
      dataString = sb.toString();
      dataBytes = dataString.getBytes();
      data = null; // 释放，节省内存
      log(TAG, dataString);
    } else {
      log(TAG, new String(dataBytes));
    }
  }

  protected void buildOtherString(StringBuilder sb) {

  }

  protected void buildListString(StringBuilder sb, List<?> dataList) {
    for (int i = 0, len = dataList.size(); i < len; i++) {
      Object item = dataList.get(i);
      sb.append('\t').append(item).append('\n');
    }
  }

  protected void log(String tag, String msg) {
    xLog.w(tag, msg);
  }

  public void print() {
    buildIssueString();
    saveIssue(this);
  }

  static void saveIssue(Issue issue) {
    executorService().execute(new SaveIssueTask(issue));
  }

  static ExecutorService executorService() {
    if (saveService == null) {
      synchronized (Issue.class) {
        if (saveService == null) {
          saveService = Executors.newSingleThreadExecutor();
        }
      }
    }
    return saveService;
  }

  static class SaveIssueTask implements Runnable {

    Issue issue;

    public SaveIssueTask(Issue issue) {
      this.issue = issue;
    }

    @Override
    public void run() {
      MappedByteBuffer buffer = gMappedByteBuffer();
      if (buffer.remaining() < issue.dataBytes.length) {
        createLogFileAndBuffer();
        buffer = gMappedByteBuffer();
      }
      buffer.put(issue.dataBytes);
      int dataPosition = buffer.position();
      // xLog.e(TAG, "SaveIssueTask buffer at:" + dataPosition);
      gLineBytes = String.format(Locale.US, LINE_FORMAT, dataPosition).getBytes();
      buffer.position(0);
      buffer.put(gLineBytes);
      buffer.position(dataPosition);
      issue.dataBytes = null;
    }
  }

  private static String ISSUES_CACHE_DIR_NAME = "issues";
  private static File ISSUES_CACHE_DIR;
  private static IssueSupplier gIssueSupplier;

  private static final int MAX_BUFFER_SIZE = 20 * 1024 * 1024;

  private static final int BUFFER_SIZE = 1024 * 1024;
  private static File gLogFile;
  private static RandomAccessFile gRandomAccessFile;
  private static MappedByteBuffer gMappedByteBuffer;
  private static byte[] gLineBytes = String.valueOf(BUFFER_SIZE).getBytes();
  // log 文件的第一行固定为文件最后字节的位置
  private static final int gLineBytesLength = gLineBytes.length;
  private static final String LINE_FORMAT = "%-" + gLineBytesLength + "d";

  protected static MappedByteBuffer gMappedByteBuffer() {
    if (null == gMappedByteBuffer) {
      initMappedByteBuffer();
    }
    return gMappedByteBuffer;
  }

  protected static void createLogFileAndBuffer() {
    xLog.e(TAG, "createLogFileAndBuffer gBuffer:" + gMappedByteBuffer);
    if (null != gMappedByteBuffer) {
      gMappedByteBuffer.force();
      gMappedByteBuffer = null;
    }
    if (null != gRandomAccessFile) {
      try {
        gRandomAccessFile.close();
      } catch (IOException e) {
        xLog.e(TAG, "gRandomAccessFile IOException", e);
      }
      gRandomAccessFile = null;
    }
    if (null != gLogFile) {
      zipLogFile(gLogFile);
      gLogFile = null;
    }
    String fileName = "issues_" + SystemClock.elapsedRealtimeNanos() + ".log";
    gLogFile = new File(ISSUES_CACHE_DIR, fileName);
    if (gLogFile.exists()) {
      gLogFile.delete();
    }
    try {
      gLogFile.createNewFile();
      xLog.e(TAG, "create log file :" + gLogFile.getAbsolutePath());
      gRandomAccessFile = new RandomAccessFile(gLogFile.getAbsolutePath(), "rw");
      gMappedByteBuffer = gRandomAccessFile.getChannel()
          .map(FileChannel.MapMode.READ_WRITE, 0, BUFFER_SIZE);
      // 写入 line
      gLineBytes = String.format(Locale.US, LINE_FORMAT, 0).getBytes();
      gMappedByteBuffer.put(gLineBytes);
    } catch (IOException e) {
      xLog.e(TAG, "gRandomAccessFile IOException", e);
    }
  }

  protected static void initMappedByteBuffer() {
    // 遍历保存文件夹，按照创建时间排序，
    // 只处理 log 文件，然后最后一个 log 文件是上一次创建的，并初始化全局的 log file
    // 其他的 log 文件做压缩处理
    if (saveService == null) {
      saveService = Executors.newSingleThreadExecutor();
    }
    File[] files = ISSUES_CACHE_DIR.listFiles();
    if (null == files) {
      createLogFileAndBuffer();
      return;
    }
    Arrays.sort(files, new Comparator<File>() {
      @Override
      public int compare(File o1, File o2) {
        // 结果为负数表示 o2 排在前面
        return (int) (o2.lastModified() - o1.lastModified());
      }
    });
    int fileLength = 0;
    List<File> waitToZipLogFiles = new ArrayList<>();
    File lastLogFile = null;
    for (int i = 0; i < files.length; i++) {
      final File file = files[i];
      if( file.isDirectory() || !file.isFile() ) {
        continue;
      }
      fileLength += file.length();
      if (file.getName().endsWith(".log")) {
        if (lastLogFile == null) {
          lastLogFile = file;
          continue;
        }
        if (lastLogFile.lastModified() < file.lastModified()) {
          waitToZipLogFiles.add(lastLogFile);
          lastLogFile = file;
        } else {
          waitToZipLogFiles.add(file);
        }
      } else if (file.getName().endsWith(".zip")) {
        // 开始上传 log 文件
        executorService().execute(new Runnable() {
          @Override
          public void run() {
            boolean uploadResult = doUploadZipLogFile(file);
            if (uploadResult) {
              file.delete();
            }
          }
        });
      }
      if (fileLength >= gIssueSupplier.maxCacheSize() && file.exists()) {
        file.delete();
      }
    }
    xLog.e(TAG, "initMappedByteBuffer lastLogFile:" + lastLogFile);
    for (int i = 0, len = waitToZipLogFiles.size(); i < len; i++) {
      zipLogFile(waitToZipLogFiles.get(i));
    }
    if (null != lastLogFile) {
      // 处理 last log file 为全局的 log file
      try {
        gLogFile = lastLogFile;
        gRandomAccessFile = new RandomAccessFile(lastLogFile.getAbsolutePath(), "rw");
        gMappedByteBuffer = gRandomAccessFile.getChannel()
            .map(FileChannel.MapMode.READ_WRITE, 0, BUFFER_SIZE);
        gMappedByteBuffer.get(gLineBytes);
        String gLineString = new String(gLineBytes).trim();
        int lastPosition = gLineBytesLength;
        if (!TextUtils.isEmpty(gLineString)) {
          // 新创建出来文件就立刻 crash 了，导致之前没有写入任何内容
          // 写入 line
          gLineBytes = String.format(Locale.US, LINE_FORMAT, 0).getBytes();
          gMappedByteBuffer.put(gLineBytes);
          lastPosition = gMappedByteBuffer().position();
        } else {
          lastPosition = Integer.parseInt(new String(gLineBytes).trim());
        }
        xLog.e(TAG, "initMappedByteBuffer lastPosition:" + lastPosition);
        if (lastPosition >= BUFFER_SIZE) {
          createLogFileAndBuffer();
        } else {
          gMappedByteBuffer.position(lastPosition);
        }
      } catch (IOException e) {
        xLog.e(TAG, "initMappedByteBuffer", e);
        createLogFileAndBuffer();
      }
    } else {
      createLogFileAndBuffer();
    }
  }

  protected static void zipLogFile(final File logFile) {
    // 压缩 log 文件，成功后删除原始 log 文件
    // 上传成功后删除压缩后的 log file
    xLog.e(TAG, "zipLogFile:" + logFile);
    executorService().execute(new Runnable() {
      @Override
      public void run() {
        File zipLogFile = doZipLogFile(logFile);
        boolean uploadResult = doUploadZipLogFile(zipLogFile);
        if (uploadResult) {
          zipLogFile.delete();
        }
      }
    });
  }

  static File doZipLogFile(File logFile) {
    File zipLogFileDir = logFile.getParentFile();
    String zipLogFileName = logFile.getName().replace(".log", ".zip");
    File zipLogFile = new File(zipLogFileDir, zipLogFileName);
    if (zipLogFile.exists()) {
      // 清理已经压缩过但是没有删除的 log 文件
      logFile.delete();
      return zipLogFile;
    }
    try {
      xLog.e(TAG, "doZipLogFile src:" + logFile.getAbsolutePath());
      xLog.e(TAG, "doZipLogFile dst:" + zipLogFile.getAbsolutePath());
      FileOutputStream fos = new FileOutputStream(zipLogFile);
      ZipOutputStream zop = new ZipOutputStream(fos);
      ZipEntry zipEntry = new ZipEntry(logFile.getName());
      zop.putNextEntry(zipEntry);
      byte[] bytes = new byte[1024 * 64];
      int length;
      FileInputStream fip = new FileInputStream(logFile);
      while ((length = fip.read(bytes)) >= 0) {
        zop.write(bytes, 0, length);
      }
      zop.closeEntry();
      zop.close();
      fos.close();
      fip.close();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      logFile.delete();
    }
    return zipLogFile;
  }

  static boolean doUploadZipLogFile(File zipLogFile) {
    if (zipLogFile == null || !zipLogFile.exists()) {
      return false;
    }
    return gIssueSupplier.upLoad(zipLogFile);
  }

  static void resetTag(String tag) {
    TAG = tag + "_Issue";
  }

  protected static void init(IssueSupplier issueSupplier) {
    if (null == issueSupplier) {
      issueSupplier = new DefaultIssueSupplier();
    }
    gIssueSupplier = issueSupplier;
    ISSUES_CACHE_DIR = new File(issueSupplier.cacheRootDir(), ISSUES_CACHE_DIR_NAME);
    ISSUES_CACHE_DIR.mkdirs();
    xLog.e(TAG, "issues save in:" + ISSUES_CACHE_DIR.getAbsolutePath());
  }

  public static class DefaultIssueSupplier implements IssueSupplier {
    @Override
    public int maxCacheSize() {
      return MAX_BUFFER_SIZE;
    }

    @Override
    public File cacheRootDir() {
      return Environment.getExternalStorageDirectory();
    }

    @Override
    public boolean upLoad(File issueFile) {
      return false;
    }
  }

  public interface IssueSupplier {
    /**
     * 最大的磁盘缓存空间
     *
     * @return
     */
    int maxCacheSize();

    /**
     * 缓存根目录
     *
     * @return
     */
    File cacheRootDir();

    /**
     * 开始上传
     *
     * @param issueFile
     * @return true 表示上传成功 false 表示失败
     */
    boolean upLoad(File issueFile);
  }

}
