package io.kaitai.struct.visualizer;

import io.kaitai.struct.KaitaiStruct;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class DataNode extends DefaultMutableTreeNode {
    private boolean explored = false;
    private int depth;
    private Object value;
    private Method method;
    private String name;

    public DataNode(int depth, Object value, Method method, String name) {
        init(depth, value, method, name);
    }

    public DataNode(int depth, Object value, Method method) {
        init(depth, value, method, null);
    }

    private void init(int depth, Object value, Method method, String name) {
        this.depth = depth;
        this.value = value;
        this.method = method;
        if (name != null) {
            this.name = name;
        } else {
            this.name = method != null ? method.getName() : "?";
        }

        add(new DefaultMutableTreeNode("Loading...", false));
        setAllowsChildren(true);

        updateVisual();
    }

    private void updateVisual() {
        StringBuilder sb = new StringBuilder(name);
        if (value != null) {
            if (value instanceof byte[]) {
                sb.append(" = ");
                byte[] bytes = (byte[]) value;
                for (int i = 0; i < 10 && i < bytes.length; i++) {
                    sb.append(String.format("%02x ", bytes[i]));
                }
            } else if (value instanceof ArrayList || value instanceof KaitaiStruct) {
                // do not expand
            } else {
                sb.append(" = ");
                sb.append(value.toString());
            }
        }
        setUserObject(sb.toString());
    }

    private void setChildren(List<DataNode> children) {
        removeAllChildren();
        setAllowsChildren(children.size() > 0);
        for (MutableTreeNode node : children) {
            add(node);
        }
        explored = true;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public void explore(final DefaultTreeModel model, final PropertyChangeListener progressListener) {
        if (explored)
            return;

        SwingWorker<List<DataNode>, Void> worker = new SwingWorker<List<DataNode>, Void>() {
            @Override
            protected List<DataNode> doInBackground() throws Exception {
                // Here access database if needed
                setProgress(0);
                List<DataNode> children = new ArrayList<DataNode>();

                System.out.println("exploring " + value);

                // Wasn't loaded yet?
                if (value == null) {
                    DataNode parentNode = (DataNode) parent;
                    System.out.println("parentNode = " + parentNode);
                    value = method.invoke(parentNode.value);
                }

                // Still null?
                if (value == null) {
                    value = "[null]";
                    updateVisual();
                    return children;
                }

                Class<?> cl = value.getClass();
                System.out.println("cl = " + cl);

                if (isImmediate(value, cl)) {
                    updateVisual();
                    return children;
                }

                if (value instanceof ArrayList) {
                    ArrayList list = (ArrayList) value;
                    for (int i = 0; i < list.size(); i++) {
                        Object el = list.get(i);
                        String arrayIdxStr = String.format("%04d", i);
                        DataNode dn = new DataNode(depth + 1, el, null, arrayIdxStr);
                        children.add(dn);
                    }
                } else {
                    for (Method m : cl.getDeclaredMethods()) {
                        // Ignore static methods, i.e. "fromFile"
                        if (Modifier.isStatic(m.getModifiers()))
                            continue;

                        String methodName = m.getName();

                        // Ignore all internal methods
                        if (methodName.charAt(0) == '_')
                            continue;

                        try {
                            Field field = cl.getDeclaredField(methodName);
                            field.setAccessible(true);
                            Object curValue = field.get(value);

                            DataNode dn = new DataNode(depth + 1, curValue, m);
                            children.add(dn);
                        } catch (NoSuchFieldException e) {
                            System.out.println("no field, ignoring method " + methodName);
                        }
                    }
                }
                setProgress(0);
                return children;
            }

            @Override
            protected void done() {
                try {
                    setChildren(get());
                    model.nodeStructureChanged(DataNode.this);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Notify user of error.
                }
                super.done();
            }
        };
        if (progressListener != null) {
            worker.getPropertyChangeSupport().addPropertyChangeListener("progress", progressListener);
        }
        worker.execute();
    }

    public static boolean isImmediate(Object value, Class<?> cl) {
        return cl.isPrimitive() ||
                value instanceof Byte ||
                value instanceof Short ||
                value instanceof Integer ||
                value instanceof Long ||
                value instanceof Float ||
                value instanceof Double ||
                value instanceof String ||
                value instanceof Boolean ||
                value instanceof byte[];
    }
}
