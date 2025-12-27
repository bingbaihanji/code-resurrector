package com.bingbaihanji.code.resurrector.core;

import com.bingbaihanji.code.resurrector.util.SystemInfoDetector;

import java.awt.event.InputEvent;

public final class Keymap {
    /**
     * 在 macOS 中，Ctrl+click 默认为"上下文菜单"，因此在那里使用 META+click。
     *
     * @return 对于 macOS 返回 META_DOWN_MASK，否则返回 CTRL_DOWN_MASK
     */
    public static int ctrlDownModifier() {
        return SystemInfoDetector.IS_MAC ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
    }
}
