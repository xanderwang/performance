package io.github.xanderwang.performance

import android.content.Context
import androidx.startup.Initializer
import io.github.xanderwang.asu.ALog.e

/**
 * @author Xander Wang
 * Created on 2021/2/3.
 * @Description
 */
class PerfInitializer : Initializer<Boolean> {
    override fun create(context: Context): Boolean {
        e(TAG, "create")
        AppHelper.appContext = context.applicationContext
        return true
    }

    override fun dependencies(): List<Class<out Initializer<*>?>> {
        e(TAG, "dependencies")
        return ArrayList()
    }

    companion object {
        private const val TAG = "PerfInitializer"
    }
}