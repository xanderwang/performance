package io.github.xanderwang.hook;

import io.github.xanderwang.hook.core.IHookBridge;
import io.github.xanderwang.hook.core.MethodHook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import io.github.xanderwang.asu.aLog;
import io.github.xanderwang.asu.aUtil;

/**
 * 提供对外的统一的 hook 方法
 */
public class HookBridge {

    /**
     * log tag
     */
    private static final String TAG = "HookBridge";

    /**
     * hook 桥接
     */
    static IHookBridge iHookBridge = null;

    static {
        ServiceLoader<IHookBridge> iHookBridgeLoader = ServiceLoader.load(IHookBridge.class);
        Iterator<IHookBridge> iterator = iHookBridgeLoader.iterator();
        if (!iterator.hasNext()) {
            // multi dex 的时候，发现会找不到配置，目前通过设置 classloader 来处理
            iHookBridgeLoader = ServiceLoader.load(IHookBridge.class, IHookBridge.class.getClassLoader());
            iterator = iHookBridgeLoader.iterator();
        }
        aLog.e(TAG, "HookBridge init has instance: %s", iterator.hasNext());
        while (iterator.hasNext()) {
            iHookBridge = iterator.next();
            aLog.e(TAG, "HookBridge init find instance: %s", iHookBridge.getClass());
        }
    }

    /**
     * 是否是 epic 库
     *
     * @return true 表示为 epic 库，false 表示为其他
     */
    public static boolean isEpic() {
        return IHookBridge.TYPE_EPIC == iHookBridge.getHookType();
    }

    /**
     * 是否是 SandHook 库
     *
     * @return true 表示为 SandHook 库，false 表示为其他
     */
    public static boolean isSandHook() {
        return IHookBridge.TYPE_SAND_HOOK == iHookBridge.getHookType();
    }

    /**
     * 是否是 FastHook 库
     *
     * @return true 表示为 FastHook 库，false 表示为其他
     */
    public static boolean isFastHook() {
        return IHookBridge.TYPE_FAST_HOOK == iHookBridge.getHookType();
    }

    /**
     * hook 所有的构造方法
     *
     * @param clazz    需要 hook 的类
     * @param callback hook 后的回调方法
     */
    public static void hookAllConstructors(Class<?> clazz, MethodHook callback) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        assertNotNullOrEmpty(constructors);
        if (null == constructors) {
            return;
        }
        for (Constructor<?> constructor : constructors) {
            hookMethod(constructor, callback);
        }
    }

    /**
     * hook 所有的方法
     *
     * @param clazz              需要 hook 的方法的类
     * @param methodName         需要 hook 的方法
     * @param methodHookCallback hook 后的回调方法
     */
    public static void findAllAndHookMethod(Class<?> clazz, String methodName, MethodHook methodHookCallback) {
        List<Method> methodList = findMethodList(clazz, methodName, true, (Class<?>) null);
        assertNotNullOrEmpty(methodList);
        for (Method method : methodList) {
            hookMethod(method, methodHookCallback);
        }
    }

    /**
     * hook 指定的方法
     *
     * @param clazz                     需要 hook 的方法的类
     * @param methodName                需要 hook 的方法
     * @param parameterTypesAndCallback 需要 hook 的方法，以及 hook 后的回调，需要注意的是，回调需要放到最后
     */
    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        Object callback = null;
        Class<?>[] parameterTypes = null;
        // 默认 parameterTypesAndCallback 里面最后一个是 MethodHook
        if (null != parameterTypesAndCallback && parameterTypesAndCallback.length > 0) {
            int len = parameterTypesAndCallback.length - 1;
            parameterTypes = new Class<?>[len];
            for (int i = 0; i < len; i++) {
                parameterTypes[i] = (Class<?>) parameterTypesAndCallback[i];
            }
            callback = parameterTypesAndCallback[len];
        }

        assertNotNullOrEmpty(callback);
        MethodHook methodCallback = (MethodHook) callback;

        List<Method> methodList = findMethodList(clazz, methodName, parameterTypes);
        assertNotNullOrEmpty(methodList);
        for (Method method : methodList) {
            hookMethod(method, methodCallback);
        }
    }

    /**
     * hook hookMethod
     *
     * @param hookMethod 需要 hook 的 hookMethod
     * @param callback   hook 后的回调
     */
    public static void hookMethod(Member hookMethod, MethodHook callback) {
        assertNotNullOrEmpty(iHookBridge);
        aLog.d(TAG, "hookMethod: %s", aUtil.memberToString(hookMethod));
        try {
            iHookBridge.hookMethod(hookMethod, callback);
        } catch (Exception e) {
            aLog.e(TAG, "hookMethod", e);
        }
    }

    /**
     * 查找方法列表
     *
     * @param clazz          方法所属的类
     * @param methodName     方法名
     * @param parameterTypes 方法传参列表类型
     * @return 所有符合条件的方法
     */
    private static List<Method> findMethodList(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return findMethodList(clazz, methodName, false, parameterTypes);
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
    private static List<Method> findMethodList(Class<?> clazz, String methodName, boolean justCheckName,
        Class<?>... parameterTypes) {
        List<Method> list = new ArrayList<>();
        if (justCheckName) {
            // getMethods  public 的方法，包括继承的
            // getDeclaredMethods 自身定义的和实现的接口的方法，无论是
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    list.add(method);
                }
            }
        } else {
            try {
                list.add(clazz.getDeclaredMethod(methodName, parameterTypes));
            } catch (Exception e) {
                aLog.e(TAG, "findMethodList", e);
            }
        }
        return list;
    }

    /**
     * 断言非空
     *
     * @param object 需要判断的实例
     */
    private static void assertNotNullOrEmpty(Object object) {
        if (null == object) {
            aLog.e(TAG, "assertNotNullOrEmpty", new IllegalArgumentException("null object!!!"));
            // throw new IllegalArgumentException("null object!!!");
        }
        if (object instanceof List) {
            if (((List<?>) object).isEmpty()) {
                aLog.e(TAG, "assertNotNullOrEmpty", new IllegalArgumentException("empty list!!!"));
                // throw new IllegalArgumentException("empty list!!!");
            }
        }
        if (object instanceof Member[]) {
            if (((Member[]) object).length == 0) {
                aLog.e(TAG, "assertNotNullOrEmpty", new IllegalArgumentException("empty array!!!"));
                // throw new IllegalArgumentException("empty array!!!");
            }
        }
    }
}
