package com.bingbaihanji.api.service;

import com.bingbaihanji.api.model.DecompilerType;
import com.bingbaihanji.core.resurrector.decompiler.DecompilerFactory;
import com.bingbaihanji.core.resurrector.decompiler.IDecompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 反编译服务
 *
 * @author bingbaihanji
 * @date 2025-12-29
 */
public class DecompileService {

    private static final Logger log = LoggerFactory.getLogger(DecompileService.class);

    /**
     * 反编译单个class文件
     *
     * @param classBytes 类字节码
     * @param fileName   文件名
     * @param type       反编译引擎类型
     * @return Java源代码
     */
    public String decompileClass(byte[] classBytes, String fileName, DecompilerType type) throws Exception {
        if (classBytes == null || classBytes.length == 0) {
            throw new IllegalArgumentException("Class bytes cannot be null or empty");
        }

        // 转换API的DecompilerType到core的DecompilerType
        com.bingbaihanji.core.resurrector.decompiler.DecompilerType coreType = convertType(type);

        IDecompiler decompiler = DecompilerFactory.getDecompiler(coreType);

        // 从文件名提取类名
        String className = extractClassName(fileName);

        log.info("Decompiling class: {} using {}", className, coreType.getName());

        return decompiler.decompile(className, classBytes);
    }

    /**
     * 反编译JAR文件，返回包含所有反编译后Java源文件的ZIP字节数组
     *
     * @param jarInputStream JAR文件输入流
     * @param type           反编译引擎类型
     * @return ZIP文件字节数组
     */
    public byte[] decompileJar(InputStream jarInputStream, DecompilerType type) throws Exception {
        if (jarInputStream == null) {
            throw new IllegalArgumentException("JAR input stream cannot be null");
        }

        // 转换API的DecompilerType到core的DecompilerType
        com.bingbaihanji.core.resurrector.decompiler.DecompilerType coreType = convertType(type);

        IDecompiler decompiler = DecompilerFactory.getDecompiler(coreType);

        // 保存JAR到临时文件
        File tempJarFile = File.createTempFile("decompile_", ".jar");
        tempJarFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempJarFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = jarInputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        log.info("Decompiling JAR file using {}", coreType.getName());

        // 反编译JAR中的所有class文件
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos);
             JarFile jarFile = new JarFile(tempJarFile)) {

            Enumeration<JarEntry> entries = jarFile.entries();
            int classCount = 0;
            int successCount = 0;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.endsWith(".class")) {
                    classCount++;
                    try {
                        // 读取class文件内容
                        byte[] classBytes = readEntryBytes(jarFile, entry);

                        // 反编译
                        String className = entryName.replace(".class", "");
                        String javaSource = decompiler.decompile(className, classBytes);

                        // 写入ZIP
                        String javaFileName = entryName.replace(".class", ".java");
                        ZipEntry zipEntry = new ZipEntry(javaFileName);
                        zos.putNextEntry(zipEntry);
                        zos.write(javaSource.getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();

                        successCount++;
                    } catch (Exception e) {
                        log.warn("Failed to decompile: {}", entryName, e);
                        // 继续处理其他文件
                    }
                }
            }

            log.info("Decompiled {}/{} classes from JAR", successCount, classCount);
        } finally {
            // 删除临时文件
            tempJarFile.delete();
        }

        return baos.toByteArray();
    }

    /**
     * 从JAR条目读取字节数组
     */
    private byte[] readEntryBytes(JarFile jarFile, JarEntry entry) throws IOException {
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

    /**
     * 从文件名提取类名
     */
    private String extractClassName(String fileName) {
        if (fileName == null) {
            return "Unknown";
        }
        // 移除.class扩展名
        if (fileName.endsWith(".class")) {
            fileName = fileName.substring(0, fileName.length() - 6);
        }
        return fileName;
    }

    /**
     * 转换API的DecompilerType到core的DecompilerType
     */
    private com.bingbaihanji.core.resurrector.decompiler.DecompilerType convertType(DecompilerType apiType) {
        if (apiType == null) {
            return com.bingbaihanji.core.resurrector.decompiler.DecompilerType.PROCYON;
        }
        return switch (apiType) {
            case CFR -> com.bingbaihanji.core.resurrector.decompiler.DecompilerType.CFR;
            case VINEFLOWER -> com.bingbaihanji.core.resurrector.decompiler.DecompilerType.VINEFLOWER;
            default -> com.bingbaihanji.core.resurrector.decompiler.DecompilerType.PROCYON;
        };
    }
}
