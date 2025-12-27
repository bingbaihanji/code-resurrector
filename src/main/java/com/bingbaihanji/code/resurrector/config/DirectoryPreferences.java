package com.bingbaihanji.code.resurrector.config;

import javax.swing.*;
import java.io.File;

/**
 * 目录偏好设置
 * 用于保存和恢复文件对话框的当前目录
 */
public class DirectoryPreferences {
    private final UserPreferences userPrefs;

    public DirectoryPreferences(UserPreferences userPrefs) {
        this.userPrefs = userPrefs;
    }

    /**
     * 恢复打开对话框的目录
     *
     * @param fc 文件选择器
     */
    public void retrieveOpenDialogDir(JFileChooser fc) {
        try {
            String currentDirStr = userPrefs.getFileOpenCurrentDirectory();
            if (currentDirStr != null && !currentDirStr.trim().isEmpty()) {
                File currentDir = new File(currentDirStr);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    fc.setCurrentDirectory(currentDir);
                }
            }
        } catch (Exception e) {
            showError("Exception!", e);
        }
    }

    /**
     * 保存打开对话框的目录
     *
     * @param fc 文件选择器
     */
    public void saveOpenDialogDir(JFileChooser fc) {
        try {
            File currentDir = fc.getCurrentDirectory();
            if (currentDir != null && currentDir.exists() && currentDir.isDirectory()) {
                userPrefs.setFileOpenCurrentDirectory(currentDir.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("Exception!", e);
        }
    }

    /**
     * 恢复保存对话框的目录
     *
     * @param fc 文件选择器
     */
    public void retrieveSaveDialogDir(JFileChooser fc) {
        try {
            String currentDirStr = userPrefs.getFileSaveCurrentDirectory();
            if (currentDirStr != null && !currentDirStr.trim().isEmpty()) {
                File currentDir = new File(currentDirStr);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    fc.setCurrentDirectory(currentDir);
                }
            }
        } catch (Exception e) {
            showError("Exception!", e);
        }
    }

    /**
     * 保存保存对话框的目录
     *
     * @param fc 文件选择器
     */
    public void saveSaveDialogDir(JFileChooser fc) {
        try {
            File currentDir = fc.getCurrentDirectory();
            if (currentDir != null && currentDir.exists() && currentDir.isDirectory()) {
                userPrefs.setFileSaveCurrentDirectory(currentDir.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("Exception!", e);
        }
    }

    /**
     * 显示错误信息
     */
    private void showError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }
}
