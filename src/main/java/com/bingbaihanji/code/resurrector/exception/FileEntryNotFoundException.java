package com.bingbaihanji.code.resurrector.exception;

import java.io.Serial;

/**
 * 文件条目未找到异常
 * 当在JAR或ZIP文件中找不到指定的条目时抛出此异常
 */
public class FileEntryNotFoundException extends Exception {
    @Serial
    private static final long serialVersionUID = -1019729947179642460L;

}
