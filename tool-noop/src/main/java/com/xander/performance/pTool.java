package com.xander.performance;

public class pTool {

  public static void init(Builder builder) {

  }

  public static class Builder {
    /**
     * 是否开启 debug 模式，这种模式下会打印相对多的 log
     */
    boolean debug = true;
    /**
     * 是否开启检测 ANR
     */
    boolean mCheckANR = true;
    /**
     * ANR 的触发时间
     */
    long mAnrTime = 5000;
    /**
     * 检测线程的 start 方法调用栈
     */
    boolean mCheckThread = true;
    /**
     * 是否检测 fps
     */
    boolean mCheckFPS = true;

    public Builder checkANR(boolean check) {
      mCheckANR = check;
      return this;
    }

    public Builder checkANR(boolean check, long time) {
      mCheckANR = check;
      mAnrTime = time;
      return this;
    }

    public Builder checkThread(boolean check) {
      mCheckThread = check;
      return this;
    }

    public Builder checkFps(boolean check) {
      mCheckFPS = check;
      return this;
    }

    public Builder build() {
      return this;
    }

  }

}
