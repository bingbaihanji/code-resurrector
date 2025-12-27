package com.bingbaihanji.code.resurrector.decompiler;

import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

/**
 * Vineflower反编译引擎实现
 * Vineflower是FernFlower的现代化分支，支持更高版本Java特性
 */
public class VineflowerDecompiler implements IDecompiler {

    private Map<String, Object> options;

    public VineflowerDecompiler() {
        initialize();
    }

    @Override
    public void initialize() {
        options = new HashMap<>();
        // Vineflower高级选项配置 - 优化lambda和泛型支持
        options.put("den", "1");  // decompile enumerations
        options.put("dgs", "1");  // decompile generic signatures
        options.put("din", "1");  // decompile inner classes
        options.put("rbr", "1");  // remove bridge methods
        options.put("rsy", "1");  // remove synthetic methods
        options.put("bto", "1");  // interpret int 1 as boolean true
        options.put("nns", "1");  // hide not set synthetic
        options.put("uto", "0");  // treat unknown as Object - disabled for better type inference
        options.put("udv", "1");  // use debug var names
        options.put("ump", "1");  // use method parameters
        options.put("fdi", "1");  // decompile finally
        options.put("asc", "0");  // ASCII string encoding - disabled for UTF-8
        options.put("rer", "1");  // remove empty ranges
        options.put("rgn", "1");  // remove getClass() for lambda
        options.put("lit", "1");  // output numeric literals
        options.put("bsm", "1");  // use lambdas (not anonymous classes)
        options.put("mpm", "60"); // max processing method time (60s)
        options.put("lac", "0");  // DO NOT decompile lambda to anonymous classes
        options.put("nls", "1");  // new line separator
        options.put("ind", "    "); // indentation
        options.put("log", "WARN"); // log level
        options.put("pll", "130"); // preferred line length
        // Java 16+ 特性
        options.put("rec", "1");  // decompile records
        options.put("sea", "1");  // decompile sealed classes
        options.put("pam", "1");  // decompile pattern matching
        options.put("swi", "1");  // decompile switch expressions
        options.put("vac", "1");  // decompile var in catch
    }

    @Override
    public String decompile(String classFilePath, byte[] classBytes) throws Exception {
        String internalName = classFilePath.replace(".class", "");
        return decompileType(internalName, classBytes);
    }

    @Override
    public String decompileType(String typeName, byte[] classBytes) throws Exception {
        final StringBuilder result = new StringBuilder();
        final Map<String, byte[]> classData = new HashMap<>();
        classData.put(typeName, classBytes);

        // 创建临时文件用于反编译
        File tempDir = Files.createTempDirectory("vineflower_").toFile();
        File tempClassFile = null;

        try {
            // 写入class文件到临时目录
            String classFileName = getSimpleName(typeName) + ".class";
            tempClassFile = new File(tempDir, classFileName);
            Files.write(tempClassFile.toPath(), classBytes);

            // 创建字节码提供器
            final File finalTempClassFile = tempClassFile;
            IBytecodeProvider bytecodeProvider = new IBytecodeProvider() {
                @Override
                public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
                    // 优先检查主类
                    if (externalPath != null && externalPath.equals(finalTempClassFile.getAbsolutePath())) {
                        return classBytes;
                    }

                    String key = internalPath != null ? internalPath : externalPath;
                    if (key != null) {
                        key = key.replace(".class", "").replace("\\", "/");
                        if (key.endsWith("/" + getSimpleName(typeName)) || key.equals(typeName)) {
                            return classBytes;
                        }
                        // 尝试从缓存获取
                        byte[] cached = BytecodeCache.getBytecode(key);
                        if (cached != null) {
                            return cached;
                        }
                    }
                    return null;
                }
            };

            // 创建结果保存器
            IResultSaver resultSaver = new IResultSaver() {
                @Override
                public void saveFolder(String path) {
                }

                @Override
                public void copyFile(String source, String path, String entryName) {
                }

                @Override
                public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
                    if (content != null && !content.isEmpty()) {
                        result.append(content);
                    }
                }

                @Override
                public void createArchive(String path, String archiveName, Manifest manifest) {
                }

                @Override
                public void saveDirEntry(String path, String archiveName, String entryName) {
                }

                @Override
                public void copyEntry(String source, String path, String archiveName, String entry) {
                }

                @Override
                public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
                    if (content != null && !content.isEmpty()) {
                        result.append(content);
                    }
                }

                @Override
                public void closeArchive(String path, String archiveName) {
                }
            };

            // 创建日志记录器
            IFernflowerLogger logger = new IFernflowerLogger() {
                @Override
                public void writeMessage(String message, Severity severity) {
                    // 静默模式
                }

                @Override
                public void writeMessage(String message, Severity severity, Throwable t) {
                    // 静默模式
                }
            };

            // 使用BaseDecompiler进行反编译
            BaseDecompiler decompiler = new BaseDecompiler(bytecodeProvider, resultSaver, options, logger);

            // 添加要反编译的文件
            decompiler.addSource(tempClassFile);

            // 执行反编译
            decompiler.decompileContext();

        } finally {
            // 清理临时文件
            if (tempClassFile != null && tempClassFile.exists()) {
                tempClassFile.delete();
            }
            if (tempDir != null && tempDir.exists()) {
                tempDir.delete();
            }
        }

        String decompiled = result.toString();
        if (decompiled.isEmpty()) {
            return "// Vineflower 反编译失败\n// 类名: " + typeName + "\n// 请检查字节码是否有效";
        }
        return decompiled;
    }

    private String getSimpleName(String typeName) {
        int idx = typeName.lastIndexOf('/');
        return idx >= 0 ? typeName.substring(idx + 1) : typeName;
    }

    @Override
    public DecompilerType getType() {
        return DecompilerType.VINEFLOWER;
    }

    @Override
    public void cleanup() {
        // 无需特殊清理
    }
}
