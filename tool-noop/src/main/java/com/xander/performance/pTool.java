package com.xander.performance;

import android.content.Context;

public class pTool {

  public static class Builder {

    public Builder checkUI(boolean check) {
      return this;
    }

    public Builder checkUI(boolean check, long time) {
      return this;
    }

    public Builder checkThread(boolean check) {
      return this;
    }

    public Builder checkFps(boolean check) {
      return this;
    }

    public Builder checkIPC(boolean check) {
      return this;
    }

    public Builder globalTag(String tag) {
      return this;
    }

    public Builder appContext(Context context) {
      return this;
    }

    public Builder logLevel(int level) {
      return this;
    }

    public Builder build() {
      return this;
    }

  }

  public static void init(Builder builder) {

  }

}
