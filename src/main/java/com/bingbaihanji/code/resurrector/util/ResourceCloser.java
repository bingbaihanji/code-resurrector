package com.bingbaihanji.code.resurrector.util;

/**
 * 资源关闭工具类
 * 提供安全关闭 AutoCloseable 资源的方法，忽略关闭时的异常
 */
public final class ResourceCloser {

    /**
     * 尝试关闭单个资源，忽略所有异常
     *
     * @param c 要关闭的资源
     */
    public static void tryClose(final AutoCloseable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 尝试关闭多个资源，忽略所有异常
     *
     * @param items 要关闭的资源数组
     */
    public static void tryClose(final AutoCloseable... items) {
        if (items == null) {
            return;
        }
        for (AutoCloseable c : items) {
            tryClose(c);
        }
    }
}
