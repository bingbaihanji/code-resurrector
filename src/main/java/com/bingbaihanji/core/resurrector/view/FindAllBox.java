package com.bingbaihanji.core.resurrector.view;

import com.bingbaihanji.core.resurrector.CodeResurrector;
import com.bingbaihanji.core.resurrector.core.ConfigSaver;
import com.bingbaihanji.core.resurrector.model.Model;
import com.bingbaihanji.core.resurrector.model.WindowPosition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Find All Dialog
 * 对原实现的"安全优化版"
 * - 不改变对外 API
 * - 修复 Swing 线程问题
 * - 保持原有行为
 */
public class FindAllBox extends JDialog {

    @Serial
    private static final long serialVersionUID = -4125409760166690462L;
    private static final int MIN_WIDTH = 640;
    private final JButton findButton;
    private final JTextField textField;
    private final JCheckBox mcase;
    private final JCheckBox regex;
    private final JCheckBox wholew;
    private final JCheckBox classname;
    private final JList<String> list;
    private final JProgressBar progressBar;
    private final JLabel statusLabel = new JLabel("");
    private final DefaultListModel<String> classesList = new DefaultListModel<>();
    private final MainWindow mainWindow;
    // 多线程状态标记(保证可见性)
    private volatile boolean locked;
    private volatile boolean searching;
    private Thread tmp_thread;

    public FindAllBox(final MainWindow mainWindow) {
        super();
        setModalityType(ModalityType.MODELESS);
        this.mainWindow = mainWindow;

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setHideOnEscapeButton();

        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(true);

        JLabel label = new JLabel("查找内容:");
        label.setOpaque(false);

        textField = new JTextField();
        textField.setOpaque(true);

        findButton = new JButton("查找");
        findButton.setOpaque(true);
        findButton.addActionListener(new FindButton());

        mcase = new JCheckBox("区分大小写");
        mcase.setOpaque(false);
        regex = new JCheckBox("正则表达式");
        regex.setOpaque(false);
        wholew = new JCheckBox("匹配整个单词");
        wholew.setOpaque(false);
        classname = new JCheckBox("类名");
        classname.setOpaque(false);

        progressBar = new JProgressBar(0, 100);

        list = new JList<>(classesList);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() != 2) return;

                int index = list.locationToIndex(evt.getPoint());
                String entryName = classesList.get(index);
                String[] array = entryName.split("/");

