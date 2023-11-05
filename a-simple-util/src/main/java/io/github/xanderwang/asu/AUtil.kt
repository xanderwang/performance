package io.github.xanderwang.asu

import org.json.JSONObject
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * a util class
 *
 * @author xanderwang
 * @date 20220108
 */
object AUtil {
    /**
     * 获取全局 log tag
     *
     * @param tag 自定义 tag
     * @return 格式化后的全局 tag
     */
    @JvmStatic
    internal fun logTag(tag: String?): String {
        return if (tag.isNullOrBlank()) {
            AConstants.globalTag
        } else format(AConstants.tagFormat, AConstants.globalTag, tag)
    }

    /**
     * 格式化 string
     * @param formatStr 格式化格式
     * @param args 参数
     * @return 格式化后的 string
     */
    @JvmStatic
    internal fun format(formatStr: String?, vararg args: Any?): String {
        formatStr ?: return AConstants.globalTag
        return if (args.isEmpty()) {
            formatStr
        } else String.format(formatStr, *args)
    }
}

/**
 * member to string
 * @return
 */
fun Member?.string(): String {
    if (this is Method) {
        return this.toString()
    }
    if (this is Constructor<*>) {
        return this.toString()
    }
    return if (null != this) {
        this.name
    } else "null"
}

/**
 * 把 object 转出 json string
 *
 * @return json 格式的 string
 */
fun Any.jsonString(): String {
    val jsonObject = JSONObject()
    val objFields = this.javaClass.declaredFields
    try {
        for (field in objFields) {
            field.isAccessible = true
            jsonObject.put(field.name, field.get(this))
        }
    } catch (e: Exception) {
    }
    return jsonObject.toString()
}