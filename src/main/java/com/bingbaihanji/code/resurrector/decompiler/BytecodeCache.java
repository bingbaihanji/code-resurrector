package com.bingbaihanji.code.resurrector.decompiler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 字节码缓存管理器
 */
public class BytecodeCache {

    private static final Map<String, byte[]> cache = new ConcurrentHashMap<>();
    private static JarFile currentJarFile;

    /**
     * 设置当前的JAR文件
     */
    public static void setCurrentJarFile(JarFile jarFile) {
        currentJarFile = jarFile;
        cache.clear();
    }

    /**
     * 清理缓存
     */
    public static void clear() {
        cache.clear();
        currentJarFile = null;
    }

    /**
     * 获取指定类的字节码
     *
     * @param internalName 内部类名 (如: java/lang/String)
     * @return 字节码数组，如果未找到返回null
     */
    public static byte[] getBytecode(String internalName) {
        if (internalName == null) {
            return null;
        }

        // 先从缓存中查找
        byte[] cached = cache.get(internalName);
        if (cached != null) {
            return cached;
        }

        // 从JAR文件中读取
        if (currentJarFile != null) {
            try {
                String entryName = internalName + ".class";
                JarEntry entry = currentJarFile.getJarEntry(entryName);
                if (entry != null) {
                    byte[] bytecode = readBytesFromEntry(currentJarFile, entry);
                    cache.put(internalName, bytecode);
                    return bytecode;
                }
            } catch (Exception e) {
                // 读取失败
            }
        }

        return null;
    }

    /**
     * 缓存字节码
     */
    public static void putBytecode(String internalName, byte[] bytecode) {
        if (internalName != null && bytecode != null) {
            cache.put(internalName, bytecode);
        }
    }

    /**
     * 从JAR条目中读取字节数组
     */
    private static byte[] readBytesFromEntry(JarFile jarFile, JarEntry entry) throws Exception {
        try (InputStream is = jarFile.getInputStream(entry);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
}
