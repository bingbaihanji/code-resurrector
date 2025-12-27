package com.bingbaihanji.code.resurrector.config;

import javax.swing.*;
import java.awt.*;

/**
 * 窗口位置配置类
 * 用于保存和恢复窗口的位置和大小信息
 */
public class WindowPositionConfig {

    private boolean isFullScreen;
    private int windowWidth;
    private int windowHeight;
    private int windowX;
    private int windowY;

    /**
     * 从 JFrame 窗口读取位置信息
     *
     * @param window 要读取位置的窗口
     */
    public void readPositionFromWindow(JFrame window) {
        isFullScreen = (window.getExtendedState() == JFrame.MAXIMIZED_BOTH);
        if (!isFullScreen) {
            this.readPositionFromComponent(window);
        }
    }

    /**
     * 从 JDialog 对话框读取位置信息
     *
     * @param dialog 要读取位置的对话框
     */
    public void readPositionFromDialog(JDialog dialog) {
        this.readPositionFromComponent(dialog);
    }

    /**
     * 从组件读取位置信息
     *
     * @param component 要读取位置的组件
     */
    private void readPositionFromComponent(Component component) {
        isFullScreen = false;
        windowWidth = component.getWidth();
        windowHeight = component.getHeight();
        windowX = component.getX();
        windowY = component.getY();
    }

    /**
     * 检查保存的窗口位置是否有效
     *
     * @return 位置是否有效
     */
    public boolean isSavedWindowPositionValid() {
        if (isFullScreen) {
            return true;
        }
        if (windowWidth < 100 || windowHeight < 100) {
            return false;
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (windowWidth > screenSize.width + 50 || windowHeight > screenSize.height + 50) {
            return false;
        }
        if (windowY < -20 || windowY > screenSize.height - 50 || windowX < 50 - windowWidth
                || windowX > screenSize.width - 50) {
            return false;
        }
        return true;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void setFullScreen(boolean isFullScreen) {
        this.isFullScreen = isFullScreen;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public int getWindowX() {
        return windowX;
    }

    public void setWindowX(int windowX) {
        this.windowX = windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public void setWindowY(int windowY) {
        this.windowY = windowY;
    }
}
