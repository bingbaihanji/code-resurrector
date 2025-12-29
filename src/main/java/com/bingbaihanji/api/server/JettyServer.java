package com.bingbaihanji.api.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Jetty Web服务器
 *
 * @author bingbaihanji
 * @date 2025-12-29
 */
public class JettyServer {

    private static final Logger log = LoggerFactory.getLogger(JettyServer.class);

    private Server server;
    private int port;

    public JettyServer() {
        this.port = loadServerPort();
    }

    public JettyServer(int port) {
        this.port = port;
    }

    /**
     * 从配置文件加载服务器端口
     */
    private int loadServerPort() {
        Properties props = new Properties();
        try (InputStream in = JettyServer.class.getResourceAsStream("/project.properties")) {
            if (in != null) {
                props.load(in);
                String serverPort = props.getProperty("server.port", "8080");
                return Integer.parseInt(serverPort);
            }
        } catch (Exception e) {
            log.warn("Failed to load server port from properties, using default 8080", e);
        }
        return 8080;
    }

    /**
     * 启动Jetty服务器
     */
    public void start() throws Exception {
        server = new Server(port);

        // 配置Jersey
        ResourceConfig config = new ResourceConfig();
        config.packages("com.bingbaihanji.api.resource");
        config.register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);

        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(servlet, "/api/*");

        server.setHandler(context);

        server.start();
        log.info("Jetty server started on port: {}", port);
        log.info("API endpoint: http://localhost:{}/api/decompile", port);
    }

    /**
     * 停止Jetty服务器
     */
    public void stop() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop();
            log.info("Jetty server stopped");
        }
    }

    /**
     * 等待服务器结束
     */
    public void join() throws InterruptedException {
        if (server != null) {
            server.join();
        }
    }

    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return server != null && server.isRunning();
    }

    public int getPort() {
        return port;
    }
}
