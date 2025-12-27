package com.bingbaihanji.code.resurrector;

import com.bingbaihanji.code.resurrector.decompiler.DecompilerType;
import com.strobel.Procyon;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * Main menu (only MainWindow should be called from here)
 */
public class MainMenuBar extends JMenuBar {
    private static final long serialVersionUID = -7949855817172562075L;
    private static final Logger logger = LoggerFactory.getLogger(MainMenuBar.class);
    private final MainWindow mainWindow;
    private final Map<String, Language> languageLookup = new HashMap<String, Language>();
    private final DecompilerSettings settings;
    private JMenu recentFiles;
    private JMenuItem clearRecentFiles;
    private JCheckBoxMenuItem flattenSwitchBlocks;
    private JCheckBoxMenuItem forceExplicitImports;
    private JCheckBoxMenuItem forceExplicitTypes;
    private JCheckBoxMenuItem showSyntheticMembers;
    private JCheckBoxMenuItem excludeNestedTypes;
    private JCheckBoxMenuItem retainRedundantCasts;
    private JCheckBoxMenuItem unicodeReplacement;
    private JCheckBoxMenuItem debugLineNumbers;
    private JCheckBoxMenuItem showDebugInfo;
    private JCheckBoxMenuItem bytecodeLineNumbers;
    private JRadioButtonMenuItem java;
    private JRadioButtonMenuItem bytecode;
    private JRadioButtonMenuItem bytecodeAST;
    private ButtonGroup languagesGroup;
    private ButtonGroup themesGroup;
    private JCheckBoxMenuItem packageExplorerStyle;
    private JCheckBoxMenuItem filterOutInnerClassEntries;
    private JCheckBoxMenuItem singleClickOpenEnabled;
    private JCheckBoxMenuItem exitByEscEnabled;
    private ButtonGroup decompilerEngineGroup;
    private LuytenPreferences luytenPrefs;

    public MainMenuBar(MainWindow mainWnd) {
        this.mainWindow = mainWnd;
        final ConfigSaver configSaver = ConfigSaver.getLoadedInstance();
        settings = configSaver.getDecompilerSettings();
        luytenPrefs = configSaver.getLuytenPreferences();

        final JMenu fileMenu = new JMenu("文件");
        fileMenu.add(new JMenuItem("..."));
        this.add(fileMenu);
        final JMenu editMenu = new JMenu("编辑");
        editMenu.add(new JMenuItem("..."));
        this.add(editMenu);
        final JMenu themesMenu = new JMenu("主题");
        themesMenu.add(new JMenuItem("..."));
        this.add(themesMenu);
        final JMenu operationMenu = new JMenu("操作");
        operationMenu.add(new JMenuItem("..."));
        this.add(operationMenu);
        final JMenu settingsMenu = new JMenu("设置");
        settingsMenu.add(new JMenuItem("..."));
        this.add(settingsMenu);
        final JMenu helpMenu = new JMenu("帮助");
        helpMenu.add(new JMenuItem("..."));
        this.add(helpMenu);

        // start quicker
        new Thread() {
            public void run() {
                try {
                    // build menu later
                    buildFileMenu(fileMenu);
                    refreshMenuPopup(fileMenu);

                    buildEditMenu(editMenu);
                    refreshMenuPopup(editMenu);

                    buildThemesMenu(themesMenu);
                    refreshMenuPopup(themesMenu);

                    buildOperationMenu(operationMenu);
                    refreshMenuPopup(operationMenu);

                    buildSettingsMenu(settingsMenu, configSaver);
                    refreshMenuPopup(settingsMenu);

                    buildHelpMenu(helpMenu);
                    refreshMenuPopup(helpMenu);

                    updateRecentFiles();
                } catch (Exception e) {
                    CodeResurrector.showExceptionDialog("Exception!", e);
                }
            }

            // refresh currently opened menu
            // (if user selected a menu before it was ready)
            private void refreshMenuPopup(JMenu menu) {
                try {
                    if (menu.isPopupMenuVisible()) {
                        menu.getPopupMenu().setVisible(false);
                        menu.getPopupMenu().setVisible(true);
                    }
                } catch (Exception e) {
                    CodeResurrector.showExceptionDialog("Exception!", e);
                }
            }
        }.start();
    }

