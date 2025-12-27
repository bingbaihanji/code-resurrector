package com.bingbaihanji.code.resurrector.util;

import javax.swing.*;
import java.awt.*;

/**
 * 图标资源管理器 - 统一管理应用中所有图标的大小和加载
 */
public class IconResourceManager {
    // 定义图标大小常量
    public static final int SIZE_TREE_ICON = 16;      // 树形菜单图标大小
    public static final int SIZE_CLOSE_BUTTON = 16;   // 关闭按钮图标大小
    public static final int SIZE_WINDOW_ICON = 32;    // 窗口图标大小

    /**
     * 加载并缩放图标
     *
     * @param resourcePath 资源路径
     * @param size         目标大小
     * @return 缩放后的 ImageIcon
     */
    public static ImageIcon loadIcon(String resourcePath, int size) {
        try {
            Image image = Toolkit.getDefaultToolkit().getImage(IconResourceManager.class.getResource(resourcePath));
            if (image != null && size > 0) {
                Image scaledImage = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + resourcePath);
        }
        return null;
    }

    /**
     * 加载并缩放图标（指定宽高）
     *
     * @param resourcePath 资源路径
     * @param width        目标宽度
     * @param height       目标高度
     * @return 缩放后的 ImageIcon
     */
    public static ImageIcon loadIcon(String resourcePath, int width, int height) {
        try {
            Image image = Toolkit.getDefaultToolkit().getImage(IconResourceManager.class.getResource(resourcePath));
            if (image != null && width > 0 && height > 0) {
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + resourcePath);
        }
        return null;
    }

    /**
     * 加载树形图标（包、Java文件、YAML文件、普通文件）
     */
    public static ImageIcon loadPackageIcon() {
        return loadIcon("/resources/package_obj.png", SIZE_TREE_ICON);
    }

    public static ImageIcon loadJavaIcon() {
        return loadIcon("/resources/java.png", 22);
    }

    public static ImageIcon loadYmlIcon() {
        return loadIcon("/resources/yml.png", SIZE_TREE_ICON);
    }

    public static ImageIcon loadFileIcon() {
        return loadIcon("/resources/file.png", SIZE_TREE_ICON);
    }

    /**
     * 加载关闭按钮图标
     */
    public static ImageIcon loadCloseIcon() {
        return loadIcon("/resources/icon_close.png", SIZE_CLOSE_BUTTON);
    }

    /**
     * 加载窗口图标
     */
    public static ImageIcon loadWindowIcon() {
        return loadIcon("/resources/coding.png", SIZE_WINDOW_ICON);
    }
}
