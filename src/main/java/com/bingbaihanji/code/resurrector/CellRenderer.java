package com.bingbaihanji.code.resurrector;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class CellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = -5691181006363313993L;
    Icon pack;
    Icon java_image;
    Icon yml_image;
    Icon file_image;

    public CellRenderer() {
        this.pack = IconManager.loadPackageIcon();
        this.java_image = IconManager.loadJavaIcon();
        this.yml_image = IconManager.loadYmlIcon();
        this.file_image = IconManager.loadFileIcon();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
                                                  int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (node.getChildCount() > 0) {
            setIcon(this.pack);
        } else if (getFileName(node).endsWith(".class") || getFileName(node).endsWith(".java")) {
            setIcon(this.java_image);
        } else if (getFileName(node).endsWith(".yml") || getFileName(node).endsWith(".yaml")) {
            setIcon(this.yml_image);
        } else {
            setIcon(this.file_image);
        }

        return this;
    }

    public String getFileName(DefaultMutableTreeNode node) {
        return ((TreeNodeUserObject) node.getUserObject()).getOriginalName();
    }

}
