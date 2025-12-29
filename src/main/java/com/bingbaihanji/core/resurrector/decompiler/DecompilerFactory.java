package com.bingbaihanji.core.resurrector.decompiler;

import java.util.HashMap;
import java.util.Map;

/**
 * 反编译引擎工厂类
 */
public class DecompilerFactory {

    private static final Map<DecompilerType, IDecompiler> decompilerCache = new HashMap<>();

    /**
     * 获取指定类型的反编译引擎实例
     *
     * @param type 反编译引擎类型
     * @return 反编译引擎实例
     */
    public static synchronized IDecompiler getDecompiler(DecompilerType type) {
        if (type == null) {
            type = DecompilerType.PROCYON;
        }

        IDecompiler decompiler = decompilerCache.get(type);
        if (decompiler == null) {
            decompiler = createDecompiler(type);
            decompilerCache.put(type, decompiler);
        }
        return decompiler;
    }

    /**
     * 创建新的反编译引擎实例
     *
     * @param type 反编译引擎类型
     * @return 反编译引擎实例
     */
    private static IDecompiler createDecompiler(DecompilerType type) {
        switch (type) {
            case CFR:
                return new CfrDecompiler();
            case VINEFLOWER:
                return new VineflowerDecompiler();
            case PROCYON:
            default:
                return new ProcyonDecompiler();
        }
    }

    /**
     * 清理所有反编译引擎缓存
     */
    public static synchronized void cleanup() {
        for (IDecompiler decompiler : decompilerCache.values()) {
            try {
                decompiler.cleanup();
            } catch (Exception e) {
                // 忽略清理异常
            }
        }
        decompilerCache.clear();
    }

    /**
     * 清理指定类型的反编译引擎
     *
     * @param type 反编译引擎类型
     */
    public static synchronized void cleanup(DecompilerType type) {
        IDecompiler decompiler = decompilerCache.remove(type);
        if (decompiler != null) {
            try {
                decompiler.cleanup();
            } catch (Exception e) {
                // 忽略清理异常
            }
        }
    }
}
