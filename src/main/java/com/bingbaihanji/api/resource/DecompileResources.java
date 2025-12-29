package com.bingbaihanji.api.resource;

import com.bingbaihanji.api.model.DecompilerType;
import com.bingbaihanji.api.service.DecompileService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 反编译 REST API
 *
 * <p>
 * 提供 Class / Jar 的反编译能力：
 * <ul>
 *     <li>Class → Java 源码文本</li>
 *     <li>Jar → Zip(Java 源码)</li>
 * </ul>
 * </p>
 *
 * @author bingbaihanji
 * @date 2025-12-29
 */
@Path("/decompile")
public class DecompileResources {

    private static final Logger log = LoggerFactory.getLogger(DecompileResources.class);
    private final DecompileService decompileService = new DecompileService();

    /**
     * 反编译单个 class 文件
     *
     * @param classInputStream 上传的 class 文件流
     * @param fileMeta         文件元信息
     * @param typeStr          反编译引擎类型(可选，默认PROCYON)
     * @return Java 源码文本
     */
    @POST
    @Path("/class")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response decompileClassFile(
            @FormDataParam("file") InputStream classInputStream,
            @FormDataParam("file") FormDataContentDisposition fileMeta,
            @FormDataParam("type") @DefaultValue("PROCYON") String typeStr
    ) {
        try {
            if (classInputStream == null || fileMeta == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("File is required")
                        .build();
            }

            String fileName = fileMeta.getFileName();
            if (fileName == null || !fileName.endsWith(".class")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Only .class files are supported")
                        .build();
            }

            // 读取class文件字节
            byte[] classBytes = classInputStream.readAllBytes();

            // 解析反编译引擎类型
            DecompilerType type = parseDecompilerType(typeStr);

            // 执行反编译
            String javaSource = decompileService.decompileClass(classBytes, fileName, type);

            return Response.ok(javaSource, MediaType.TEXT_PLAIN)
                    .header("Content-Disposition", "attachment; filename=\"" +
                            fileName.replace(".class", ".java") + "\"")
                    .build();

        } catch (Exception e) {
            log.error("Failed to decompile class file", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Decompilation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 反编译 jar 包
     *
     * @param jarInputStream 上传的 jar 文件流
     * @param fileMeta       文件元信息
     * @param typeStr        反编译引擎类型(可选，默认PROCYON)
     * @return zip 压缩包响应（Java 源码）
     */
    @POST
    @Path("/jar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/zip")
    public Response decompileJarFile(
            @FormDataParam("file") InputStream jarInputStream,
            @FormDataParam("file") FormDataContentDisposition fileMeta,
            @FormDataParam("type") @DefaultValue("PROCYON") String typeStr
    ) {
        try {
            if (jarInputStream == null || fileMeta == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("File is required")
                        .build();
            }

            String fileName = fileMeta.getFileName();
            if (fileName == null || !fileName.endsWith(".jar")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Only .jar files are supported")
                        .build();
            }

            // 解析反编译引擎类型
            DecompilerType type = parseDecompilerType(typeStr);

            // 执行反编译
            byte[] zipBytes = decompileService.decompileJar(jarInputStream, type);

            // 生成输出文件名
            String outputFileName = fileName.replace(".jar", "-sources.zip");

            return Response.ok(new ByteArrayInputStream(zipBytes), "application/zip")
                    .header("Content-Disposition", "attachment; filename=\"" + outputFileName + "\"")
                    .build();

        } catch (Exception e) {
            log.error("Failed to decompile JAR file", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Decompilation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析反编译引擎类型
     */
    private DecompilerType parseDecompilerType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return DecompilerType.PROCYON;
        }
        try {
            return DecompilerType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid decompiler type: {}, using PROCYON", typeStr);
            return DecompilerType.PROCYON;
        }
    }
}