                try {
                    if (entryName.toLowerCase().endsWith(".class")) {
                        String internalName = StringUtilities.removeRight(entryName, ".class");
                        TypeReference type = Model.metadataSystem.lookupType(internalName);
                        mainWindow.getSelectedModel()
                                .extractClassToTextPane(type, array[array.length - 1], entryName, null);
                    } else {
                        JarFile jfile = new JarFile(mainWindow.getSelectedModel().getOpenedFile());
                        mainWindow.getSelectedModel()
                                .extractSimpleFileEntryToTextPane(
                                        jfile.getInputStream(jfile.getEntry(entryName)),
                                        array[array.length - 1],
                                        entryName);
                        jfile.close();
                    }
                } catch (Exception e) {
                    CodeResurrector.showExceptionDialog("Exception!", e);
                }
            }
        });

        JScrollPane listScroller = new JScrollPane(list);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.max((int) (screenSize.width * 0.35), MIN_WIDTH);
        setBounds((int) (width * 0.2), 100, width, 500);
        setResizable(false);

        GroupLayout layout = new GroupLayout(mainPanel);
        mainPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addComponent(label)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(statusLabel)
                                .addComponent(textField)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(mcase)
                                        .addComponent(wholew)
                                        .addComponent(regex)
                                        .addComponent(classname))
                                .addComponent(listScroller)
                                .addComponent(progressBar))
                        .addComponent(findButton)
        );

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup()
                                .addComponent(label)
                                .addComponent(textField)
                                .addComponent(findButton))
                        .addGroup(layout.createParallelGroup()
                                .addComponent(mcase)
                                .addComponent(wholew)
                                .addComponent(regex)
                                .addComponent(classname))
                        .addComponent(listScroller)
                        .addComponent(statusLabel)
                        .addComponent(progressBar)
        );

        // 将主面板添加到对话框
        getContentPane().add(mainPanel);

        adjustWindowPositionBySavedState();
        setSaveWindowPositionOnClosing();
        setTitle("全部查找");

        // 强制更新UI
        SwingUtilities.updateComponentTreeUI(this);
    }

    // ================== 原有 public API（原样保留） ==================

    public void showFindBox() {
        if (SwingUtilities.isEventDispatchThread()) {
            setVisible(true);
            textField.requestFocus();
        } else {
            SwingUtilities.invokeLater(() -> {
                setVisible(true);
                textField.requestFocus();
            });
        }
    }

    public void hideFindBox() {
        if (SwingUtilities.isEventDispatchThread()) {
            setVisible(false);
        } else {
            SwingUtilities.invokeLater(() -> setVisible(false));
        }
    }

    public boolean isSearching() {
        return searching;
    }

    public void setSearching(boolean searching) {
        this.searching = searching;
    }

    // ================== 搜索匹配逻辑（未改行为） ==================

    private boolean search(String bulk) {
        String a = textField.getText();
        String b = bulk;

        if (regex.isSelected())
            return Pattern.matches(a, b);

        if (wholew.isSelected())
            a = " " + a + " ";

        if (!mcase.isSelected()) {
            a = a.toLowerCase();
            b = b.toLowerCase();
        }
        return b.contains(a);
    }

    // ================== Swing UI 安全工具 ==================

    private void ui(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    public void setStatus(String text) {
        ui(() -> {
            if (text.length() > 25) {
                statusLabel.setText("Searching in file: ..." +
                        text.substring(text.length() - 25));
            } else {
                statusLabel.setText("Searching in file: " + text);
            }
            progressBar.setValue(progressBar.getValue() + 1);
        });
    }

    public void addClassName(String className) {
        ui(() -> classesList.addElement(className));
    }

    public void initProgressBar(Integer length) {
        ui(() -> {
            progressBar.setMaximum(length);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
        });
    }

    // ================== 查找按钮逻辑（线程安全修复） ==================

    private void setHideOnEscapeButton() {
        getRootPane().registerKeyboardAction(
                e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ================== 窗口辅助 ==================

    private void adjustWindowPositionBySavedState() {
        WindowPosition pos =
                ConfigSaver.getLoadedInstance().getFindWindowPosition();
        if (pos.isSavedWindowPositionValid()) {
            setLocation(pos.getWindowX(), pos.getWindowY());
        }
    }

    private void setSaveWindowPositionOnClosing() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) {
                ConfigSaver.getLoadedInstance()
                        .getFindWindowPosition()
                        .readPositionFromDialog(FindAllBox.this);
            }
        });
    }

    private class FindButton extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent event) {

            // 停止搜索
            if ("停止".equals(findButton.getText())) {
                if (tmp_thread != null)
                    tmp_thread.interrupt();
                setStatus("已停止.");
                findButton.setText("查找");
                locked = false;
                searching = false;
                return;
            }

            // 开始搜索
            findButton.setText("停止");
            classesList.clear();
            searching = true;
            locked = false;

            tmp_thread = new Thread(() -> {
                File inFile = mainWindow.getSelectedModel().getOpenedFile();
                DecompilerSettings settings =
                        ConfigSaver.getLoadedInstance().getDecompilerSettings();
                boolean filter =
                        ConfigSaver.getLoadedInstance()
                                .getLuytenPreferences()
                                .isFilterOutInnerClassEntries();

                try (JarFile jfile = new JarFile(inFile)) {

                    initProgressBar(Collections.list(jfile.entries()).size());
                    Enumeration<JarEntry> entries = jfile.entries();

                    while (!Thread.currentThread().isInterrupted()
                            && entries.hasMoreElements()
                            && "停止".equals(findButton.getText())) {

                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        setStatus(name);

                        if (filter && name.contains("$"))
                            continue;

                        // 仅搜索类名
                        if (classname.isSelected()) {
                            if (search(name))
                                addClassName(name);
                            continue;
                        }

                        // class 文件搜索
                        if (name.endsWith(".class")) {
                            synchronized (settings) {
                                String internalName =
                                        StringUtilities.removeRight(name, ".class");
                                TypeReference type =
                                        Model.metadataSystem.lookupType(internalName);
                                TypeDefinition def =
                                        type != null ? type.resolve() : null;
                                if (def == null)
                                    continue;

                                StringWriter sw = new StringWriter();
                                DecompilationOptions opt = new DecompilationOptions();
                                opt.setSettings(settings);
                                opt.setFullDecompilation(true);

                                PlainTextOutput out = new PlainTextOutput(sw);
                                out.setUnicodeOutputEnabled(
                                        settings.isUnicodeOutputEnabled());

                                settings.getLanguage()
                                        .decompileType(def, out, opt);

                                if (search(sw.toString()))
                                    addClassName(name);
                            }
                        }
                        // 普通文本文件
                        else {
                            StringBuilder sb = new StringBuilder();
                            long nonPrintable = 0;

                            // 使用 UTF-8 编码读取文件，确保中文正确显示
                            try (BufferedReader reader =
                                         new BufferedReader(
                                                 new InputStreamReader(
                                                         jfile.getInputStream(entry), StandardCharsets.UTF_8))) {

                                String line;
                                while ((line = reader.readLine()) != null) {
                                    sb.append(line).append('\n');
                                    for (byte b : line.getBytes()) {
                                        if (b <= 0)
                                            nonPrintable++;
                                    }
                                }
                            }

                            if (nonPrintable < 5 && search(sb.toString()))
                                addClassName(name);
                        }
                    }

                } catch (Exception e) {
                    CodeResurrector.showExceptionDialog("Exception!", e);
                } finally {
                    searching = false;
                    locked = false;
                    ui(() -> findButton.setText("查找"));
                }
            });

            tmp_thread.start();
        }
    }
}
