package io.github.xanderwang.performance

import java.io.File

object PERF {

    fun init(builder: Builder?) {
    }

    class Builder {
        fun checkUI(check: Boolean): Builder {
            return this
        }

        fun checkUI(check: Boolean, blockTime: Long): Builder {
            return this
        }

        fun checkThread(check: Boolean): Builder {
            return this
        }

        fun checkThread(check: Boolean, threadBlockTime: Long): Builder {
            return this
        }

        fun checkFps(check: Boolean): Builder {
            return this
        }

        fun checkFps(check: Boolean, fpsIntervalTime: Long): Builder {
            return this
        }

        fun checkIPC(check: Boolean): Builder {
            return this
        }

        fun checkIPC(check: Boolean, ipcBlockTime: Long): Builder {
            return this
        }

        fun checkBitmap(check: Boolean): Builder {
            return this
        }

        fun filterClass(filter: Boolean): Builder {
            return this
        }

        fun globalTag(tag: String?): Builder {
            return this
        }

        fun cacheDirSupplier(cache: IssueSupplier<File?>?): Builder {
            return this
        }

        fun maxCacheSizeSupplier(cacheSize: IssueSupplier<Int?>?): Builder {
            return this
        }

        fun uploaderSupplier(uploader: IssueSupplier<LogFileUploader?>?): Builder {
            return this
        }

        fun logLevel(level: Int): Builder {
            return this
        }

        fun build(): Builder {
            return this
        }
    }

    interface IssueSupplier<T> {
        fun get(): T
    }

    interface LogFileUploader {
        fun upload(logFile: File?): Boolean
    }
}