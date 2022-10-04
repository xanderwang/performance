package io.github.xanderwang.performance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @ProjectName: performace
 * @Package: com.xander.performance
 * @ClassName: StackTraceUtils
 * @Description:
 * @Author: Xander
 * @CreateDate: 2020/11/4 21:42
 * @Version: 1.0
 */
class StackTraceUtils {

    private static HashSet<String> IGNORE_CLASS_NAME_SET = new HashSet<>();
    private static ArrayList<String> IGNORE_CLASS_NAME_List = new ArrayList<>();

    static {
        IGNORE_CLASS_NAME_SET.add(BitmapTool.class.getName());
        IGNORE_CLASS_NAME_SET.add(IPCTool.class.getName());
        IGNORE_CLASS_NAME_SET.add(ThreadTool.class.getName());
        IGNORE_CLASS_NAME_SET.add(UIBlockTool.class.getName());
        IGNORE_CLASS_NAME_SET.add(StackTraceUtils.class.getName());

        IGNORE_CLASS_NAME_List.add(BitmapTool.class.getName());
        IGNORE_CLASS_NAME_List.add(IPCTool.class.getName());
        IGNORE_CLASS_NAME_List.add(ThreadTool.class.getName());
        IGNORE_CLASS_NAME_List.add(UIBlockTool.class.getName());

        IGNORE_CLASS_NAME_List.add("io.github.xanderwang.hook");
        IGNORE_CLASS_NAME_List.add("de.robv.android");
        IGNORE_CLASS_NAME_List.add("me.weishu.epic");
        IGNORE_CLASS_NAME_List.add("com.swift.sandhook");
        IGNORE_CLASS_NAME_List.add("SandHookerNew_");
    }

    /**
     * 是否是应该忽略的 class
     *
     * @param className
     * @return
     */
    private static boolean isIgnoreClass(String className) {
        if (null == className) {
            return false;
        }
        if (!Config.FILTER_CLASS_NAME) {
            return false;
        }
        if (IGNORE_CLASS_NAME_SET.contains(className)) {
            return true;
        }
        for (int i = IGNORE_CLASS_NAME_List.size() - 1; i >= 0; i--) {
            if (className.startsWith(IGNORE_CLASS_NAME_List.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static List<String> list() {
        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        return list(stackTraceElements);
    }

    public static List<String> list(Thread thread) {
        StackTraceElement[] stackTraceElements = thread.getStackTrace();
        return list(stackTraceElements);
    }

    public static List<String> list(StackTraceElement[] stackTraceElements) {
        List<String> list = new ArrayList<>(stackTraceElements.length);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0, len = stackTraceElements.length; i < len; i++) {
            StackTraceElement element = stackTraceElements[i];
            String lineLog = stringStackTraceElement(element, stringBuilder);
            if (null == lineLog) {
                continue;
            }
            list.add(lineLog);
        }
        return list;
    }

    private static String stringStackTraceElement(StackTraceElement element, StringBuilder stringBuilder) {
        String className = element.getClassName();
        if (isIgnoreClass(className)) {
            return null;
        }
        stringBuilder.delete(0, stringBuilder.length());
        stringBuilder.append(className).append('.').append(element.getMethodName()).append('(')
            .append(element.getFileName()).append(':').append(element.getLineNumber()).append(')');
        return stringBuilder.toString();
    }

}
