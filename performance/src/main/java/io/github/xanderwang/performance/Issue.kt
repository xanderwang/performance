package io.github.xanderwang.performance

import io.github.xanderwang.asu.ALog.d
import io.github.xanderwang.asu.ALog.e
import io.github.xanderwang.asu.ALog.w
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Comparator
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Issue 基类，用来封装
 * @param type 类型
 * @param msg  信息
 * @param data 详细数据
 */
open class Issue(type: Int, msg: String, data: Any?) {
    /**
     * 类型
     */
    var type = -1
        protected set

    /**
     * 消息
     */
    var msg = ""
        protected set

    /**
     * 发生的时间
     */
    private var createTime = ""

    /**
     * 数据
     */
    var data: Any?

    /**
     * byte 数据，用来缓存数据的 string 数组
     */
    protected var dataBytes: ByteArray? = null

    init {
        this.type = type
        this.msg = msg
        createTime = dateFormat.format(Date())
        this.data = data
    }

    /**
     * type 格式化为 string
     *
     * @return
     */
    private fun formatType(): String {
        return when (type) {
            TYPE_UI_BLOCK -> "UI BLOCK"
            TYPE_FPS -> "FPS"
            TYPE_IPC -> "IPC"
            TYPE_THREAD -> "THREAD"
            TYPE_BITMAP -> "BITMAP"
            else -> "UNKNOWN"
        }
    }

    /**
     * 格式化 issue
     */
    private fun formatIssue() {
        var issueString: String?
        if (dataBytes?.isNotEmpty() == true) {
            log(TAG, dataBytes?.decodeToString())
        } else {
            val sb = StringBuilder()
            sb.append("\n=================================================\n")
            sb.append("type: ").append(formatType()).append('\n')
            sb.append("msg: ").append(msg).append('\n')
            sb.append("create time: ").append(createTime).append('\n')
            formatExtraInfo(sb)
            if (data is List<*>) {
                sb.append("trace:\n")
                formatDataList(sb, data as List<*>)
            } else if (null != data) {
                sb.append("data: ").append(data).append('\n')
            }
            issueString = sb.toString()
            dataBytes = issueString.toByteArray()
            // data = null; // 释放，节省内存
            log(TAG, issueString)
        }
    }

    /**
     * 格式化其他的额外信息
     *
     * @param sb
     */
    protected open fun formatExtraInfo(sb: StringBuilder?) {

    }

    protected fun formatDataList(sb: StringBuilder?, dataList: List<*>?) {
        sb ?: return
        dataList?.let {
            for (item in it) {
                sb.append('\t').append(item).append('\n')
            }
        }
    }

    private fun log(tag: String?, msg: String?) {
        w(tag, msg)
    }

    fun print() {
        formatIssue()
        saveIssue(this)
    }

    /**
     * 保存 issue 的 task
     * @param issue 待保存的 issue
     */
    internal class SaveIssueTask(private var issue: Issue?) : Runnable {
        override fun run() {
            // 没有 issue 信息
            val issueDataBytes = issue?.dataBytes ?: return
            if (issueDataBytes.isEmpty()) {
                return
            }
            val logFileSize = gRandomAccessLogFile?.length() ?: return

            if (logFileSize + issueDataBytes.size > MAX_CACHE_SIZE) {
                // 超过大小，需要重建一个文件
                createLogFile()
            }
            // 写入文件
            gRandomAccessLogFile?.write(issueDataBytes)
            issue?.dataBytes = null
        }
    }


    companion object {
        /**
         * log tag
         */
        private const val TAG = "Issue"

        /**
         * 检测 UI block
         */
        const val TYPE_UI_BLOCK = 0

        /**
         * 检测 FPS
         */
        const val TYPE_FPS = 1

        /**
         * 检测 IPC，进程间通讯
         */
        const val TYPE_IPC = 2

        /**
         * 检测线程的创建
         */
        const val TYPE_THREAD = 3

        /**
         * 图片相关的
         */
        const val TYPE_BITMAP = 4

        /**
         * 任务线程池
         */
        @Volatile
        private var taskService = Executors.newSingleThreadExecutor()

        /**
         * 虽然 SimpleDateFormat 是线程不安全的，但是这里只在单线程池里面使用，
         *
         *
         * 所以这样写没有太大的问题
         */
        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)

