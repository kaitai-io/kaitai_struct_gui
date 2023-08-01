package io.github.proto4j.kaitai.vis.tree; //@date 28.07.2023

import io.kaitai.struct.KaitaiStruct;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class StructTreeModel implements TreeModel {

    protected Object root;
    protected EventListenerList listenerList = new EventListenerList();

    public StructTreeModel(Object root) {
        this.root = root;
    }

    public StructTreeModel(String name, KaitaiStruct struct) throws ReflectiveOperationException {
        this(new StructNode(name, null, struct, State.SHOW));
    }

    //
    // Default implementations for methods in the TreeModel interface.
    //

    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public int getIndexOfChild(Object parent, Object child) {
        for (int i = 0; i < getChildCount(parent); i++) {
            if (getChild(parent, i).equals(child)) {
                return i;
            }
        }
        return -1;
    }

    public void addTreeModelListener(TreeModelListener l) {
        listenerList.add(TreeModelListener.class, l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listenerList.remove(TreeModelListener.class, l);
    }

    @Override
    public StructNode getRoot() {
        return (StructNode) root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return parent instanceof TreeNode ? ((TreeNode) parent).getChildAt(index) : null;
    }

    @Override
    public int getChildCount(Object parent) {
        return parent instanceof TreeNode ? ((TreeNode) parent).getChildCount() : 0;
    }

    // TODO: Remove this enum
    public enum State {
        SHOW,
        HIDE
    }
}
