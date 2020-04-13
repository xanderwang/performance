package com.xander.performance.tool;

import android.util.Log;

import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.MethodParams;
import com.swift.sandhook.annotation.ThisObject;
import com.swift.sandhook.wrapper.HookErrorException;
import com.swift.sandhook.wrapper.HookWrapper;

import java.lang.reflect.Method;
import java.security.AccessControlContext;

/**
 * @ProjectName: performace
 * @Package: com.xander.performace.tool
 * @ClassName: ThreadTool
 * @Description: 用来监听自定义线程的创建，打印创建线程的堆栈
 * 主要原理就是通过反射，在调用构造方法的时候，
 * @Author: Xander
 * @CreateDate: 2020/4/13 22:30
 * @Version: 1.0
 */
public class ThreadTool {

    private static final String TAG = "ThreadTool";

    public static void init() {
        try {
            SandHook.addHookClass(ThreadHooker.class);
        } catch (HookErrorException e) {
            e.printStackTrace();
        }
    }

    @HookClass(Thread.class)
    static class ThreadHooker {

        @HookMethodBackup("start")
        static Method startOrigin;

        @HookMethodBackup("start")
        static HookWrapper.HookEntity startBackUp;

        @HookMethod("start")
        public static void startMethodHooked(Thread thread) {
            Log.e(TAG, "startMethodHooked:" + thread.getClass().getSimpleName());
            Log.e(TAG, "startMethodHooked startOrigin:" + startOrigin);
            Log.e(TAG, "startMethodHooked startBackUp:" + startBackUp);
            try {
                startBackUp.callOrigin(thread);
//                SandHook.callOriginByBackup(startOrigin, thread);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        /*@HookMethodBackup("init")
        @MethodParams({
                ThreadGroup.class, Runnable.class, String.class, long.class, AccessControlContext.class
        })
        static HookWrapper.HookEntity initBackUp;

        @HookMethod("init")
        @MethodParams({
                ThreadGroup.class, Runnable.class, String.class, long.class, AccessControlContext.class
        })
        public static void initMethodHooked(Thread thread, ThreadGroup g, Runnable target, String name,
                                            long stackSize, AccessControlContext acc) {
            Log.e(TAG, "initMethodHooked:" + thread.getClass().getSimpleName());
            Log.e(TAG, "initMethodHooked ThreadGroup:" + g);
            Log.e(TAG, "initMethodHooked target:" + target);
            Log.e(TAG, "initMethodHooked name:" + name);
            Log.e(TAG, "initMethodHooked stackSize:" + stackSize);
            Log.e(TAG, "initMethodHooked acc:" + acc);
            try {
                initBackUp.callOrigin(thread, g, true, name, stackSize, acc);
                //SandHook.callOriginByBackup(initBackUp, thread);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }*/
    }
}
