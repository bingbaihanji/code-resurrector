package com.bingbaihanji.code.resurrector.config;

import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.prefs.Preferences;

/**
 * 配置管理器
 * 负责加载和保存应用程序的配置信息，包括反编译器设置、窗口位置、用户偏好等
 */
public class ConfigurationManager {

    private static final String FLATTEN_SWITCH_BLOCKS_ID = "flattenSwitchBlocks";
    private static final String FORCE_EXPLICIT_IMPORTS_ID = "forceExplicitImports";
    private static final String SHOW_SYNTHETIC_MEMBERS_ID = "showSyntheticMembers";
    private static final String EXCLUDE_NESTED_TYPES_ID = "excludeNestedTypes";
    private static final String FORCE_EXPLICIT_TYPE_ARGUMENTS_ID = "forceExplicitTypeArguments";
    private static final String RETAIN_REDUNDANT_CASTS_ID = "retainRedundantCasts";
    private static final String INCLUDE_ERROR_DIAGNOSTICS_ID = "includeErrorDiagnostics";
    private static final String UNICODE_REPLACE_ENABLED_ID = "unicodeReplaceEnabled";
    private static final String LANGUAGE_NAME_ID = "languageName";

    private static final String MAIN_WINDOW_ID_PREFIX = "main";
    private static final String FIND_WINDOW_ID_PREFIX = "find";
    private static final String WINDOW_IS_FULL_SCREEN_ID = "WindowIsFullScreen";
    private static final String WINDOW_WIDTH_ID = "WindowWidth";
    private static final String WINDOW_HEIGHT_ID = "WindowHeight";
    private static final String WINDOW_X_ID = "WindowX";
    private static final String WINDOW_Y_ID = "WindowY";

    private static volatile ConfigurationManager theLoadedInstance;

    private DecompilerSettings decompilerSettings;
    private WindowPositionConfig mainWindowPosition;
    private WindowPositionConfig findWindowPosition;
    private UserPreferences userPreferences;

    /**
     * 私有构造函数，请使用 getLoadedInstance() 获取实例
     */
    private ConfigurationManager() {
    }

    /**
     * 获取配置管理器的单例实例
     *
     * @return 配置管理器实例
     */
    public static ConfigurationManager getLoadedInstance() {
        if (theLoadedInstance == null) {
            synchronized (ConfigurationManager.class) {
                if (theLoadedInstance == null) {
                    theLoadedInstance = new ConfigurationManager();
                    theLoadedInstance.loadConfig();
                }
            }
        }
        return theLoadedInstance;
    }

    /**
     * 加载配置信息
     */
    private void loadConfig() {
        decompilerSettings = new DecompilerSettings();
        if (decompilerSettings.getJavaFormattingOptions() == null) {
            decompilerSettings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());
        }
        userPreferences = new UserPreferences();
        mainWindowPosition = new WindowPositionConfig();
        findWindowPosition = new WindowPositionConfig();

