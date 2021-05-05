package io.github.xanderwang.performance;

import java.io.File;

public class PERF {

  public static class Builder {

    public Builder checkUI(boolean check) {
      return this;
    }

    public Builder checkUI(boolean check, long blockTime) {
      return this;
    }

    public Builder checkThread(boolean check) {
      return this;
    }

    public Builder checkThread(boolean check, long threadBlockTime) {
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

    public Builder checkBitmap(boolean check) {
      return this;
    }

    public Builder globalTag(String tag) {
      return this;
    }

    public Builder cacheDirSupplier(IssueSupplier<File> cache) {
      return this;
    }

    public Builder maxCacheSizeSupplier(IssueSupplier<Integer> cacheSize) {
      return this;
    }

    public Builder uploaderSupplier(IssueSupplier<LogFileUploader> uploader) {
      return this;
    }

    public Builder logLevel(int level) {
      return this;
    }

    public Builder build() {
      return this;
    }

  }

  public interface IssueSupplier<T> {
    T get();
  }

  public interface LogFileUploader {
    boolean upload(File logFile);
  }

  public static void init(Builder builder) {

  }

}
