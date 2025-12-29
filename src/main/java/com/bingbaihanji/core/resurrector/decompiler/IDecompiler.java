package com.bingbaihanji.core.resurrector.decompiler;

/**
 * 反编译引擎接口
 */
public interface IDecompiler {

    /**
     * 反编译class文件
     *
     * @param classFilePath class文件路径
     * @param classBytes    class文件字节数组
     * @return 反编译后的Java源代码
     * @throws Exception 反编译失败时抛出异常
     */
    String decompile(String classFilePath, byte[] classBytes) throws Exception;

    /**
     * 反编译指定的类型
     *
     * @param typeName   完全限定类名 (例如: java/lang/String)
     * @param classBytes class文件字节数组
     * @return 反编译后的Java源代码
     * @throws Exception 反编译失败时抛出异常
     */
    String decompileType(String typeName, byte[] classBytes) throws Exception;

    /**
     * 获取引擎类型
     *
     * @return 反编译引擎类型
     */
    DecompilerType getType();

    /**
     * 获取引擎名称
     *
     * @return 引擎名称
     */
    default String getName() {
        return getType().getName();
    }

    /**
     * 初始化引擎
     */
    void initialize();

    /**
     * 清理资源
     */
    void cleanup();
}
