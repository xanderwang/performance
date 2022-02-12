package io.github.xanderwang.asu;

import android.util.Log;

/**
 * log 工具类，用来打印 log
 *
 * @author xanderwang
 * @date: 20220108
 */
public class aLog {

    public static void d(String tag, String formatMsg, Object... args) {
        if (aConstants.logLevel <= Log.DEBUG) {
            Log.d(aUtil.logTag(tag), aUtil.format(formatMsg, args));
        }
    }

    public static void de(String tag, String errMsg, Throwable throwable) {
        if (aConstants.logLevel <= Log.DEBUG) {
            Log.d(aUtil.logTag(tag), errMsg, throwable);
        }
    }

    public static void w(String tag, String formatMsg, Object... args) {
        if (aConstants.logLevel <= Log.WARN) {
            Log.w(aUtil.logTag(tag), aUtil.format(formatMsg, args));
        }
    }

    public static void we(String tag, String errMsg, Throwable throwable) {
        if (aConstants.logLevel <= Log.WARN) {
            Log.w(aUtil.logTag(tag), errMsg, throwable);
        }
    }

    public static void e(String tag, String formatMsg, Object... args) {
        if (aConstants.logLevel <= Log.ERROR) {
            Log.e(aUtil.logTag(tag), aUtil.format(formatMsg, args));
        }
    }

    public static void ee(String tag, String errMsg, Throwable throwable) {
        if (aConstants.logLevel <= Log.ERROR) {
            Log.e(aUtil.logTag(tag), errMsg, throwable);
        }
    }
}
