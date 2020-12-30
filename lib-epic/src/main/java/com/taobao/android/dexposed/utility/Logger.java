package com.taobao.android.dexposed.utility;

import android.util.Log;

/**
 * Created by weishu on 17/11/10.
 */
public class Logger {

    private static int LOG_LEVEL = Log.DEBUG;

    public static String preFix = "epic_";

    public static void setLogLevel(int logLevel) {
        LOG_LEVEL = logLevel;
    }

    public static void setPreFix(String preFix) {
        Logger.preFix = preFix;
    }

    public static void i(String tag, String msg) {
        if (LOG_LEVEL <= Log.INFO) {
            Log.i(preFix + tag, msg);
        }
    }

    public static void d(String tagSuffix, String msg) {
        if (LOG_LEVEL <= Log.DEBUG) {
            Log.d(preFix + tagSuffix, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (LOG_LEVEL <= Log.WARN) {
            Log.w(preFix + tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (LOG_LEVEL <= Log.ERROR) {
            Log.e(preFix + tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (LOG_LEVEL <= Log.ERROR) {
            Log.e(preFix + tag, msg, e);
        }
    }

}
