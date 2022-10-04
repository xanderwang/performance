package io.github.xanderwang.performance;

/**
 * @author xander
 */
class Config {

    /**
     * 检测 ui 是否阻塞的阈值
     */
    public static long UI_BLOCK_TIME = 100L;
    /**
     * FPS 检测的时间间隔
     */
    public static long FPS_INTERVAL_TIME = 1000L;
    /**
     * 线程后台执行任务的时间检测间隔，超时就打印出来
     */
    public static long THREAD_BLOCK_TIME = 1000L;
    /**
     * IPC 通讯的耗时时间间隔
     */
    public static long IPC_BLOCK_TIME = 100L;
    /**
     * 是否过滤类名
     */
    public static boolean FILTER_CLASS_NAME = true;
}
