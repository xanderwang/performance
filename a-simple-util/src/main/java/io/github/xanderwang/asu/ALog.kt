package io.github.xanderwang.asu

import android.util.Log
import io.github.xanderwang.asu.AUtil.format
import io.github.xanderwang.asu.AUtil.logTag

/**
 * log 工具类，用来打印 log
 *
 * @author xanderwang
 * @date: 20220108
 */
object ALog {
    /**
     * log d
     *
     * @param tag
     * @param msg
     * @param args
     */
    @JvmStatic
    fun d(tag: String?, msg: String?, vararg args: Any?) {
        if (AConstants.logLevel <= Log.DEBUG) {
            Log.d(logTag(tag), format(msg, *args))
        }
    }

    /**
     * log d ，同时打印 error
     *
     * @param tag
     * @param msg
     * @param throwable
     */
    @JvmStatic
    fun d(tag: String?, msg: String?, throwable: Throwable) {
        if (AConstants.logLevel <= Log.DEBUG) {
            Log.d(logTag(tag), msg, throwable)
        }
    }

    /**
     * log w
     *
     * @param tag
     * @param msg
     * @param args
     */
    @JvmStatic
    fun w(tag: String?, msg: String?, vararg args: Any?) {
        if (AConstants.logLevel <= Log.WARN) {
            Log.w(logTag(tag), format(msg, *args))
        }
    }

    /**
     * log w
     *
     * @param tag
     * @param msg
     * @param throwable
     */
    @JvmStatic
    fun w(tag: String?, msg: String?, throwable: Throwable) {
        if (AConstants.logLevel <= Log.WARN) {
            Log.w(logTag(tag), msg, throwable)
        }
    }

    /**
     * log e
     *
     * @param tag
     * @param msg
     * @param args
     */
    @JvmStatic
    fun e(tag: String?, msg: String?, vararg args: Any?) {
        if (AConstants.logLevel <= Log.ERROR) {
            Log.e(logTag(tag), format(msg, *args))
        }
    }

    /**
     * log e
     *
     * @param tag
     * @param msg
     * @param throwable
     */
    @JvmStatic
    fun e(tag: String?, msg: String?, throwable: Throwable) {
        if (AConstants.logLevel <= Log.ERROR) {
            Log.e(logTag(tag), msg, throwable)
        }
    }
}