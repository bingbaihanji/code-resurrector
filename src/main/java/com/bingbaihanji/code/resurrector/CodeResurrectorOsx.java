package com.bingbaihanji.code.resurrector;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

import java.io.File;

/**
 * An OS X-specific initialization method for dragging/dropping
 */
public class CodeResurrectorOsx extends CodeResurrector {
    public static void main(String[] args) {
        // Add an adapter as the handler to a new instance of the application
        // class
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

        // Call the superclass's main function
        CodeResurrector.main(args);
    }
}
