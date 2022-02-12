package io.github.xanderwang.asu;

import android.util.Log;

/**
 * 工具的常量类，用来放一些全局的常量
 *
 * @author xanderwang
 * @date: 20220108
 */
public class aConstants {
    /**
     * log 等级
     */
    public static int logLevel = Log.DEBUG;
    /**
     * 全局 log
     */
    protected static String globalTag = "asu";
    /**
     * tag 格式化
     */
    protected static String tagFormat = "%s_%s";

    /**
     * 设置全局 tag
     *
     * @param tag 全局 tag
     */
    public static void setGlobalTag(String tag) {
        if (null == tag || "".equals(tag.trim())) {
            return;
        }
        globalTag = tag;
    }

}
