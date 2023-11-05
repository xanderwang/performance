package io.github.xanderwang.asu

import android.util.Log

/**
 * 工具的常量类，用来放一些全局的常量
 *
 * @author xanderwang
 * @date: 20220108
 */
object AConstants {
    /**
     * log 等级
     */
    @JvmField
    var logLevel = Log.DEBUG

    /**
     * 全局 log
     */
    internal var globalTag = "asu"

    /**
     * tag 格式化
     */
    internal var tagFormat = "%s_%s"

    /**
     * 设置全局 tag
     *
     * @param tag 全局 tag
     */
    @JvmStatic
    fun setGlobalTag(tag: String?) {
        if (tag.isNullOrBlank()) {
            return
        }
        globalTag = tag
    }
}