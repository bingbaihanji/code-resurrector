package com.bingbaihanji.code.resurrector.io;

import com.bingbaihanji.code.resurrector.CodeResurrector;
import com.bingbaihanji.code.resurrector.core.ConfigSaver;
import com.bingbaihanji.code.resurrector.core.LuytenTypeLoader;
import com.bingbaihanji.code.resurrector.model.LuytenPreferences;
import com.strobel.assembler.metadata.*;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * 执行保存和全部保存操作
 */
public class FileSaver {

    private final JProgressBar bar;
    private final JLabel label;
    private boolean cancel;
    private boolean extracting;

    public FileSaver(JProgressBar bar, JLabel label) {
        this.bar = bar;
        this.label = label;
        final JPopupMenu menu = new JPopupMenu("取消");
        final JMenuItem item = new JMenuItem("取消");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setCancel(true);
            }
        });
        menu.add(item);
        this.label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent ev) {
                if (SwingUtilities.isRightMouseButton(ev) && isExtracting())
                    menu.show(ev.getComponent(), ev.getX(), ev.getY());
            }
        });
    }

    public static String getTime(long time) {
        // 根据流失时间訚試换成人类可読的二缠進位
        long lap = System.currentTimeMillis() - time;
        lap = lap / 1000;
        StringBuilder sb = new StringBuilder();
        long hour = ((lap / 60) / 60);
        long min = ((lap - (hour * 60 * 60)) / 60);
        long sec = ((lap - (hour * 60 * 60) - (min * 60)));
        if (hour > 0)
            sb.append("Hour:").append(hour).append(" ");
        sb.append("Min(s): ").append(min).append(" Sec: ").append(sec);
        return sb.toString();
    }

    public void saveText(final String text, final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DecompilerSettings settings = cloneSettings();
                boolean isUnicodeEnabled = settings.isUnicodeOutputEnabled();
                long time = System.currentTimeMillis();
                try (FileOutputStream fos = new FileOutputStream(file);
                     OutputStreamWriter writer = isUnicodeEnabled ? new OutputStreamWriter(fos, "UTF-8")
                             : new OutputStreamWriter(fos);
                     BufferedWriter bw = new BufferedWriter(writer);) {
                    label.setText("处理中: " + file.getName());
                    bar.setVisible(true);
                    bw.write(text);
                    bw.flush();
                    label.setText("完成: " + getTime(time));
                } catch (Exception e1) {
                    label.setText("不能保存文件: " + file.getName());
                    CodeResurrector.showExceptionDialog("无法保存文件", e1);
                } finally {
                    setExtracting(false);
                    bar.setVisible(false);
                }
            }
        }).start();
    }

    public void saveAllDecompiled(final File inFile, final File outFile) {
        new Thread(() -> {
            long start = System.currentTimeMillis();
            // ---- UI: 开始 ----
            SwingUtilities.invokeLater(() -> {
                bar.setVisible(true);
                setExtracting(true);
                label.setText("处理中: " + outFile.getName());
            });

            try {
                System.out.println("[SaveAll]: "
                        + inFile.getName() + " -> " + outFile.getName());

                String name = inFile.getName().toLowerCase(Locale.ROOT);

                if (name.endsWith(".jar") || name.endsWith(".zip")) {
                    doSaveJarDecompiled(inFile, outFile);
                } else if (name.endsWith(".class")) {
                    doSaveClassDecompiled(inFile, outFile);
                } else {
                    doSaveUnknownFile(inFile, outFile);
                }

                if (cancel) {
                    SwingUtilities.invokeLater(() -> label.setText("已取消"));
                    outFile.delete();
                    setCancel(false);
                } else {
                    SwingUtilities.invokeLater(() ->
                            label.setText("完成: " + getTime(start))
                    );
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    label.setText("不能保存文件: " + outFile.getName());
                    CodeResurrector.showExceptionDialog("无法保存文件", e);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    setExtracting(false);
                    bar.setVisible(false);
                });
            }
        }).start();
    }


    private void doSaveJarDecompiled(File inFile, File outFile) throws Exception {
        try (JarFile jfile = new JarFile(inFile);
             FileOutputStream dest = new FileOutputStream(outFile);
             BufferedOutputStream buffDest = new BufferedOutputStream(dest);
             ZipOutputStream out = new ZipOutputStream(buffDest);) {
            bar.setMinimum(0);
            bar.setMaximum(jfile.size());
            byte[] data = new byte[1024 * 8];
            DecompilerSettings settings = cloneSettings();
            LuytenTypeLoader typeLoader = new LuytenTypeLoader();
            MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
            ITypeLoader jarLoader = new JarTypeLoader(jfile);
            typeLoader.getTypeLoaders().add(jarLoader);

            DecompilationOptions decompilationOptions = new DecompilationOptions();
            decompilationOptions.setSettings(settings);
            decompilationOptions.setFullDecompilation(true);

            List<String> mass = null;
            JarEntryFilter jarEntryFilter = new JarEntryFilter(jfile);
            LuytenPreferences luytenPrefs = ConfigSaver.getLoadedInstance().getLuytenPreferences();
            if (luytenPrefs.isFilterOutInnerClassEntries()) {
                mass = jarEntryFilter.getEntriesWithoutInnerClasses();
            } else {
                mass = jarEntryFilter.getAllEntriesFromJar();
            }

            Enumeration<JarEntry> ent = jfile.entries();
            Set<String> history = new HashSet<String>();
            int tick = 0;
            while (ent.hasMoreElements() && !cancel) {
                bar.setValue(++tick);
                JarEntry entry = ent.nextElement();
                if (!mass.contains(entry.getName()))
                    continue;
                label.setText("处理中: " + entry.getName());
                bar.setVisible(true);
                if (entry.getName().endsWith(".class")) {
                    JarEntry etn = new JarEntry(entry.getName().replace(".class", ".java"));
                    label.setText("提取中: " + etn.getName());
                    System.out.println("[保存全部]：" + etn.getName() + " -> " + outFile.getName());

                    if (history.add(etn.getName())) {
                        out.putNextEntry(etn);
                        try {
                            boolean isUnicodeEnabled = decompilationOptions.getSettings().isUnicodeOutputEnabled();
                            String internalName = StringUtilities.removeRight(entry.getName(), ".class");
                            TypeReference type = metadataSystem.lookupType(internalName);
                            TypeDefinition resolvedType = null;
                            if ((type == null) || ((resolvedType = type.resolve()) == null)) {
                                throw new Exception("Unable to resolve type.");
                            }
                            Writer writer = isUnicodeEnabled ? new OutputStreamWriter(out, "UTF-8")
                                    : new OutputStreamWriter(out);
                            PlainTextOutput plainTextOutput = new PlainTextOutput(writer);
                            plainTextOutput.setUnicodeOutputEnabled(isUnicodeEnabled);
                            settings.getLanguage().decompileType(resolvedType, plainTextOutput, decompilationOptions);
                            writer.flush();
                        } catch (Exception e) {
                            label.setText("不能反编译文件: " + entry.getName());
                            CodeResurrector.showExceptionDialog("无法反编译文件", e);
                        } finally {
                            out.closeEntry();
                        }
                    }
                } else {
                    try {
                        JarEntry etn = new JarEntry(entry.getName());
                        if (entry.getName().endsWith(".java"))
                            etn = new JarEntry(entry.getName().replace(".java", ".src.java"));
                        if (history.add(etn.getName())) {
                            out.putNextEntry(etn);
                            try {
                                InputStream in = jfile.getInputStream(etn);
                                if (in != null) {
                                    try {
                                        int count;
                                        while ((count = in.read(data, 0, 1024)) != -1) {
                                            out.write(data, 0, count);
                                        }
                                    } finally {
                                        in.close();
                                    }
                                }
                            } finally {
                                out.closeEntry();
                            }
                        }
                    } catch (ZipException ze) {
                        if (!ze.getMessage().contains("duplicate")) {
                            throw ze;
                        }
                    }
                }
            }
        }
    }

    private void doSaveClassDecompiled(File inFile, File outFile) throws Exception {
        DecompilerSettings settings = cloneSettings();
        LuytenTypeLoader typeLoader = new LuytenTypeLoader();
        MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
        TypeReference type = metadataSystem.lookupType(inFile.getCanonicalPath());

        DecompilationOptions decompilationOptions = new DecompilationOptions();
        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);

        boolean isUnicodeEnabled = decompilationOptions.getSettings().isUnicodeOutputEnabled();
        TypeDefinition resolvedType = null;
        if (type == null || ((resolvedType = type.resolve()) == null)) {
            throw new Exception("Unable to resolve type.");
        }
        StringWriter stringwriter = new StringWriter();
        PlainTextOutput plainTextOutput = new PlainTextOutput(stringwriter);
        plainTextOutput.setUnicodeOutputEnabled(isUnicodeEnabled);
        settings.getLanguage().decompileType(resolvedType, plainTextOutput, decompilationOptions);
        String decompiledSource = stringwriter.toString();

        System.out.println("[SaveAll]: " + inFile.getName() + " -> " + outFile.getName());
        try (FileOutputStream fos = new FileOutputStream(outFile);
             OutputStreamWriter writer = isUnicodeEnabled ? new OutputStreamWriter(fos, "UTF-8")
                     : new OutputStreamWriter(fos);
             BufferedWriter bw = new BufferedWriter(writer);) {
            bw.write(decompiledSource);
            bw.flush();
        }
    }

    private void doSaveUnknownFile(File inFile, File outFile) throws Exception {
        try (FileInputStream in = new FileInputStream(inFile); FileOutputStream out = new FileOutputStream(outFile);) {
            System.out.println("[SaveAll]: " + inFile.getName() + " -> " + outFile.getName());

            byte[] data = new byte[1024 * 8];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                out.write(data, 0, count);
            }
        }
    }

    private DecompilerSettings cloneSettings() {
        DecompilerSettings settings = ConfigSaver.getLoadedInstance().getDecompilerSettings();
        DecompilerSettings newSettings = new DecompilerSettings();
        if (newSettings.getJavaFormattingOptions() == null) {
            newSettings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());
        }
        // 同步：防止主菜单更改
        synchronized (settings) {
            newSettings.setExcludeNestedTypes(settings.getExcludeNestedTypes());
            newSettings.setFlattenSwitchBlocks(settings.getFlattenSwitchBlocks());
            newSettings.setForceExplicitImports(settings.getForceExplicitImports());
            newSettings.setForceExplicitTypeArguments(settings.getForceExplicitTypeArguments());
            newSettings.setOutputFileHeaderText(settings.getOutputFileHeaderText());
            newSettings.setLanguage(settings.getLanguage());
            newSettings.setShowSyntheticMembers(settings.getShowSyntheticMembers());
            newSettings.setAlwaysGenerateExceptionVariableForCatchBlocks(
                    settings.getAlwaysGenerateExceptionVariableForCatchBlocks());
            newSettings.setOutputDirectory(settings.getOutputDirectory());
            newSettings.setRetainRedundantCasts(settings.getRetainRedundantCasts());
            newSettings.setIncludeErrorDiagnostics(settings.getIncludeErrorDiagnostics());
            newSettings.setIncludeLineNumbersInBytecode(settings.getIncludeLineNumbersInBytecode());
            newSettings.setRetainPointlessSwitches(settings.getRetainPointlessSwitches());
            newSettings.setUnicodeOutputEnabled(settings.isUnicodeOutputEnabled());
            newSettings.setMergeVariables(settings.getMergeVariables());
            newSettings.setShowDebugLineNumbers(settings.getShowDebugLineNumbers());
        }
        return newSettings;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isExtracting() {
        return extracting;
    }

    public void setExtracting(boolean extracting) {
        this.extracting = extracting;
    }
}