    public void updateRecentFiles() {
        if (RecentFiles.paths.isEmpty()) {
            recentFiles.setEnabled(false);
            clearRecentFiles.setEnabled(false);
            return;
        } else {
            recentFiles.setEnabled(true);
            clearRecentFiles.setEnabled(true);
        }

        recentFiles.removeAll();
        ListIterator<String> li = RecentFiles.paths.listIterator(RecentFiles.paths.size());
        boolean rfSaveNeeded = false;

        while (li.hasPrevious()) {
            String path = li.previous();
            final File file = new File(path);

            if (!file.exists()) {
                rfSaveNeeded = true;
                continue;
            }

            JMenuItem menuItem = new JMenuItem(path);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mainWindow.loadNewFile(file);
                }
            });
            recentFiles.add(menuItem);
        }

        if (rfSaveNeeded) RecentFiles.save();
    }

    private void buildFileMenu(final JMenu fileMenu) {
        fileMenu.removeAll();
        JMenuItem menuItem = new JMenuItem("打开文件...");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onOpenFileMenu();
            }
        });
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("关闭文件");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane house = mainWindow.getSelectedModel().house;

                if (e.getModifiers() != 2 || house.getTabCount() == 0)
                    mainWindow.onCloseFileMenu();
                else {
                    mainWindow.getSelectedModel().closeOpenTab(house.getSelectedIndex());
                }
            }
        });
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("另存为...");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onSaveAsMenu();
            }
        });
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("全部保存...");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onSaveAllMenu();
            }
        });
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        recentFiles = new JMenu("最近打开");
        fileMenu.add(recentFiles);

        clearRecentFiles = new JMenuItem("清除最近打开");
        clearRecentFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RecentFiles.paths.clear();
                RecentFiles.save();
                updateRecentFiles();
            }
        });
        fileMenu.add(clearRecentFiles);

        fileMenu.addSeparator();

        // Only add the exit command for non-OS X. OS X handles its close
        // automatically
        if (!Boolean.getBoolean("apple.laf.useScreenMenuBar")) {
            menuItem = new JMenuItem("退出");
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mainWindow.onExitMenu();
                }
            });
            fileMenu.add(menuItem);
        }
    }

    private void buildEditMenu(JMenu editMenu) {
        editMenu.removeAll();
        JMenuItem menuItem = new JMenuItem("剪切");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setEnabled(false);
        editMenu.add(menuItem);

        menuItem = new JMenuItem("复制");
        menuItem.addActionListener(new DefaultEditorKit.CopyAction());
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        editMenu.add(menuItem);

        menuItem = new JMenuItem("粘贴");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setEnabled(false);
        editMenu.add(menuItem);

        editMenu.addSeparator();

        menuItem = new JMenuItem("全选");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onSelectAllMenu();
            }
        });
        editMenu.add(menuItem);
        editMenu.addSeparator();

        menuItem = new JMenuItem("查找...");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onFindMenu();
            }
        });
        editMenu.add(menuItem);

        menuItem = new JMenuItem("查找下一个");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mainWindow.findBox != null) mainWindow.findBox.fireExploreAction(true);
            }
        });
        editMenu.add(menuItem);

        menuItem = new JMenuItem("查找上一个");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mainWindow.findBox != null) mainWindow.findBox.fireExploreAction(false);
            }
        });
        editMenu.add(menuItem);

        menuItem = new JMenuItem("全部查找");
        menuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onFindAllMenu();

            }
        });
        editMenu.add(menuItem);
    }

    private void buildThemesMenu(JMenu themesMenu) {
        themesMenu.removeAll();
        themesGroup = new ButtonGroup();
        JRadioButtonMenuItem a = new JRadioButtonMenuItem(new ThemeAction("默认", "default.xml"));
        a.setSelected("default.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("默认-Alt", "default-alt.xml"));
        a.setSelected("default-alt.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("深色", "dark.xml"));
        a.setSelected("dark.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("深色-自定义", "dark-custom.xml"));
        a.setSelected("dark-custom.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("Eclipse", "eclipse.xml"));
        a.setSelected("eclipse.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("Visual Studio", "vs.xml"));
        a.setSelected("vs.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);

        a = new JRadioButtonMenuItem(new ThemeAction("IntelliJ", "idea.xml"));
        a.setSelected("idea.xml".equals(luytenPrefs.getThemeXml()));
        themesGroup.add(a);
        themesMenu.add(a);
    }

    private void buildOperationMenu(JMenu operationMenu) {
        operationMenu.removeAll();
        packageExplorerStyle = new JCheckBoxMenuItem("包管理器样式");
        packageExplorerStyle.setSelected(luytenPrefs.isPackageExplorerStyle());
        packageExplorerStyle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                luytenPrefs.setPackageExplorerStyle(packageExplorerStyle.isSelected());
                mainWindow.onTreeSettingsChanged();
            }
        });
        operationMenu.add(packageExplorerStyle);

        filterOutInnerClassEntries = new JCheckBoxMenuItem("过滤内部类");
        filterOutInnerClassEntries.setSelected(luytenPrefs.isFilterOutInnerClassEntries());
        filterOutInnerClassEntries.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                luytenPrefs.setFilterOutInnerClassEntries(filterOutInnerClassEntries.isSelected());
                mainWindow.onTreeSettingsChanged();
            }
        });
        operationMenu.add(filterOutInnerClassEntries);

        singleClickOpenEnabled = new JCheckBoxMenuItem("单击打开");
        singleClickOpenEnabled.setSelected(luytenPrefs.isSingleClickOpenEnabled());
        singleClickOpenEnabled.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                luytenPrefs.setSingleClickOpenEnabled(singleClickOpenEnabled.isSelected());
            }
        });
        operationMenu.add(singleClickOpenEnabled);

        exitByEscEnabled = new JCheckBoxMenuItem("按Esc退出");
        exitByEscEnabled.setSelected(luytenPrefs.isExitByEscEnabled());
        exitByEscEnabled.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                luytenPrefs.setExitByEscEnabled(exitByEscEnabled.isSelected());
            }
        });
        operationMenu.add(exitByEscEnabled);
    }

    private void buildSettingsMenu(JMenu settingsMenu, ConfigSaver configSaver) {
        settingsMenu.removeAll();

        // 反编译引擎选择
        JMenu decompilerEngineMenu = new JMenu("反编译引擎");
        decompilerEngineGroup = new ButtonGroup();

        for (DecompilerType type : DecompilerType.values()) {
            JRadioButtonMenuItem engineItem = new JRadioButtonMenuItem(type.getName());
            engineItem.setSelected(type == luytenPrefs.getDecompilerType());
            engineItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    luytenPrefs.setDecompilerType(type);
                    mainWindow.onSettingsChanged();
                }
            });
            decompilerEngineGroup.add(engineItem);
            decompilerEngineMenu.add(engineItem);
        }
        settingsMenu.add(decompilerEngineMenu);
        settingsMenu.addSeparator();

        ActionListener settingsChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        populateSettingsFromSettingsMenu();
                        mainWindow.onSettingsChanged();
                    }
                }.start();
            }
        };
        flattenSwitchBlocks = new JCheckBoxMenuItem("展平Switch块");
        flattenSwitchBlocks.setSelected(settings.getFlattenSwitchBlocks());
        flattenSwitchBlocks.addActionListener(settingsChanged);
        settingsMenu.add(flattenSwitchBlocks);

        forceExplicitImports = new JCheckBoxMenuItem("强制显式导入");
        forceExplicitImports.setSelected(settings.getForceExplicitImports());
        forceExplicitImports.addActionListener(settingsChanged);
        settingsMenu.add(forceExplicitImports);

        forceExplicitTypes = new JCheckBoxMenuItem("强制显式类型");
        forceExplicitTypes.setSelected(settings.getForceExplicitTypeArguments());
        forceExplicitTypes.addActionListener(settingsChanged);
        settingsMenu.add(forceExplicitTypes);

        showSyntheticMembers = new JCheckBoxMenuItem("显示合成成员");
        showSyntheticMembers.setSelected(settings.getShowSyntheticMembers());
        showSyntheticMembers.addActionListener(settingsChanged);
        settingsMenu.add(showSyntheticMembers);

        excludeNestedTypes = new JCheckBoxMenuItem("排除嵌套类型");
        excludeNestedTypes.setSelected(settings.getExcludeNestedTypes());
        excludeNestedTypes.addActionListener(settingsChanged);
        settingsMenu.add(excludeNestedTypes);

        retainRedundantCasts = new JCheckBoxMenuItem("保留冗余强制转换");
        retainRedundantCasts.setSelected(settings.getRetainRedundantCasts());
        retainRedundantCasts.addActionListener(settingsChanged);
        settingsMenu.add(retainRedundantCasts);

        unicodeReplacement = new JCheckBoxMenuItem("启用Unicode替换");
        unicodeReplacement.setSelected(settings.isUnicodeOutputEnabled());
        unicodeReplacement.addActionListener(settingsChanged);
        settingsMenu.add(unicodeReplacement);

        debugLineNumbers = new JCheckBoxMenuItem("显示调试行号");
        debugLineNumbers.setSelected(settings.getShowDebugLineNumbers());
        debugLineNumbers.addActionListener(settingsChanged);
        settingsMenu.add(debugLineNumbers);

        JMenu debugSettingsMenu = new JMenu("调试设置");
        showDebugInfo = new JCheckBoxMenuItem("包含错误诊断");
        showDebugInfo.setSelected(settings.getIncludeErrorDiagnostics());
        showDebugInfo.addActionListener(settingsChanged);

        debugSettingsMenu.add(showDebugInfo);
        settingsMenu.add(debugSettingsMenu);
        settingsMenu.addSeparator();

        languageLookup.put(Languages.java().getName(), Languages.java());
        languageLookup.put(Languages.bytecode().getName(), Languages.bytecode());
        languageLookup.put(Languages.bytecodeAst().getName(), Languages.bytecodeAst());

        languagesGroup = new ButtonGroup();
        java = new JRadioButtonMenuItem(Languages.java().getName());
        java.getModel().setActionCommand(Languages.java().getName());
        java.setSelected(Languages.java().getName().equals(settings.getLanguage().getName()));
        languagesGroup.add(java);
        settingsMenu.add(java);
        bytecode = new JRadioButtonMenuItem(Languages.bytecode().getName());
        bytecode.getModel().setActionCommand(Languages.bytecode().getName());
        bytecode.setSelected(Languages.bytecode().getName().equals(settings.getLanguage().getName()));
        languagesGroup.add(bytecode);
        settingsMenu.add(bytecode);
        bytecodeAST = new JRadioButtonMenuItem(Languages.bytecodeAst().getName());
        bytecodeAST.getModel().setActionCommand(Languages.bytecodeAst().getName());
        bytecodeAST.setSelected(Languages.bytecodeAst().getName().equals(settings.getLanguage().getName()));
        languagesGroup.add(bytecodeAST);
        settingsMenu.add(bytecodeAST);

        JMenu debugLanguagesMenu = new JMenu("调试语言");
        for (final Language language : Languages.debug()) {
            final JRadioButtonMenuItem m = new JRadioButtonMenuItem(language.getName());
            m.getModel().setActionCommand(language.getName());
            m.setSelected(language.getName().equals(settings.getLanguage().getName()));
            languagesGroup.add(m);
            debugLanguagesMenu.add(m);
            languageLookup.put(language.getName(), language);
        }
        for (AbstractButton button : Collections.list(languagesGroup.getElements())) {
            button.addActionListener(settingsChanged);
        }
        settingsMenu.add(debugLanguagesMenu);

        bytecodeLineNumbers = new JCheckBoxMenuItem("在字节码中显示行号");
        bytecodeLineNumbers.setSelected(settings.getIncludeLineNumbersInBytecode());
        bytecodeLineNumbers.addActionListener(settingsChanged);
        settingsMenu.add(bytecodeLineNumbers);
    }

    private void buildHelpMenu(JMenu helpMenu) {
        helpMenu.removeAll();
        JMenuItem menuItem = new JMenuItem("法律信息");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onLegalMenu();
            }
        });
        helpMenu.add(menuItem);
        JMenu menuDebug = new JMenu("调试");
        menuItem = new JMenuItem("列出JVM类");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.onListLoadedClasses();
            }
        });
        menuDebug.add(menuItem);
        helpMenu.add(menuDebug);
        menuItem = new JMenuItem("关于");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JPanel pane = new JPanel();
                pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
                JLabel title = new JLabel("Luyten " + CodeResurrector.getVersion());
                title.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
                pane.add(title);
                pane.add(new JLabel("by Deathmarine"));
                String project = "https://github.com/deathmarine/Luyten/";
                JLabel link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + project + "</U></FONT></HTML>");
                link.setCursor(new Cursor(Cursor.HAND_CURSOR));
                link.addMouseListener(new LinkListener(project, link));
                pane.add(link);
                pane.add(new JLabel("贡献者:"));
                pane.add(new JLabel("zerdei, toonetown, dstmath"));
                pane.add(new JLabel("virustotalop, xtrafrancyz,"));
                pane.add(new JLabel("mbax, quitten, mstrobel,"));
                pane.add(new JLabel("FisheyLP, and Syquel"));
                pane.add(new JLabel("冰白寒祭"));
                pane.add(new JLabel(" "));
                pane.add(new JLabel("由动力:"));
                String procyon = "https://bitbucket.org/mstrobel/procyon";
                link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + procyon + "</U></FONT></HTML>");
                link.setCursor(new Cursor(Cursor.HAND_CURSOR));
                link.addMouseListener(new LinkListener(procyon, link));
                pane.add(link);
                pane.add(new JLabel("版本: " + Procyon.version()));
                pane.add(new JLabel("(c) 2018 Mike Strobel"));
                String rsyntax = "https://github.com/bobbylight/RSyntaxTextArea";
                link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + rsyntax + "</U></FONT></HTML>");
                link.setCursor(new Cursor(Cursor.HAND_CURSOR));
                link.addMouseListener(new LinkListener(rsyntax, link));
                pane.add(link);
                pane.add(new JLabel("版本: 3.0.2"));
                pane.add(new JLabel("(c) 2019 Robert Futrell"));
                pane.add(new JLabel(" "));
                JOptionPane.showMessageDialog(null, pane);
            }
        });
        helpMenu.add(menuItem);
    }

    private void populateSettingsFromSettingsMenu() {
        // synchronized: do not disturb decompiler at work (synchronize every
        // time before run decompiler)
        synchronized (settings) {
            settings.setFlattenSwitchBlocks(flattenSwitchBlocks.isSelected());
            settings.setForceExplicitImports(forceExplicitImports.isSelected());
            settings.setShowSyntheticMembers(showSyntheticMembers.isSelected());
            settings.setExcludeNestedTypes(excludeNestedTypes.isSelected());
            settings.setForceExplicitTypeArguments(forceExplicitTypes.isSelected());
            settings.setRetainRedundantCasts(retainRedundantCasts.isSelected());
            settings.setIncludeErrorDiagnostics(showDebugInfo.isSelected());
            settings.setUnicodeOutputEnabled(unicodeReplacement.isSelected());
            settings.setShowDebugLineNumbers(debugLineNumbers.isSelected());
            //
            // Note: You shouldn't ever need to set this. It's only for
            // languages that support catch
            // blocks without an exception variable. Java doesn't allow this. I
            // think Scala does.
            //
            // settings.setAlwaysGenerateExceptionVariableForCatchBlocks(true);
            //

            final ButtonModel selectedLanguage = languagesGroup.getSelection();
            if (selectedLanguage != null) {
                final Language language = languageLookup.get(selectedLanguage.getActionCommand());

                if (language != null)
                    settings.setLanguage(language);
            }

            if (java.isSelected()) {
                settings.setLanguage(Languages.java());
            } else if (bytecode.isSelected()) {
                settings.setLanguage(Languages.bytecode());
            } else if (bytecodeAST.isSelected()) {
                settings.setLanguage(Languages.bytecodeAst());
            }
            settings.setIncludeLineNumbersInBytecode(bytecodeLineNumbers.isSelected());
        }
    }

    private static class LinkListener extends MouseAdapter {
        String link;
        JLabel label;

        public LinkListener(String link, JLabel label) {
            this.link = link;
            this.label = label;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            try {
                Desktop.getDesktop().browse(new URI(link));
            } catch (Exception e1) {
                logger.error("Failed to open URL in browser: {}", link, e1);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            label.setText("<HTML><FONT color=\"#00aa99\"><U>" + link + "</U></FONT></HTML>");
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setText("<HTML><FONT color=\"#000099\"><U>" + link + "</U></FONT></HTML>");
        }

    }

    private class ThemeAction extends AbstractAction {
        private static final long serialVersionUID = -6618680171943723199L;
        private final String xml;

        public ThemeAction(String name, String xml) {
            putValue(NAME, name);
            this.xml = xml;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            luytenPrefs.setThemeXml(xml);
            mainWindow.onThemesChanged();
        }
    }
}
