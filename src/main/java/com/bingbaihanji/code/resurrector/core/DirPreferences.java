package com.bingbaihanji.code.resurrector.core;

import com.bingbaihanji.code.resurrector.config.DirectoryPreferences;
import com.bingbaihanji.code.resurrector.config.UserPreferences;
import com.bingbaihanji.code.resurrector.model.LuytenPreferences;

import javax.swing.*;

/**
 * @deprecated 请使用 {@link DirectoryPreferences}
 * 此类仅为向后兼容保留，将在未来版本中移除
 */
@Deprecated
public class DirPreferences {
    private final DirectoryPreferences delegate;

    public DirPreferences(LuytenPreferences luytenPrefs) {
        this.delegate = new DirectoryPreferences(luytenPrefs);
    }

    public DirPreferences(UserPreferences userPrefs) {
        this.delegate = new DirectoryPreferences(userPrefs);
    }

    public void retrieveOpenDialogDir(JFileChooser fc) {
        delegate.retrieveOpenDialogDir(fc);
    }

    public void saveOpenDialogDir(JFileChooser fc) {
        delegate.saveOpenDialogDir(fc);
    }

    public void retrieveSaveDialogDir(JFileChooser fc) {
        delegate.retrieveSaveDialogDir(fc);
    }

    public void saveSaveDialogDir(JFileChooser fc) {
        delegate.saveSaveDialogDir(fc);
    }
}