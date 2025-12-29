package com.bingbaihanji.core.resurrector.util;

import java.util.Locale;

/**
 * 系统信息检测工具
 * 用于检测当前操作系统类型
 */
public class SystemInfoDetector {
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_NAME_LOWER = OS_NAME.toLowerCase(Locale.US);

    /**
     * 是否为 macOS 系统
     */
    public static boolean IS_MAC = OS_NAME_LOWER.startsWith("mac");

    /**
     * 是否为 Windows 系统
     */
    public static boolean IS_WINDOWS = OS_NAME_LOWER.startsWith("windows");

    /**
     * 是否为 Linux 系统
     */
    public static boolean IS_LINUX = OS_NAME_LOWER.startsWith("linux");
}
