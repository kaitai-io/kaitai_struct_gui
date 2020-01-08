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
    private final int depth;
    private Object value;
    private final Method method;
    private final String name;
    private final Integer posStart;
    private final Integer posEnd;

    public DataNode(int depth, Object value, String name) {
        this(depth, value, null, name, null, null);
    }

    private DataNode(int depth, Object value, Method method, Integer posStart, Integer posEnd) {
        this(depth, value, method, null, posStart, posEnd);
    }

    private DataNode(int depth, Object value, Method method, String name, Integer posStart, Integer posEnd) {
        this.depth = depth;
        this.value = value;
        this.method = method;
        if (name != null) {
            this.name = name;
        } else {
            this.name = method != null ? method.getName() : "?";
        }
        this.posStart = posStart;
        this.posEnd = posEnd;

        add(new DefaultMutableTreeNode("Loading...", false));
        setAllowsChildren(true);

        updateVisual();
    }

    public Integer posStart() {
        return posStart;
    }

    public Integer posEnd() {
        return posEnd;
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
            } else if (value instanceof ArrayList) {
                ArrayList list = (ArrayList) value;
                sb.append(String.format(" (%d = 0x%x entries)", list.size(), list.size()));
            } else if (value instanceof KaitaiStruct) {
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
                final List<DataNode> children = new ArrayList<>();

                System.out.println("exploring field " + name + ", value = " + value);

                // Wasn't loaded yet?
                if (value == null) {
                    DataNode parentNode = (DataNode) parent;
                    System.out.println("parentNode: name = " + parentNode.name + "; value = " + parentNode.value);
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

                        children.add(new DataNode(depth + 1, el, arrayIdxStr));
                    }
                } else if (value instanceof KaitaiStruct) {
                    DebugAids debug = DebugAids.fromStruct((KaitaiStruct) value);

                    for (Method m : cl.getDeclaredMethods()) {
                        // Ignore static methods, i.e. "fromFile"
                        if (Modifier.isStatic(m.getModifiers()))
                            continue;

                        String methodName = m.getName();

                        // Ignore all internal methods, i.e. "_io", "_parent", "_root"
                        if (methodName.charAt(0) == '_')
                            continue;

                        try {
                            Field field = cl.getDeclaredField(methodName);
                            field.setAccessible(true);
                            Object curValue = field.get(value);

                            Integer posStart = debug.getStart(methodName);
                            Integer posEnd = debug.getEnd(methodName);

                            DataNode dn = new DataNode(depth + 1, curValue, m, posStart, posEnd);
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
