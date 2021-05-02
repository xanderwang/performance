package io.github.xanderwang.performance.demo;

import android.app.Application;
import android.content.Context;

import io.github.xanderwang.performance.PERF;

import java.io.File;

/**
 * @author Xander Wang
 * Created on 2021/1/25.
 * @Description //TODO
 */
public class JavaApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    initPERF(this);
  }

  private void initPERF(final Context context) {
    final PERF.LogFileUploader logFileUploader = new PERF.LogFileUploader() {
      @Override
      public boolean upload(File logFile) {
        return false;
      }
    };
    PERF.init(new PERF.Builder()
        .checkUI(true, 100) // 检查 ui lock
        .checkIPC(true) // 检查 ipc 调用
        .checkFps(true, 1000) // 检查 fps
        .checkThread(true) // 检查线程和线程池
        .globalTag("test_perf") // 全局 logcat tag ,方便过滤
        .cacheDirSupplier(new PERF.IssueSupplier<File>() {
          @Override
          public File get() {
            // issue 文件保存目录
            return context.getCacheDir();
          }
        })
        .maxCacheSizeSupplier(new PERF.IssueSupplier<Integer>() {
          @Override
          public Integer get() {
            // issue 文件最大占用存储空间
            return 10 * 1024 * 1024;
          }
        })
        .uploaderSupplier(new PERF.IssueSupplier<PERF.LogFileUploader>() {
          @Override
          public PERF.LogFileUploader get() {
            // issue 文件上传接口
            return logFileUploader;
          }
        })
        .build());
  }

}
