package com.bingbaihanji.code.resurrector.decompiler;

/**
 * 反编译引擎类型枚举
 */
public enum DecompilerType {
    PROCYON("Procyon", "基于Procyon的反编译引擎"),
    CFR("CFR", "基于CFR的反编译引擎"),
    VINEFLOWER("Vineflower", "基于Vineflower的反编译引擎");

    private final String name;
    private final String description;

    DecompilerType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static DecompilerType fromName(String name) {
        for (DecompilerType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return PROCYON; // 默认返回Procyon
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
