package com.bingbaihanji.code.resurrector.config;

import com.bingbaihanji.code.resurrector.decompiler.DecompilerType;

/**
 * 用户偏好设置
 * <p>
 * 注意：不要直接实例化此类，请通过 ConfigurationManager 获取实例。
 * 所有非静态字段会自动保存，字段名即为配置项ID。
 * 仅支持 String、boolean 和 int 类型字段。
 * 在字段声明中设置默认值。
 */
public class UserPreferences {
    public static final String THEME_XML_PATH = "/org/fife/ui/rsyntaxtextarea/themes/";
    public static final String DEFAULT_THEME_XML = "dark.xml";

    private String themeXml = DEFAULT_THEME_XML;
    private String fileOpenCurrentDirectory = "";
    private String fileSaveCurrentDirectory = "";
    private int font_size = 10;

    private boolean isPackageExplorerStyle = true;
    private boolean isFilterOutInnerClassEntries = true;
    private boolean isSingleClickOpenEnabled = true;
    private boolean isExitByEscEnabled = false;
    private String decompilerEngine = DecompilerType.PROCYON.getName();

    public String getThemeXml() {
        return themeXml;
    }

    public void setThemeXml(String themeXml) {
        this.themeXml = themeXml;
    }

    public String getFileOpenCurrentDirectory() {
        return fileOpenCurrentDirectory;
    }

    public void setFileOpenCurrentDirectory(String fileOpenCurrentDirectory) {
        this.fileOpenCurrentDirectory = fileOpenCurrentDirectory;
    }

    public String getFileSaveCurrentDirectory() {
        return fileSaveCurrentDirectory;
    }

    public void setFileSaveCurrentDirectory(String fileSaveCurrentDirectory) {
        this.fileSaveCurrentDirectory = fileSaveCurrentDirectory;
    }

    public boolean isPackageExplorerStyle() {
        return isPackageExplorerStyle;
    }

    public void setPackageExplorerStyle(boolean isPackageExplorerStyle) {
        this.isPackageExplorerStyle = isPackageExplorerStyle;
    }

    public boolean isFilterOutInnerClassEntries() {
        return isFilterOutInnerClassEntries;
    }

    public void setFilterOutInnerClassEntries(boolean isFilterOutInnerClassEntries) {
        this.isFilterOutInnerClassEntries = isFilterOutInnerClassEntries;
    }

    public boolean isSingleClickOpenEnabled() {
        return isSingleClickOpenEnabled;
    }

    public void setSingleClickOpenEnabled(boolean isSingleClickOpenEnabled) {
        this.isSingleClickOpenEnabled = isSingleClickOpenEnabled;
    }

    public boolean isExitByEscEnabled() {
        return isExitByEscEnabled;
    }

    public void setExitByEscEnabled(boolean isExitByEscEnabled) {
        this.isExitByEscEnabled = isExitByEscEnabled;
    }

    public int getFont_size() {
        return font_size;
    }

    public void setFont_size(int font_size) {
        this.font_size = font_size;
    }

    public String getDecompilerEngine() {
        return decompilerEngine;
    }

    public void setDecompilerEngine(String decompilerEngine) {
        this.decompilerEngine = decompilerEngine;
    }

    public DecompilerType getDecompilerType() {
        return DecompilerType.fromName(decompilerEngine);
    }

    public void setDecompilerType(DecompilerType type) {
        this.decompilerEngine = type.getName();
    }
}
