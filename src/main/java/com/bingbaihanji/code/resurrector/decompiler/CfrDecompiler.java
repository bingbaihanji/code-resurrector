package com.bingbaihanji.code.resurrector.decompiler;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

import java.io.IOException;
import java.util.*;

/**
 * CFR反编译引擎实现
 */
public class CfrDecompiler implements IDecompiler {

    private Map<String, String> options;

    public CfrDecompiler() {
        initialize();
    }

    @Override
    public void initialize() {
        options = new HashMap<>();
        // CFR配置选项 - 优化lambda和泛型支持
        options.put("showversion", "false");
        options.put("hideutf", "false");
        options.put("innerclasses", "true");
        options.put("decodelambdas", "true");           // 解码lambda表达式
        options.put("decodestringswitch", "true");      // 解码switch字符串
        options.put("decodeenumswitch", "true");        // 解码switch枚举
        options.put("sugarenums", "true");              // 枚举糖衣
        options.put("decodefinally", "true");           // 解码finally块
        options.put("removebadgenerics", "true");       // 移除错误泛型
        options.put("sugarasserts", "true");            // assert糖衣
        options.put("sugarboxing", "true");             // 自动装箱糖衣
        options.put("showops", "false");                // 不显示操作码
        options.put("silent", "true");                  // 静默模式
        options.put("recover", "true");                 // 恢复模式
        options.put("eclipse", "true");                 // Eclipse兼容
        options.put("override", "true");                // 显示@Override
        options.put("showinferrable", "true");          // 显示可推断泛型
        options.put("stringbuilder", "true");           // StringBuilder优化
        options.put("stringconcat", "true");            // 字符串连接优化
        options.put("tryresources", "true");            // try-with-resources
        options.put("recordtypes", "true");             // record类型支持
        options.put("sealedclasses", "true");           // sealed class支持
        options.put("switchexpression", "true");        // switch表达式支持
        options.put("instanceofpattern", "true");       // instanceof模式匹配
    }

    @Override
    public String decompile(String classFilePath, byte[] classBytes) throws Exception {
        String internalName = classFilePath.replace(".class", "");
        return decompileType(internalName, classBytes);
    }

    @Override
    public String decompileType(String typeName, byte[] classBytes) throws Exception {
        final StringBuilder result = new StringBuilder();
        final byte[] bytecode = classBytes;

        // 创建自定义ClassFileSource
        ClassFileSource classFileSource = new ClassFileSource() {
            @Override
            public void informAnalysisRelativePathDetail(String usePath, String specPath) {
            }

            @Override
            public Collection<String> addJar(String jarPath) {
                return Collections.emptyList();
            }

            @Override
            public String getPossiblyRenamedPath(String path) {
                return path;
            }

            @Override
            public Pair<byte[], String> getClassFileContent(String path) throws IOException {
                String normalizedPath = path.replace("\\", "/");
                String normalizedTypeName = typeName.replace("\\", "/");

                // 匹配请求的类名
                if (normalizedPath.equals(normalizedTypeName) ||
                        normalizedPath.equals(normalizedTypeName + ".class") ||
                        normalizedPath.endsWith("/" + getSimpleName(normalizedTypeName) + ".class") ||
                        normalizedPath.endsWith("/" + getSimpleName(normalizedTypeName))) {
                    return Pair.make(bytecode, normalizedPath);
                }

                // 尝试从缓存获取其他类
                String internalName = normalizedPath.replace(".class", "");
                byte[] otherBytes = BytecodeCache.getBytecode(internalName);
                if (otherBytes != null) {
                    return Pair.make(otherBytes, normalizedPath);
                }

                return null;
            }

            private String getSimpleName(String name) {
                int idx = name.lastIndexOf('/');
                return idx >= 0 ? name.substring(idx + 1) : name;
            }
        };

        // 创建输出接收器 - 修复：正确处理SinkReturns.Decompiled
        OutputSinkFactory outputSinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                if (sinkType == SinkType.JAVA) {
                    return Collections.singletonList(SinkClass.DECOMPILED);
                }
                return Collections.emptyList();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
                    return (Sink<T>) (OutputSinkFactory.Sink<SinkReturns.Decompiled>) decompiled -> {
                        // 从SinkReturns.Decompiled对象获取Java代码
                        result.append(decompiled.getJava());
                    };
                }
                return ignore -> {
                };
            }
        };

        // 执行反编译
        CfrDriver driver = new CfrDriver.Builder()
                .withClassFileSource(classFileSource)
                .withOutputSink(outputSinkFactory)
                .withOptions(options)
                .build();

        driver.analyse(Collections.singletonList(typeName + ".class"));

        String decompiled = result.toString();
        if (decompiled.isEmpty()) {
            return "// CFR 反编译失败\n// 类名: " + typeName + "\n// 请检查字节码是否有效";
        }
        return decompiled;
    }

    @Override
    public DecompilerType getType() {
        return DecompilerType.CFR;
    }

    @Override
    public void cleanup() {
        // CFR不需要特殊清理
    }
}
