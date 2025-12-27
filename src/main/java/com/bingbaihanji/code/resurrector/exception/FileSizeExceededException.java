package com.bingbaihanji.code.resurrector.exception;

import java.text.DecimalFormat;

/**
 * 文件大小超限异常
 * 当文件大小超过允许的最大值时抛出此异常
 */
public class FileSizeExceededException extends Exception {
    private static final long serialVersionUID = 6091096838075139962L;
    private final long size;

    public FileSizeExceededException(long size) {
        this.size = size;
    }

    /**
     * 获取可读的文件大小格式
     *
     * @return 格式化后的文件大小字符串，如 "1.5 GB"
     */
    public String getReadableFileSize() {
        if (size <= 0)
            return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
