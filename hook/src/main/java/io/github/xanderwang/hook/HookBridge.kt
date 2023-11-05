package io.github.xanderwang.hook

import io.github.xanderwang.asu.ALog.d
import io.github.xanderwang.asu.ALog.e
import io.github.xanderwang.asu.string
import io.github.xanderwang.hook.core.IHookBridge
import io.github.xanderwang.hook.core.MethodHook
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.ServiceLoader

/**
 * 提供对外的统一的 hook 方法
 */
object HookBridge {
    /**
     * log tag
     */
    private const val TAG = "HookBridge"

    /**
     * hook 桥接
     */
    private var iHookBridge: IHookBridge? = null

    init {
        var iHookBridgeLoader = ServiceLoader.load(IHookBridge::class.java)
        var iHookBridgeIterator: Iterator<IHookBridge> = iHookBridgeLoader.iterator()
        if (!iHookBridgeIterator.hasNext()) {
            // multi dex 的时候，发现会找不到配置，目前通过设置 classloader 来处理
            iHookBridgeLoader = ServiceLoader.load(IHookBridge::class.java, IHookBridge::class.java.classLoader)
            iHookBridgeIterator = iHookBridgeLoader.iterator()
        }
        d(TAG, "HookBridge init has instance: ${iHookBridgeIterator.hasNext()}")
        while (iHookBridgeIterator.hasNext()) {
            iHookBridge = iHookBridgeIterator.next()
            d(TAG, "HookBridge init find instance: ${iHookBridge?.javaClass}")
        }
    }

    /**
     * 是否是 epic 库
     *
     * @return true 表示为 epic 库，false 表示为其他
     */
    val isEpic: Boolean
        get() = IHookBridge.TYPE_EPIC == iHookBridge?.hookType

    /**
     * 是否是 SandHook 库
     *
     * @return true 表示为 SandHook 库，false 表示为其他
     */
    val isSandHook: Boolean
        get() = IHookBridge.TYPE_SAND_HOOK == iHookBridge?.hookType

    /**
     * 是否是 FastHook 库
     *
     * @return true 表示为 FastHook 库，false 表示为其他
     */
    val isFastHook: Boolean
        get() = IHookBridge.TYPE_FAST_HOOK == iHookBridge?.hookType

    /**
     * hook 所有的构造方法
     *
     * @param clazz    需要 hook 的类
     * @param callback hook 后的回调方法
     */
    @JvmStatic
    fun hookAllConstructors(clazz: Class<*>, callback: MethodHook?) {
        val constructors = clazz.declaredConstructors
        assertNotNullOrEmpty(constructors)
        for (constructor in constructors) {
            hookMethod(constructor, callback)
        }
    }

    /**
     * hook 所有的方法
     *
     * @param clazz              需要 hook 的方法的类
     * @param methodName         需要 hook 的方法
     * @param methodHookCallback hook 后的回调方法
     */
    fun findAllAndHookMethod(clazz: Class<*>, methodName: String, methodHookCallback: MethodHook?) {
        val methods = findMethods(clazz, methodName, true)
        assertNotNullOrEmpty(methods)
        for (method in methods) {
            hookMethod(method, methodHookCallback)
        }
    }

    /**
     * hook 指定的方法
     *
     * @param clazz                     需要 hook 的方法的类
     * @param methodName                需要 hook 的方法
     * @param parameterTypesAndCallback 需要 hook 的方法，以及 hook 后的回调，需要注意的是，回调需要放到最后
     */
    @JvmStatic
    fun findAndHookMethod(clazz: Class<*>, methodName: String, vararg parameterTypesAndCallback: Any?) {
        var callback: Any? = null
        var parameterTypes: Array<Class<*>>? = null
        // 默认 parameterTypesAndCallback 里面最后一个是 MethodHook
        if (parameterTypesAndCallback.isNotEmpty()) {
            callback = parameterTypesAndCallback.last()
            val parameterTypeList = parameterTypesAndCallback.dropLast(1)
            val typeList = ArrayList<Class<*>>()
            parameterTypeList.forEach {
                typeList.add(it as Class<*>)
            }
            if (typeList.isNotEmpty()) {
                parameterTypes = typeList.toTypedArray()
            }
        }
        assertNotNullOrEmpty(callback)
        val methodCallback = callback as MethodHook?
        val methods: List<Method> = if (null != parameterTypes) {
            findMethods(clazz, methodName, false, *parameterTypes)
        } else {
            findMethods(clazz, methodName, false)
        }
        assertNotNullOrEmpty(methods)
        for (method in methods) {
            hookMethod(method, methodCallback)
        }
    }

    /**
     * hook hookMethod
     *
     * @param hookMethod 需要 hook 的 hookMethod
     * @param callback   hook 后的回调
     */
    @JvmStatic
    fun hookMethod(hookMethod: Member?, callback: MethodHook?) {
        assertNotNullOrEmpty(iHookBridge)
        d(TAG, "hookMethod: ${hookMethod.string()}")
        try {
            iHookBridge?.hookMethod(hookMethod, callback)
        } catch (e: Exception) {
            e(TAG, "hookMethod", e)
        }
    }

    /**
     * 查找方法列表
     *
     * @param clazz          方法所属的类
     * @param methodName     方法名
     * @param justCheckName  是否只检查名称，而忽略传参
     * @param parameterTypes 方法传参列表类型
     * @return 所有符合条件的方法
     */
    private fun findMethods(
        clazz: Class<*>,
        methodName: String,
        justCheckName: Boolean,
        vararg parameterTypes: Class<*>?
    ): List<Method> {
        val methodList: MutableList<Method> = ArrayList()
        if (justCheckName) {
            // getMethods          public 的方法，包括继承的
            // getDeclaredMethods  自身定义的和实现的接口的方法
            val methods = clazz.declaredMethods
            for (method in methods) {
                if (method.name == methodName) {
                    methodList.add(method)
                }
            }
        } else {
            try {
                if (parameterTypes.isNotEmpty()) {
                    methodList.add(clazz.getDeclaredMethod(methodName, *parameterTypes))
                } else {
                    methodList.add(clazz.getDeclaredMethod(methodName))
                }
            } catch (e: Exception) {
                e(TAG, "findMethods", e)
            }
        }
        return methodList
    }

    /**
     * 断言非空
     *
     * @param someObj 需要判断的实例
     */
    private fun assertNotNullOrEmpty(someObj: Any?): Boolean {
        if (null == someObj) {
            e(TAG, "assertNotNullOrEmpty", IllegalArgumentException("null object!!!"))
            // throw new IllegalArgumentException("null object!!!");
            return false
        }
        if (someObj is List<*>) {
            if (someObj.isEmpty()) {
                e(TAG, "assertNotNullOrEmpty", IllegalArgumentException("empty list!!!"))
                // throw new IllegalArgumentException("empty list!!!");
                return false
            }
        }
        if (someObj is Array<*>) {
            if (someObj.isEmpty()) {
                e(TAG, "assertNotNullOrEmpty", IllegalArgumentException("empty array!!!"))
                // throw new IllegalArgumentException("empty array!!!");
                return false
            }
        }
        return true
    }
}