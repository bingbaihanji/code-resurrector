package com.bingbaihanji.core.resurrector;

import com.bingbaihanji.api.server.JettyServer;
import com.bingbaihanji.core.resurrector.view.MainWindow;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 应用程序启动器，主入口类
 */
public class CodeResurrector {
    private static final Logger log = LoggerFactory.getLogger(CodeResurrector.class);

    private static final AtomicReference<MainWindow> mainWindowRef = new AtomicReference<>();
    private static final List<File> pendingFiles = new ArrayList<>();
    private static ServerSocket lockSocket = null;
    private static JettyServer jettyServer = null;

    private static String socketPort;

    static {
        Properties props = new Properties();
        try {
            InputStream in = CodeResurrector.class.getResourceAsStream("/project.properties");
            props.load(in);
            socketPort = props.getProperty("socket.port");
        } catch (IOException e) {
            log.error("Failed to load properties", e);
        }
    }

    public static void main(final String[] args) {

        // 检查是否是Web服务模式
        boolean webServerMode = checkWebServerMode(args);

        if (webServerMode) {
            // Web服务模式：仅启动Jetty服务器
            startWebServer();
            return;
        }

        // GUI模式：原有逻辑
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (lockSocket != null) {
                        lockSocket.close();
                    }
                    if (jettyServer != null) {
                        jettyServer.stop();
                    }
                } catch (Exception e) {
                    log.error("Failed to stop jetty server", e);
                }
            }
        }));

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.warn("Failed to set system look and feel", e);
        }

        // 用于 TotalCommander 外部查看器设置:
        // javaw -jar "c:\Program Files\Luyten\luyten.jar"
        // (TC 不会在打开 .zip 或 .jar 中的 .class 时抱怨临时文件)
        final File fileFromCommandLine = getFileFromCommandLine(args);

        try {
            launchMainInstance(fileFromCommandLine);
        } catch (Exception e) {
            // 实例已存在，在运行的实例中打开新文件
            try {
                Socket socket = new Socket("localhost", Integer.parseInt(socketPort));
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(args[0]);
                dos.flush();
                dos.close();
                socket.close();
            } catch (IOException ex) {
                log.error("Failed to connect to existing instance", ex);
                showExceptionDialog("无法打开文件", e);
            }
        }
    }

    private static void launchMainInstance(final File fileFromCommandLine) throws IOException {
        lockSocket = new ServerSocket(Integer.parseInt(socketPort));
        launchSession(fileFromCommandLine);
        new Thread(new Runnable() {
            @Override
            public void run() {
                launchServer();
            }
        }).start();
    }

    private static void launchSession(final File fileFromCommandLine) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FlatMacDarkLaf.setup();
                if (!mainWindowRef.compareAndSet(null, new MainWindow(fileFromCommandLine))) {
                    // 已经设置 - 因此添加要打开的文件
                    addToPendingFiles(fileFromCommandLine);
                }
                processPendingFiles();
                mainWindowRef.get().setVisible(true);
            }
        });
    }

    private static void launchServer() {
        try { // 服务器
            while (true) {
                try {
                    Socket socket = lockSocket.accept();
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    addToPendingFiles(getFileFromCommandLine(dis.readUTF()));
                    processPendingFiles();
                    dis.close();
                    socket.close();
                } catch (SocketException e) {
                    // Socket 已关闭，程序正在退出
                    break;
                }
            }
        } catch (IOException e) { // 客户端
            // 忽略关闭时的异常
        }
    }

    // 处理所有待处理文件 - 需要在待处理文件列表上同步
    public static void processPendingFiles() {
        final MainWindow mainWindow = mainWindowRef.get();
        if (mainWindow != null) {
            synchronized (pendingFiles) {
                for (File f : pendingFiles) {
                    mainWindow.loadNewFile(f);
                }
                pendingFiles.clear();
            }
        }
    }

    // 在实例中打开给定文件（如果运行中）- 否则处理这些文件
    public static void addToPendingFiles(File fileToOpen) {
        synchronized (pendingFiles) {
            if (fileToOpen != null) {
                pendingFiles.add(fileToOpen);
            }
        }
    }

    // 如果应用程序正在运行，退出它
    public static void quitInstance() {
        final MainWindow mainWindow = mainWindowRef.get();
        if (mainWindow != null) {
            mainWindow.onExitMenu();
        }
    }

    public static File getFileFromCommandLine(String... args) {
        File fileFromCommandLine = null;
        try {
            if (args.length > 0) {
                String realFileName = new File(args[0]).getCanonicalPath();
                fileFromCommandLine = new File(realFileName);
            }
        } catch (Exception e) {
            log.warn("Error getting canonical path for file", e);
        }
        return fileFromCommandLine;
    }

    public static String getVersion() {
        String result = "";
        try {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("META-INF/maven/us.deathmarine/luyten/pom.properties"))));
            while ((line = br.readLine()) != null) {
                if (line.contains("version"))
                    result = line.split("=")[1];
            }
            br.close();
        } catch (Exception e) {
            return result;
        }
        return result;

    }

    /**
     * 允许用户复制堆栈跟踪以报告任何问题
     * 为鼠标用户添加超链接
     *
     * @param message 错误消息
     * @param e       异常对象
     */
    public static void showExceptionDialog(String message, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stacktrace = sw.toString();
        try {
            sw.close();
            pw.close();
        } catch (IOException e1) {
            log.error("Error closing stream writer", e1);
        }
        System.out.println(stacktrace);

        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        if (message.contains("\n")) {
            for (String s : message.split("\n")) {
                pane.add(new JLabel(s));
            }
        } else {
            pane.add(new JLabel(message));
        }
        pane.add(new JLabel(" \n")); // 空白
        final JTextArea exception = new JTextArea(25, 100);
        exception.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        exception.setText(stacktrace);
        exception.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    new JPopupMenu() {
                        private static final long serialVersionUID = 562054483562666832L;

                        {
                            JMenuItem menuitem = new JMenuItem("全选");
                            menuitem.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    exception.requestFocus();
                                    exception.selectAll();
                                }
                            });
                            this.add(menuitem);
                            menuitem = new JMenuItem("复制");
                            menuitem.addActionListener(new DefaultEditorKit.CopyAction());
                            this.add(menuitem);
                        }
                    }.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        JScrollPane scroll = new JScrollPane(exception);
        scroll.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("堆栈跟踪"),
                new BevelBorder(BevelBorder.LOWERED)));
        pane.add(scroll);
        final String issue = "https://github.com/bingbaihanji/fxgeometric-view/issues";
        final JLabel link = new JLabel("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(issue));
                } catch (Exception e1) {
                    log.error("Failed to open URL in browser: {}", issue, e1);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                link.setText("<HTML>Submit to <FONT color=\"#00aa99\"><U>" + issue + "</U></FONT></HTML>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                link.setText("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
            }
        });
        pane.add(link);
        JOptionPane.showMessageDialog(null, pane, "Error!", JOptionPane.ERROR_MESSAGE);
    }

    public static String getSocketPort() {
        return socketPort;
    }

    /**
     * 检查是否是Web服务模式
     */
    private static boolean checkWebServerMode(String[] args) {
        for (String arg : args) {
            if ("--server".equals(arg) || "-s".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 启动Web服务器模式
     */
    private static void startWebServer() {
        try {
            log.info("Starting Code Resurrector in Web Server mode...");
            jettyServer = new JettyServer();
            jettyServer.start();
            log.info("Web server started successfully on port: {}", jettyServer.getPort());
            log.info("API Endpoints:");
            log.info("  - POST http://localhost:{}/api/decompile/class  (decompile single .class file)", jettyServer.getPort());
            log.info("  - POST http://localhost:{}/api/decompile/jar    (decompile .jar file)", jettyServer.getPort());
            log.info("Press Ctrl+C to stop the server.");
            jettyServer.join();
        } catch (Exception e) {
            log.error("Failed to start web server", e);
            System.exit(1);
        }
    }

    /**
     * 在GUI模式下动态启动Web服务
     */
    public static void startWebServerInBackground() {
        if (jettyServer != null && jettyServer.isRunning()) {
            log.warn("Web server is already running on port: {}", jettyServer.getPort());
            return;
        }

        new Thread(() -> {
            try {
                log.info("Starting Web Server in background...");
                jettyServer = new JettyServer();
                jettyServer.start();
                log.info("Web server started on port: {}", jettyServer.getPort());

                // 在GUI中显示通知
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            mainWindowRef.get(),
                            String.format("Web 服务已启动\n\n" +
                                            "端口: %d\n" +
                                            "API 接口:\n" +
                                            "  • POST http://localhost:%d/api/decompile/class\n" +
                                            "  • POST http://localhost:%d/api/decompile/jar",
                                    jettyServer.getPort(), jettyServer.getPort(), jettyServer.getPort()),
                            "Web 服务启动成功",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });
            } catch (Exception e) {
                log.error("Failed to start web server in background", e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            mainWindowRef.get(),
                            "Web 服务启动失败: " + e.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        }, "WebServerThread").start();
    }

    /**
     * 停止Web服务
     */
    public static void stopWebServer() {
        if (jettyServer == null || !jettyServer.isRunning()) {
            log.warn("Web server is not running");
            return;
        }

        try {
            log.info("Stopping Web Server...");
            jettyServer.stop();
            jettyServer = null;
            log.info("Web server stopped successfully");

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        mainWindowRef.get(),
                        "Web 服务已停止",
                        "Web 服务",
                        JOptionPane.INFORMATION_MESSAGE
                );
            });
        } catch (Exception e) {
            log.error("Failed to stop web server", e);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        mainWindowRef.get(),
                        "Web 服务停止失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
            });
        }
    }

    /**
     * 检查Web服务是否正在运行
     */
    public static boolean isWebServerRunning() {
        return jettyServer != null && jettyServer.isRunning();
    }
}