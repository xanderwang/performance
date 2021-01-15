package com.xander.performance;

import java.io.File;

public class PERF {

  public static class Builder {

    public Builder checkUI(boolean check) {
      return this;
    }

    public Builder checkUI(boolean check, long blockIntervalTime) {
      return this;
    }

    public Builder checkThread(boolean check) {
      return this;
    }

    public Builder checkFps(boolean check) {
      return this;
    }

    public Builder checkFps(boolean check, long fpsIntervalTime) {
      return this;
    }

    public Builder checkIPC(boolean check) {
      return this;
    }

    public Builder globalTag(String tag) {
      return this;
    }

    public Builder issueSupplier(IssueSupplier supplier) {
      return this;
    }

    public Builder logLevel(int level) {
      return this;
    }

    public Builder build() {
      return this;
    }

  }

  public interface IssueSupplier {
    /**
     * 最大的磁盘缓存空间
     *
     * @return
     */
    long maxCacheSize();

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

  public static void init(Builder builder) {

  }

}
