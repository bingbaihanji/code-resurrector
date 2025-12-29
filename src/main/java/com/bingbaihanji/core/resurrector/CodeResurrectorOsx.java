package com.bingbaihanji.core.resurrector;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

import java.io.File;

/**
 * An OS X-specific initialization method for dragging/dropping
 */
public class CodeResurrectorOsx extends CodeResurrector {
    public static void main(String[] args) {
        // 为应用程序类的新实例添加适配器作为处理器
        // 类
        @SuppressWarnings("deprecation")
        Application app = new Application();
        app.addApplicationListener(new ApplicationAdapter() {
            public void handleOpenFile(ApplicationEvent e) {
                CodeResurrector.addToPendingFiles(new File(e.getFilename()));
                CodeResurrector.processPendingFiles();
            }

            public void handleQuit(ApplicationEvent e) {
                CodeResurrector.quitInstance();
            }
        });

        // 调用父类的主函数
        CodeResurrector.main(args);
    }
}
