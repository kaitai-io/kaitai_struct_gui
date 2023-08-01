package io.github.proto4j.kaitai.vis.tree;//@date 29.07.2023

import cms.rendner.hexviewer.common.ranges.ByteRange;
import io.github.proto4j.kaitai.vis.util.ColorSpec;
import io.kaitai.struct.KaitaiStruct;

import javax.swing.tree.TreeNode;
import java.awt.*;
import java.lang.reflect.Array;

/**
 * The abstract class KaitaiTreeNode serves as the base implementation for tree nodes
 * in the Kaitai Struct visualization tool.
 *
 * @see ArrayNode
 * @see StructNode
 * @see SimpleNode
 */
public abstract class KaitaiTreeNode implements TreeNode, CategorizedNode {

    // Common properties for tree nodes
    protected final String name;
    protected final TreeNode parent;
    protected final Object value;
    protected final ByteRange span;

    protected Color color;

    /**
     * Basic constructor for creating a KaitaiTreeNode instance.
     *
     * @param name   The name of the tree node.
     * @param parent The parent node of the tree node.
     * @param value  The value associated with the tree node.
     * @param span   The ByteRange representing the span of the tree node's value in the original data.
     */
    public KaitaiTreeNode(String name, TreeNode parent, Object value, ByteRange span) {
        this.name = name;
        this.parent = parent;
        this.value = value;
        this.span = span;
        this.color = ColorSpec.random();
    }

    /**
     * Convert the value of a KaitaiTreeNode to a string representation.
     * <p>
     * The following list tries to illustrate how different value types will be
     * represented:
     * <li> Array: {@code <class.name>[<length>]}</li>
     * <li> Struct: {@code struct <class.name>}</li>
     * <li> Value: {@code String.valueOf(<node.value>)}</li>
     * <li> Enum: {@code <enum.class.name>::<value.name> (<value.enum_value>)}</li>
     *
     * @param node The KaitaiTreeNode for which to convert the value to a string.
     * @return A string representation of the node's value.
     */
    public static String valueToString(KaitaiTreeNode node) {
        if (node instanceof ArrayNode) {
            String name = ((ArrayNode) node).getValueType().getSimpleName();
            return String.format("%s[%d]", name, node.getChildCount());
        } else if (node instanceof StructNode) {
            String name = node.getValue().getClass().getSimpleName();
            return "struct " + name;
        } else {
            SimpleNode simpleNode = (SimpleNode) node;
            if (simpleNode.getType() == Type.VALUE) {
                Object value = simpleNode.getValue();
                String format = String.valueOf(value);
                if (value instanceof byte[]) {
                    byte[] data = (byte[]) value;
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    int maxIter = Math.min(data.length, 8);

                    for (int i = 0; i < maxIter; ++i) {
                        sb.append(String.format("0x%02X", data[i]));
                        if (i < maxIter - 1) {
                            sb.append(", ");
                        } else if (i == maxIter - 1 && data.length > maxIter) {
                            sb.append(", ...");
                        }
                    }

                    sb.append("]");
                    format = sb.toString();
                }

                return simpleNode.getValueType().getSimpleName() + " = " + format;
            }
            // Enum Type is a bit special
            Class<? /*extends Enum<?>*/> enumClass = simpleNode.getValueType();
            try {
                Object values = enumClass.getMethod("values").invoke(null);
                for (int i = 0; i < Array.getLength(values); i++) {
                    Enum<?> value = (Enum<?>) Array.get(values, i);
                    if (((Enum<?>) simpleNode.value).ordinal() == value.ordinal()) {
                        return String.format("%s::%s (%#x)",
                                enumClass.getSimpleName(), value.name(),
                                value.ordinal());
                    }
                }
                return String.format("%s::??? (%s)", enumClass.getSimpleName(), simpleNode.value);
            } catch (Exception e) {
                return String.valueOf(simpleNode.getValue());
            }
        }
    }

    /**
     * Get the color associated with the tree node.
     *
     * @return The Color of the tree node.
     */
    @Override
    public Color getColor() {
        return color;
    }

    /**
     * Get the parent node of the tree node.
     *
     * @return The parent TreeNode of the tree node.
     */
    @Override
    public TreeNode getParent() {
        return parent;
    }

    /**
     * Get the value associated with the tree node.
     *
     * @return The value of the tree node.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Get the name of the tree node.
     *
     * @return The name of the tree node.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the ByteRange representing the span of the tree node's value in the original data.
     *
     * @return The ByteRange representing the span of the tree node's value.
     */
    public ByteRange getSpan() {
        return span;
    }

    /**
     * Create a new KaitaiTreeNode instance based on the provided parameters.
     * This method is used to create child nodes of the current node.
     *
     * @param name       The name of the new node.
     * @param value      The value of the new node.
     * @param valueType  The type of the value (class) of the new node.
     * @param span       The ByteRange representing the span of the new node's value in the original data.
     * @return A new KaitaiTreeNode instance representing the child node.
     * @throws ReflectiveOperationException If there is an error during the creation of the new node.
     */
    protected final KaitaiTreeNode create(String name, Object value, Class<?> valueType, ByteRange span) throws ReflectiveOperationException {
        return value instanceof KaitaiStruct
                ? new StructNode(name, this, span, (KaitaiStruct) value, StructTreeModel.State.SHOW)
                : new SimpleNode(name, this, value, span, valueType);

    }

    /**
     * Convert the tree node and its value to a string representation.
     *
     * @return A string representing the tree node and its value.
     */
    @Override
    public String toString() {
        return String.format("%s: %s", getName(), valueToString(this));
    }

}
