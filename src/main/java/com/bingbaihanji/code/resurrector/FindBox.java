package com.bingbaihanji.code.resurrector;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.*;

public class FindBox extends JDialog {

    private static final long serialVersionUID = -4125409760166690462L;

    JCheckBox mcase;
    JCheckBox regex;
    JCheckBox wholew;
    JCheckBox reverse;
    JCheckBox wrap;
    JTextField textField;
    private JButton findButton;
    private MainWindow mainWindow;

    public FindBox(final MainWindow mainWindow) {
        super();
        setModalityType(ModalityType.MODELESS);

        this.mainWindow = mainWindow;

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setHideOnEscapeButton();

        // 创建主面板并设置为不透明
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(true);

        JLabel label = new JLabel("查找内容:");
        label.setOpaque(false);

        // 文本框
        textField = new JTextField();
        textField.setOpaque(true);

        RSyntaxTextArea pane = mainWindow.getSelectedModel().getCurrentTextArea();
        if (pane != null && pane.getSelectedText() != null) {
            textField.setText(pane.getSelectedText());
        }

        mcase = new JCheckBox("区分大小写");
        mcase.setOpaque(false);
        regex = new JCheckBox("正则表达式");
        regex.setOpaque(false);
        wholew = new JCheckBox("匹配整个单词");
        wholew.setOpaque(false);
        reverse = new JCheckBox("向后搜索");
        reverse.setOpaque(false);
        wrap = new JCheckBox("途中换行");
        wrap.setOpaque(false);

        findButton = new JButton("查找");
        findButton.setOpaque(true);
        findButton.addActionListener(new FindButton());
        getRootPane().setDefaultButton(findButton);

        // F3 / Shift+F3
        bindKey(KeyEvent.VK_F3, 0, "FindNext", new FindExploreAction(true));
        bindKey(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK, "FindPrevious", new FindExploreAction(false));

        clearCheckBoxBorder(mcase, regex, wholew, reverse, wrap);

        // ===== Layout =====
        GroupLayout layout = new GroupLayout(mainPanel);
        mainPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addComponent(label)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(textField)
                                .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                .addComponent(mcase)
                                                .addComponent(wholew)
                                                .addComponent(wrap))
                                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                .addComponent(regex)
                                                .addComponent(reverse))))
                        .addComponent(findButton)
        );

        layout.linkSize(SwingConstants.HORIZONTAL, findButton);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(label)
                                .addComponent(textField)
                                .addComponent(findButton))
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                .addComponent(mcase)
                                                .addComponent(regex))
                                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                .addComponent(wholew)
                                                .addComponent(reverse))
                                        .addComponent(wrap)))
        );

        // 将主面板添加到对话框
        getContentPane().add(mainPanel);

        adjustWindowPositionBySavedState();
        setSaveWindowPositionOnClosing();

        setTitle("查找");
        setName("查找");
        setResizable(false);

        // 设置大小
        setDialogBounds();

        // 强制更新UI
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void bindKey(int keyCode, int modifiers, String name, Action action) {
        KeyStroke ks = KeyStroke.getKeyStroke(keyCode, modifiers, false);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, name);
        getRootPane().getActionMap().put(name, action);
    }

    private void clearCheckBoxBorder(AbstractButton... buttons) {
        for (AbstractButton b : buttons) {
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setOpaque(false);
        }
    }

    private void setDialogBounds() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = new Dimension(
                (int) (screen.width * 0.35),
                Math.min((int) (screen.height * 0.20), 200)
        );
        setBounds(size.width / 5, size.height / 5, size.width, size.height);
    }

    public void showFindBox() {
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            textField.requestFocusInWindow();
            textField.selectAll();
        });
    }

    public void hideFindBox() {
        setVisible(false);
    }

    private void setHideOnEscapeButton() {
        Action escapeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        bindKey(KeyEvent.VK_ESCAPE, 0, "ESCAPE", escapeAction);
    }

    private void adjustWindowPositionBySavedState() {
        WindowPosition pos = ConfigSaver.getLoadedInstance().getFindWindowPosition();
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
                        .readPositionFromDialog(FindBox.this);
            }
        });
    }

    public void fireExploreAction(boolean forward) {
        fireSearch(forward);
    }

    private void fireSearch(boolean forward) {
        if (textField.getText().isEmpty()) return;

        RSyntaxTextArea pane = mainWindow.getSelectedModel().getCurrentTextArea();
        if (pane == null) return;

        SearchContext context = new SearchContext();
        context.setSearchFor(textField.getText());
        context.setMatchCase(mcase.isSelected());
        context.setRegularExpression(regex.isSelected());
        context.setWholeWord(wholew.isSelected());
        context.setSearchForward(forward);

        if (!SearchEngine.find(pane, context).wasFound()) {
            if (wrap.isSelected()) {
                pane.setSelectionStart(0);
                pane.setSelectionEnd(0);
            } else {
                mainWindow.getLabel().setText("搜索完成");
            }
        }
    }

    private class FindButton extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            fireSearch(!reverse.isSelected());
        }
    }

    class FindExploreAction extends AbstractAction {
        private final boolean forward;

        FindExploreAction(boolean forward) {
            this.forward = forward;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireSearch(forward);
        }
    }
}
