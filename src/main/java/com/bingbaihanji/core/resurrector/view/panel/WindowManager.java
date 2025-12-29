package com.bingbaihanji.core.resurrector.view.panel;

import com.bingbaihanji.core.resurrector.model.OpenFile;
import com.bingbaihanji.core.resurrector.util.IconResourceManager;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 浮动代码窗口管理器（单例）
 * 负责创建和管理所有浮动代码窗口
 *
 * @author bingbaihanji
 * @date 2025-12-29 14:07:03
 * @description 管理浮动代码窗口的生命周期
 */
public class WindowManager {

    private static WindowManager instance;
    private final List<FloatingCodeWindow> floatingWindows;

    private WindowManager() {
        this.floatingWindows = new ArrayList<>();
    }

    /**
     * 获取单例实例
     */
    public static synchronized WindowManager getInstance() {
        if (instance == null) {
            instance = new WindowManager();
        }
        return instance;
    }

    /**
     * 创建浮动代码窗口
     *
     * @param openFile 要显示的文件
     * @param theme    主题
     * @return 浮动窗口实例
     */
    public FloatingCodeWindow createFloatingCodeWindow(OpenFile openFile, Theme theme) {
        FloatingCodeWindow window = new FloatingCodeWindow(openFile, theme);

        // 添加到管理列表
        floatingWindows.add(window);

        // 设置窗口关闭时自动从列表移除
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                floatingWindows.remove(window);
            }
        });

        return window;
    }

    /**
     * 关闭所有浮动窗口
     */
    public void closeAllFloatingWindows() {
        // 创建副本避免并发修改
        List<FloatingCodeWindow> windowsCopy = new ArrayList<>(floatingWindows);
        for (FloatingCodeWindow window : windowsCopy) {
            window.dispose();
        }
        floatingWindows.clear();
    }

    /**
     * 获取当前打开的浮动窗口数量
     */
    public int getFloatingWindowCount() {
        return floatingWindows.size();
    }

    /**
     * 浮动代码窗口
     */
    public static class FloatingCodeWindow extends JFrame {
        private final OpenFile openFile;
        private final RTextScrollPane scrollPane;

        public FloatingCodeWindow(OpenFile openFile, Theme theme) {
            this.openFile = openFile;
            this.scrollPane = openFile.scrollPane;

            // 设置窗口标题
            setTitle(openFile.name);

            // 设置窗口图标
            setIconImage(IconResourceManager.loadWindowIcon().getImage());

            // 设置窗口大小和位置
            setSize(900, 700);
            setLocationRelativeTo(null); // 居中显示

            // 设置布局
            setLayout(new BorderLayout());
            add(scrollPane, BorderLayout.CENTER);

            // 应用主题
            if (theme != null) {
                theme.apply(openFile.textArea);
            }

            // 设置默认关闭操作
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }

        /**
         * 获取关联的 OpenFile
         */
        public OpenFile getOpenFile() {
            return openFile;
        }

        /**
         * 获取滚动面板
         */
        public RTextScrollPane getScrollPane() {
            return scrollPane;
        }
    }
}