        /**
         * 保存 issue
         *
         * @param issue
         */
        fun saveIssue(issue: Issue?) {
            executorService()?.execute(SaveIssueTask(issue))
        }

        /**
         * 获取线程池
         *
         * @return 线程池
         */
        private fun executorService(): ExecutorService? {
            if (taskService == null) {
                taskService = Executors.newSingleThreadExecutor()
            }
            return taskService
        }

        private const val ISSUES_CACHE_DIR_NAME = "perf_issues"

        private var ISSUES_CACHE_DIR: File? = null
        private var gCacheDirSupplier: PERF.IssueSupplier<File> = object : PERF.IssueSupplier<File> {
            override fun get(): File {
                return AppHelper.appContext?.cacheDir!!
            }
        }
        private var gMaxCacheSizeSupplier: PERF.IssueSupplier<Int> = object : PERF.IssueSupplier<Int> {
            override fun get(): Int {
                return MAX_CACHE_SIZE
            }
        }
        private val defLogFileUploader: PERF.LogFileUploader = object : PERF.LogFileUploader {
            override fun upload(logFile: File?): Boolean {
                return false
            }
        }

        private var gLogFileUploader: PERF.LogFileUploader? = null

        private fun logFileUploader(): PERF.LogFileUploader {
            return gLogFileUploader ?: defLogFileUploader
        }

        /** 单个 log 文件的最大大小 */
        private const val MAX_CACHE_SIZE = 10 * 1024 * 1024

        /** log 文件 */
        private var gLogFile: File? = null

        /** 随机读写文件，用来写入数据 */
        private var gRandomAccessLogFile: RandomAccessFile? = null
            get() {
                if (field == null) {
                    loadLogFile()
                }
                return field
            }

        /**
         * 初始化
         *
         * @param cacheDir        缓存目录
         * @param maxCacheSize    最大缓存大小
         * @param logFileUploader 文件上传
         */
        @JvmStatic
        fun init(
            cacheDir: PERF.IssueSupplier<File>?,
            maxCacheSize: PERF.IssueSupplier<Int>?,
            logFileUploader: PERF.LogFileUploader?
        ) {
            cacheDir?.let { gCacheDirSupplier = it }
            maxCacheSize?.let { gMaxCacheSizeSupplier = it }
            logFileUploader?.let { gLogFileUploader = it }
            val issuesCacheDir = File(gCacheDirSupplier.get(), ISSUES_CACHE_DIR_NAME)
            if (!issuesCacheDir.exists()) {
                issuesCacheDir.mkdirs()
            }
            ISSUES_CACHE_DIR = issuesCacheDir
            e(TAG, "issues save in: ${issuesCacheDir.absolutePath}")
            // 删除无用的旧文件
            cleanZipLogFiles()
        }

