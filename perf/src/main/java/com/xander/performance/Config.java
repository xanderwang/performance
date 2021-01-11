package com.xander.performance;

/**
 * @author xander
 */
class Config {

  /**
   * 检测 ui 是否阻塞的阈值
   */
  public static long UI_BLOCK_INTERVAL_TIME = 100L;

  public static long FPS_INTERVAL_TIME = 2000L;

  @Deprecated
  public static long MAX_HANDLER_DISPATCH_MSG_TIME = 100L;

}
