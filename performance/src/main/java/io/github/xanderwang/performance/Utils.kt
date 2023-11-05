package io.github.xanderwang.performance

/**
 * 工具类
 * @author xanderwang
 */
internal object Utils {

    /** 完整类名白名单 */
    private val ignoreClassWhiteSet = HashSet<String>()

    /** 类名白名单，可能是完整类名，也可能是简单类名 */
    private val ignoreClassWhiteList = ArrayList<String>()

    init {
        ignoreClassWhiteSet.add(BitmapTool::class.java.name)
        ignoreClassWhiteSet.add(IPCTool::class.java.name)
        ignoreClassWhiteSet.add(ThreadTool::class.java.name)
        ignoreClassWhiteSet.add(UIBlockTool::class.java.name)
        ignoreClassWhiteSet.add(Utils::class.java.name)

        ignoreClassWhiteList.add(BitmapTool::class.java.name)
        ignoreClassWhiteList.add(IPCTool::class.java.name)
        ignoreClassWhiteList.add(ThreadTool::class.java.name)
        ignoreClassWhiteList.add(UIBlockTool::class.java.name)
        ignoreClassWhiteList.add("io.github.xanderwang.hook")
        ignoreClassWhiteList.add("de.robv.android")
        ignoreClassWhiteList.add("me.weishu.epic")
        ignoreClassWhiteList.add("com.swift.sandhook")
        ignoreClassWhiteList.add("SandHookerNew_")
    }

    /**
     * 是否是应该忽略的 class
     *
     * @param className
     * @return
     */
    fun isIgnoreClass(className: String?): Boolean {
        if (null == className) {
            return false
        }
        if (!Config.FILTER_CLASS_NAME) {
            return false
        }
        if (ignoreClassWhiteSet.contains(className)) {
            return true
        }
        for (i in 0 until ignoreClassWhiteList.size) {
            if (className.startsWith(ignoreClassWhiteList[i])) {
                return true
            }
        }
        return false
    }

    fun list(): List<String> {
        val stackTraceElements = Throwable().stackTrace
        return list(stackTraceElements)
    }

    fun list(thread: Thread): List<String> {
        val stackTraceElements = thread.stackTrace
        return list(stackTraceElements)
    }

    fun list(stackTraceElements: Array<StackTraceElement>): List<String> {
        return stackTraceElements.strings()
    }


}

internal fun Array<StackTraceElement>.strings(): List<String> {
    val list: MutableList<String> = ArrayList(this.size)
    val sb = StringBuilder()
    this.forEach { element ->
        element.string(sb, true)?.let {
            list.add(it)
        }
    }
    return list
}

internal fun StackTraceElement.string(stringBuilder: StringBuilder?, cleanCache: Boolean = true): String? {
    val sb = stringBuilder ?: StringBuilder()
    if (cleanCache && sb.isNotEmpty()) {
        sb.delete(0, sb.length)
    }
    if (Utils.isIgnoreClass(className)) {
        return null
    }
    sb
        .append(className).append('.')
        .append(methodName)
        .append('(').append(fileName).append(':').append(lineNumber).append(')')
    return sb.toString()
}