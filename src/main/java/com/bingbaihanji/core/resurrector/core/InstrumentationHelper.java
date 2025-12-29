package com.bingbaihanji.core.resurrector.core;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instrumentation API 辅助类，用于获取JVM中已加载的类信息
 */
public class InstrumentationHelper {

    private static volatile Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * 获取所有已加载的类
     *
     * @return 已加载的类数组，如果Instrumentation不可用则返回空数组
     */
    public static Class<?>[] getAllLoadedClasses() {
        if (instrumentation == null) {
            return new Class[0];
        }
        return instrumentation.getAllLoadedClasses();
    }

    /**
     * 获取所有已加载的类并按类加载器分组
     *
     * @return 按类加载器分组的类列表
     */
    public static List<Class<?>> getLoadedClassesByClassLoader(ClassLoader classLoader) {
        if (instrumentation == null) {
            return Collections.emptyList();
        }

        Class<?>[] allClasses = instrumentation.getAllLoadedClasses();
        List<Class<?>> result = new ArrayList<>();

        for (Class<?> clazz : allClasses) {
            if (clazz != null && clazz.getClassLoader() == classLoader) {
                result.add(clazz);
            }
        }

        return result;
    }

    /**
     * 检查是否可以使用Instrumentation API
     *
     * @return 如果可用返回true，否则返回false
     */
    public static boolean isInstrumentationAvailable() {
        return instrumentation != null;
    }

    /**
     * 获取Instrumentation不可用时的提示信息
     *
     * @return 提示信息
     */
    public static String getInstrumentationNotAvailableMessage() {
        return "\n注意：Instrumentation API 不可用。\n" +
                "要启用此功能，请使用 -javaagent 参数启动应用程序，例如：\n" +
                "java -javaagent:code-resurrector-0.7.0.jar -jar code-resurrector-0.7.0.jar\n" +
                "在当前模式下，只能显示有限的类加载器信息。";
    }
}