        private fun loadLogFile() {
            var raFile: RandomAccessFile? = null
            ISSUES_CACHE_DIR?.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".log") }
                ?.sortedWith(Comparator { a, b ->
                    return@Comparator (a.lastModified() - b.lastModified()).toInt()
                })
                ?.forEachIndexed { index, logFile ->
                    if (index == 0) {
                        gLogFile = logFile
                        raFile = RandomAccessFile(logFile.absolutePath, "rwd").apply {
                            this.seek(this.length())
                        }
                        d(TAG, "load log file:${logFile.absolutePath}")
                    } else {
                        asyncZipAndUploadLogFile(logFile)
                    }

                }
            if (null == raFile) {
                createLogFile()
            } else {
                gRandomAccessLogFile = raFile
            }
        }

        /**
         * 初始化 log 文件以及 log 对于的 buffer
         */
        protected fun createLogFile() {
            // 保存上一次的文件
            gLogFile?.let { logFile ->
                try {
                    gRandomAccessLogFile?.close()
                    asyncZipAndUploadLogFile(File(logFile.absolutePath))
                } catch (e: IOException) {
                    e(TAG, "createLogFile globalRAFile IOException", e)
                }
            }

            // 开始创建新的 log 文件
            val fileName = "issues_${dateFormat.format(Date())}.log"
            try {
                gLogFile = File(ISSUES_CACHE_DIR, fileName)
                // 先删除
                gLogFile?.delete()
                // 再创建，确保是创建了新的文件
                gLogFile?.createNewFile()
                d(TAG, "create log file: ${gLogFile?.absolutePath}")
                // "rwd" 模式可用于减少执行的 I/O 操作的数量
                gRandomAccessLogFile = RandomAccessFile(gLogFile?.absolutePath, "rwd").apply {
                    this.seek(this.length())
                }
            } catch (e: Exception) {
                e(TAG, "gRandomAccessFile IOException", e)
            }
        }

        /**
         * 异步压缩并删除 log 文件，同时上传 zip 文件
         *
         * @param logFile
         */
        protected fun asyncZipAndUploadLogFile(logFile: File?) {
            // 压缩 log 文件，成功后删除原始 log 文件
            // 上传成功后删除压缩后的 log file
            d(TAG, "asyncZipAndDeleteLogFile logFile: $logFile")
            executorService()?.submit {
                val zipLogFile = doZipAndDeleteLogFile(logFile)
                val uploadSuccess = doUploadZipLogFile(zipLogFile)
                if (uploadSuccess) {
                    try {
                        zipLogFile?.delete()
                    } catch (e: Exception) {
                        e(TAG, "asyncZipAndDeleteLogFile delete zip file: ${zipLogFile?.absolutePath}", e)
                    }
                }
            }
        }

        /**
         * 执行压缩 log 文件，并删除 log 文件
         *
         * @param logFile
         * @return
         */
        private fun doZipAndDeleteLogFile(logFile: File?): File? {
            var returnZipFile: File? = null
            val zipLogFileDir = logFile?.parentFile ?: return null
            val zipLogFileName = logFile.name.replace(".log", ".zip")
            val zipLogFile = File(zipLogFileDir, zipLogFileName)
            try {
                if (zipLogFile.exists()) {
                    // 如果 zip 文件和 log 文件都存在，说明有异常了，重新压缩一遍
                    zipLogFile.delete()
                }
                d(TAG, "doZipLogFile src: ${logFile.absolutePath}")
                d(TAG, "doZipLogFile dst: ${zipLogFile.absolutePath}")
                val fos = FileOutputStream(zipLogFile)
                val zop = ZipOutputStream(fos)
                val zipEntry = ZipEntry(logFile.name)
                zop.putNextEntry(zipEntry)
                val bytes = ByteArray(1024 * 64)
                var length: Int
                val fip = FileInputStream(logFile)
                while (fip.read(bytes).also { length = it } >= 0) {
                    zop.write(bytes, 0, length)
                }
                zop.closeEntry()
                zop.close()
                fos.close()
                fip.close()
                logFile.delete()
                returnZipFile = zipLogFile
            } catch (e: Exception) {
                d(TAG, "zip log file error: ${logFile.absolutePath}", e)
            }
            return returnZipFile
        }

        /**
         * 上传 log 文件
         *
         * @param zipLogFile
         * @return
         */
        private fun doUploadZipLogFile(zipLogFile: File?): Boolean {
            return if (zipLogFile == null || !zipLogFile.exists() || !zipLogFile.canRead()) {
                false
            } else {
                logFileUploader().upload(zipLogFile)
            }
        }

        /**
         * 删除已经压缩的 log 文件
         */
        private fun cleanZipLogFiles() {
            // .log 文件忽略，然后按时间排序，然后删除
            val maxCacheSize = gMaxCacheSizeSupplier.get().toLong()
            val deleteRunnable = Runnable {
                val files = ISSUES_CACHE_DIR?.listFiles() ?: return@Runnable
                if (files.isEmpty()) {
                    return@Runnable
                }
                Arrays.sort(files) { fileA, fileB ->
                    (fileA.lastModified() - fileB.lastModified()).toInt()
                }
                var fileLength: Long = 0
                files.filter { file ->
                    file.isFile && file.name.endsWith(".zip")
                }.forEach { file ->
                    if (fileLength >= maxCacheSize) {
                        if (doUploadZipLogFile(file)) {
                            file.delete()
                        }
                    } else {
                        fileLength += file.length()
                    }
                }
            }
            executorService()?.submit(deleteRunnable)
        }
    }
}