        try {
            Preferences prefs = Preferences.userNodeForPackage(ConfigurationManager.class);
            if (!prefs.get(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName())
                    .equals(decompilerSettings.getLanguage().getName()))
                prefs.put(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName());

            decompilerSettings.setFlattenSwitchBlocks(
                    prefs.getBoolean(FLATTEN_SWITCH_BLOCKS_ID, decompilerSettings.getFlattenSwitchBlocks()));
            decompilerSettings.setForceExplicitImports(
                    prefs.getBoolean(FORCE_EXPLICIT_IMPORTS_ID, decompilerSettings.getForceExplicitImports()));
            decompilerSettings.setShowSyntheticMembers(
                    prefs.getBoolean(SHOW_SYNTHETIC_MEMBERS_ID, decompilerSettings.getShowSyntheticMembers()));
            decompilerSettings.setExcludeNestedTypes(
                    prefs.getBoolean(EXCLUDE_NESTED_TYPES_ID, decompilerSettings.getExcludeNestedTypes()));
            decompilerSettings.setForceExplicitTypeArguments(prefs.getBoolean(FORCE_EXPLICIT_TYPE_ARGUMENTS_ID,
                    decompilerSettings.getForceExplicitTypeArguments()));
            decompilerSettings.setRetainRedundantCasts(
                    prefs.getBoolean(RETAIN_REDUNDANT_CASTS_ID, decompilerSettings.getRetainRedundantCasts()));
            decompilerSettings.setIncludeErrorDiagnostics(
                    prefs.getBoolean(INCLUDE_ERROR_DIAGNOSTICS_ID, decompilerSettings.getIncludeErrorDiagnostics()));
            decompilerSettings.setLanguage(
                    findLanguageByName(prefs.get(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName())));
            decompilerSettings.setUnicodeOutputEnabled(prefs.getBoolean(UNICODE_REPLACE_ENABLED_ID, false));

            mainWindowPosition = loadWindowPosition(prefs, MAIN_WINDOW_ID_PREFIX);
            findWindowPosition = loadWindowPosition(prefs, FIND_WINDOW_ID_PREFIX);
            userPreferences = loadUserPreferences(prefs);
        } catch (Exception e) {
            // 加载配置失败，使用默认值
            showConfigError("加载配置失败", e);
        }
    }

    /**
     * 加载窗口位置配置
     */
    private WindowPositionConfig loadWindowPosition(Preferences prefs, String windowIdPrefix) {
        WindowPositionConfig windowPosition = new WindowPositionConfig();
        windowPosition.setFullScreen(prefs.getBoolean(windowIdPrefix + WINDOW_IS_FULL_SCREEN_ID, false));
        windowPosition.setWindowWidth(prefs.getInt(windowIdPrefix + WINDOW_WIDTH_ID, 0));
        windowPosition.setWindowHeight(prefs.getInt(windowIdPrefix + WINDOW_HEIGHT_ID, 0));
        windowPosition.setWindowX(prefs.getInt(windowIdPrefix + WINDOW_X_ID, 0));
        windowPosition.setWindowY(prefs.getInt(windowIdPrefix + WINDOW_Y_ID, 0));
        return windowPosition;
    }

    /**
     * 加载用户偏好设置
     */
    private UserPreferences loadUserPreferences(Preferences prefs) throws Exception {
        UserPreferences newUserPrefs = new UserPreferences();
        for (Field field : UserPreferences.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            String prefId = field.getName();
            Object defaultVal = field.get(newUserPrefs);

            if (field.getType() == String.class) {
                String defaultStr = (String) (defaultVal == null ? "" : defaultVal);
                field.set(newUserPrefs, prefs.get(prefId, defaultStr));

            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                Boolean defaultBool = (Boolean) (defaultVal == null ? Boolean.FALSE : defaultVal);
                field.setBoolean(newUserPrefs, prefs.getBoolean(prefId, defaultBool));

            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                Integer defaultInt = (Integer) (defaultVal == null ? 0 : defaultVal);
                field.setInt(newUserPrefs, prefs.getInt(prefId, defaultInt));
            }
        }
        return newUserPrefs;
    }

    /**
     * 保存配置信息
     */
    public void saveConfig() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ConfigurationManager.class);

            prefs.putBoolean(FLATTEN_SWITCH_BLOCKS_ID, decompilerSettings.getFlattenSwitchBlocks());
            prefs.putBoolean(FORCE_EXPLICIT_IMPORTS_ID, decompilerSettings.getForceExplicitImports());
            prefs.putBoolean(SHOW_SYNTHETIC_MEMBERS_ID, decompilerSettings.getShowSyntheticMembers());
            prefs.putBoolean(EXCLUDE_NESTED_TYPES_ID, decompilerSettings.getExcludeNestedTypes());
            prefs.putBoolean(FORCE_EXPLICIT_TYPE_ARGUMENTS_ID, decompilerSettings.getForceExplicitTypeArguments());
            prefs.putBoolean(RETAIN_REDUNDANT_CASTS_ID, decompilerSettings.getRetainRedundantCasts());
            prefs.putBoolean(INCLUDE_ERROR_DIAGNOSTICS_ID, decompilerSettings.getIncludeErrorDiagnostics());
            prefs.putBoolean(UNICODE_REPLACE_ENABLED_ID, decompilerSettings.isUnicodeOutputEnabled());
            prefs.put(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName());

            saveWindowPosition(prefs, MAIN_WINDOW_ID_PREFIX, mainWindowPosition);
            saveWindowPosition(prefs, FIND_WINDOW_ID_PREFIX, findWindowPosition);
            saveUserPreferences(prefs);
        } catch (Exception e) {
            showConfigError("保存配置失败", e);
        }
    }

    /**
     * 保存窗口位置配置
     */
    private void saveWindowPosition(Preferences prefs, String windowIdPrefix, WindowPositionConfig windowPosition) {
        prefs.putBoolean(windowIdPrefix + WINDOW_IS_FULL_SCREEN_ID, windowPosition.isFullScreen());
        prefs.putInt(windowIdPrefix + WINDOW_WIDTH_ID, windowPosition.getWindowWidth());
        prefs.putInt(windowIdPrefix + WINDOW_HEIGHT_ID, windowPosition.getWindowHeight());
        prefs.putInt(windowIdPrefix + WINDOW_X_ID, windowPosition.getWindowX());
        prefs.putInt(windowIdPrefix + WINDOW_Y_ID, windowPosition.getWindowY());
    }

    /**
     * 保存用户偏好设置
     */
    private void saveUserPreferences(Preferences prefs) throws Exception {
        for (Field field : UserPreferences.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            String prefId = field.getName();
            Object value = field.get(userPreferences);

            if (field.getType() == String.class) {
                prefs.put(prefId, (String) (value == null ? "" : value));

            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                prefs.putBoolean(prefId, (Boolean) (value == null ? Boolean.FALSE : value));

            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                prefs.putInt(prefId, (Integer) (value == null ? 0 : value));
            }
        }
    }

    /**
     * 根据名称查找语言
     */
    private Language findLanguageByName(String languageName) {
        if (languageName != null) {
            if (languageName.equals(Languages.java().getName())) {
                return Languages.java();
            } else if (languageName.equals(Languages.bytecode().getName())) {
                return Languages.bytecode();
            } else if (languageName.equals(Languages.bytecodeAst().getName())) {
                return Languages.bytecodeAst();
            }

            for (Language language : Languages.debug()) {
                if (languageName.equals(language.getName())) {
                    return language;
                }
            }
        }
        return Languages.java();
    }

    /**
     * 显示配置错误信息
     */
    private void showConfigError(String message, Exception e) {
        // 临时使用 System.err，避免循环依赖
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }

    public DecompilerSettings getDecompilerSettings() {
        return decompilerSettings;
    }

    public WindowPositionConfig getMainWindowPosition() {
        return mainWindowPosition;
    }

    public WindowPositionConfig getFindWindowPosition() {
        return findWindowPosition;
    }

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    // 兼容旧API
    @Deprecated
    public UserPreferences getLuytenPreferences() {
        return userPreferences;
    }
}
