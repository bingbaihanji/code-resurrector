package com.bingbaihanji.core.resurrector.util;

import com.bingbaihanji.core.resurrector.model.TreeNodeUserObject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.HashSet;
import java.util.Set;

/**
 * 树节点工具类
 * 用于保存和恢复 JTree 的展开状态
 */
public class TreeNodeUtils {

    private JTree tree;

    public TreeNodeUtils() {
    }

    public TreeNodeUtils(JTree tree) {
        this.tree = tree;
    }

    /**
     * 获取当前树的展开状态
     *
     * @return 展开的节点路径集合
     */
    public Set<String> getExpansionState() {
        Set<String> openedSet = new HashSet<>();
        if (tree != null) {
            int rowCount = tree.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                TreePath path = tree.getPathForRow(i);
                if (tree.isExpanded(path)) {
                    String rowPathStr = getRowPathStr(path);
                    // 为切换包浏览器开/关功能
                    openedSet.addAll(getAllParentPathsStr(rowPathStr));
                }
            }
        }
        return openedSet;
    }

    /**
     * 获取节点路径的所有父路径
     *
     * @param rowPathStr 节点路径字符串
     * @return 所有父路径集合
     */
    private Set<String> getAllParentPathsStr(String rowPathStr) {
        Set<String> parents = new HashSet<>();
        parents.add(rowPathStr);
        if (rowPathStr.contains("/")) {
            String[] pathElements = rowPathStr.split("/");
            String path = "";
            for (String pathElement : pathElements) {
                path = path + pathElement + "/";
                parents.add(path);
            }
        }
        return parents;
    }

    /**
     * 恢复树的展开状态
     *
     * @param expansionState 之前保存的展开状态
     */
    public void restoreExpanstionState(Set<String> expansionState) {
        if (tree != null && expansionState != null) {
            // tree.getRowCount() 在 tree.expandRow() 时会改变
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath path = tree.getPathForRow(i);
                if (expansionState.contains(getRowPathStr(path))) {
                    tree.expandRow(i);
                }
            }
        }
    }

    /**
     * 获取树路径的字符串表示
     *
     * @param trp 树路径
     * @return 路径字符串
     */
    private String getRowPathStr(TreePath trp) {
        StringBuilder pathStr = new StringBuilder();
        if (trp.getPathCount() > 1) {
            for (int i = 1; i < trp.getPathCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) trp.getPathComponent(i);
                TreeNodeUserObject userObject = (TreeNodeUserObject) node.getUserObject();
                pathStr.append(userObject.getOriginalName()).append("/");
            }
        }
        return pathStr.toString();
    }

    public JTree getTree() {
        return tree;
    }

    public void setTree(JTree tree) {
        this.tree = tree;
    }
}
