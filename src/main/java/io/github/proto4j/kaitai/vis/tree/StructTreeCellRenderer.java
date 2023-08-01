package io.github.proto4j.kaitai.vis.tree;//@date 28.07.2023

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public abstract class StructTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof KaitaiTreeNode) {
            KaitaiTreeNode node = (KaitaiTreeNode) value;
            Icon layeredIcon = this.getIconForNode(node);
            if (layeredIcon != null) {
                setIcon(layeredIcon);
            }
        }
        return this;
    }

    protected abstract Icon getIconForNode(KaitaiTreeNode node);
}
