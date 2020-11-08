package com.xander.performance;

public class pTool {

  public static class Builder {

    public Builder checkANR(boolean check) {
      return this;
    }

    public Builder checkANR(boolean check, long time) {
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

    @Deprecated
    public Builder checkHandler(boolean check, long time) {
      return this;
    }

    public Builder globalTag(String tag) {
      return this;
    }

    public Builder build() {
      return this;
    }

  }

  public static void init(Builder builder) {

  }

}
