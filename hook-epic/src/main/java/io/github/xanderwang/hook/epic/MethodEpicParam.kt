package io.github.xanderwang.hook.epic

import de.robv.android.xposed.XC_MethodHook
import io.github.xanderwang.hook.core.MethodParam
import java.lang.reflect.Member

class MethodEpicParam : MethodParam {

    var methodHookParam: XC_MethodHook.MethodHookParam? = null

    override var result: Any?
        get() = methodHookParam?.result
        set(result) {
            methodHookParam?.result = result
        }

    override var throwable: Throwable?
        get() = methodHookParam?.throwable
        set(throwable) {
            methodHookParam?.throwable = throwable
        }

    override fun hasThrowable(): Boolean {
        return methodHookParam?.hasThrowable() == true
    }

    @get:Throws(Throwable::class)
    override val resultOrThrowable: Any?
        get() = methodHookParam?.resultOrThrowable


    override var args: Array<Any?>?
        get() = methodHookParam?.args
        set(args) {
            methodHookParam?.args = args
        }

    override var thisObject: Any?
        get() = methodHookParam?.thisObject
        set(thisObject) {
            methodHookParam?.thisObject = thisObject
        }

    override var method: Member?
        get() = methodHookParam?.method
        set(method) {
            methodHookParam?.method = method
        }
}