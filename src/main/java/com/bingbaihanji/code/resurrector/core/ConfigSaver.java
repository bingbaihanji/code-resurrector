package com.bingbaihanji.code.resurrector.core;

import com.bingbaihanji.code.resurrector.CodeResurrector;
import com.bingbaihanji.code.resurrector.config.ConfigurationManager;
import com.bingbaihanji.code.resurrector.config.UserPreferences;
import com.bingbaihanji.code.resurrector.config.WindowPositionConfig;
import com.bingbaihanji.code.resurrector.model.LuytenPreferences;
import com.bingbaihanji.code.resurrector.model.WindowPosition;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.prefs.Preferences;

/**
 * @deprecated 请使用 {@link ConfigurationManager}
 * 此类仅为向后兼容保留，将在未来版本中移除
 */
@Deprecated
public class ConfigSaver {

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
    private static volatile ConfigSaver theLoadedInstance;
    private DecompilerSettings decompilerSettings;
    private WindowPositionConfig mainWindowPosition;
    private WindowPositionConfig findWindowPosition;
    private LuytenPreferences luytenPreferences;

    /**
     * 不要实例化，请获取载入的实例
     */
    private ConfigSaver() {
    }

    public static ConfigSaver getLoadedInstance() {
        if (theLoadedInstance == null) {
            synchronized (ConfigSaver.class) {
                if (theLoadedInstance == null) {
                    theLoadedInstance = new ConfigSaver();
                    theLoadedInstance.loadConfig();
                }
            }
        }
        return theLoadedInstance;
    }

    /**
     * 不要加载，请获取已载入的实例
     */
    private void loadConfig() {
        decompilerSettings = new DecompilerSettings();
        if (decompilerSettings.getJavaFormattingOptions() == null) {
            decompilerSettings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());
        }
        luytenPreferences = new LuytenPreferences();
        mainWindowPosition = new WindowPositionConfig();
        findWindowPosition = new WindowPositionConfig();
        try {
            Preferences prefs = Preferences.userNodeForPackage(ConfigSaver.class);
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
            luytenPreferences = loadLuytenPreferences(prefs);
        } catch (Exception e) {
            // 加载配置失败，使用默认值，输出错误
            CodeResurrector.showExceptionDialog("加载配置此天", e);
        }
    }

    private WindowPositionConfig loadWindowPosition(Preferences prefs, String windowIdPrefix) {
        WindowPositionConfig windowPosition = new WindowPositionConfig();
        windowPosition.setFullScreen(prefs.getBoolean(windowIdPrefix + WINDOW_IS_FULL_SCREEN_ID, false));
        windowPosition.setWindowWidth(prefs.getInt(windowIdPrefix + WINDOW_WIDTH_ID, 0));
        windowPosition.setWindowHeight(prefs.getInt(windowIdPrefix + WINDOW_HEIGHT_ID, 0));
        windowPosition.setWindowX(prefs.getInt(windowIdPrefix + WINDOW_X_ID, 0));
        windowPosition.setWindowY(prefs.getInt(windowIdPrefix + WINDOW_Y_ID, 0));
        return windowPosition;
    }

    // 根据反射序列化加载用户偏好设置
    private LuytenPreferences loadLuytenPreferences(Preferences prefs) throws Exception {
        LuytenPreferences newLuytenPrefs = new LuytenPreferences();
        for (Field field : UserPreferences.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            String prefId = field.getName();
            Object defaultVal = field.get(newLuytenPrefs);

            if (field.getType() == String.class) {
                String defaultStr = (String) (defaultVal == null ? "" : defaultVal);
                field.set(newLuytenPrefs, prefs.get(prefId, defaultStr));

            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                Boolean defaultBool = (Boolean) (defaultVal == null ? Boolean.FALSE : defaultVal);
                field.setBoolean(newLuytenPrefs, prefs.getBoolean(prefId, defaultBool));

            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                Integer defaultInt = (Integer) (defaultVal == null ? 0 : defaultVal);
                field.setInt(newLuytenPrefs, prefs.getInt(prefId, defaultInt));
            }
        }
        return newLuytenPrefs;
    }

    public void saveConfig() {
        // Windows XP 注册表路径:
        // HKEY_CURRENT_USER/Software/JavaSoft/Prefs/us/deathmarine/luyten
        try {
            Preferences prefs = Preferences.userNodeForPackage(ConfigSaver.class);

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
            saveLuytenPreferences(prefs);
        } catch (Exception e) {
            // 保存配置失败，输出错误信息
            CodeResurrector.showExceptionDialog("保存配置失败", e);
        }
    }

    private void saveWindowPosition(Preferences prefs, String windowIdPrefix, WindowPositionConfig windowPosition) {
        // 保存窗口位置信息
        prefs.putBoolean(windowIdPrefix + WINDOW_IS_FULL_SCREEN_ID, windowPosition.isFullScreen());
        prefs.putInt(windowIdPrefix + WINDOW_WIDTH_ID, windowPosition.getWindowWidth());
        prefs.putInt(windowIdPrefix + WINDOW_HEIGHT_ID, windowPosition.getWindowHeight());
        prefs.putInt(windowIdPrefix + WINDOW_X_ID, windowPosition.getWindowX());
        prefs.putInt(windowIdPrefix + WINDOW_Y_ID, windowPosition.getWindowY());
    }

    // 根据反射序列化保存用户偏好设置
    private void saveLuytenPreferences(Preferences prefs) throws Exception {
        for (Field field : UserPreferences.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            String prefId = field.getName();
            Object value = field.get(luytenPreferences);

            if (field.getType() == String.class) {
                prefs.put(prefId, (String) (value == null ? "" : value));

            } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                prefs.putBoolean(prefId, (Boolean) (value == null ? Boolean.FALSE : value));

            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                prefs.putInt(prefId, (Integer) (value == null ? 0 : value));
            }
        }
    }

    private Language findLanguageByName(String languageName) {
        // 找到特定的语言。不存在返回 Java
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

    public DecompilerSettings getDecompilerSettings() {
        return decompilerSettings;
    }

    public WindowPosition getMainWindowPosition() {
        // 向下转换为兼容类型
        WindowPosition wp = new WindowPosition();
        wp.setFullScreen(mainWindowPosition.isFullScreen());
        wp.setWindowWidth(mainWindowPosition.getWindowWidth());
        wp.setWindowHeight(mainWindowPosition.getWindowHeight());
        wp.setWindowX(mainWindowPosition.getWindowX());
        wp.setWindowY(mainWindowPosition.getWindowY());
        return wp;
    }

    public WindowPosition getFindWindowPosition() {
        // 向下转换为兼容类型
        WindowPosition wp = new WindowPosition();
        wp.setFullScreen(findWindowPosition.isFullScreen());
        wp.setWindowWidth(findWindowPosition.getWindowWidth());
        wp.setWindowHeight(findWindowPosition.getWindowHeight());
        wp.setWindowX(findWindowPosition.getWindowX());
        wp.setWindowY(findWindowPosition.getWindowY());
        return wp;
    }

    public LuytenPreferences getLuytenPreferences() {
        // 直接返回内部实例的引用
        // 这样外部对返回对象的修改会影响到内部状态，配置才能正确保存
        return luytenPreferences;
    }
}
