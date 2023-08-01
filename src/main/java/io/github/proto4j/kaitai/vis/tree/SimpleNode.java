package io.github.proto4j.kaitai.vis.tree; //@date 28.07.2023

import cms.rendner.hexviewer.common.ranges.ByteRange;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Enumeration;
/**
 * The SimpleNode class represents a leaf node in the Kaitai Struct visualization
 * tool's tree hierarchy.
 */
public class SimpleNode extends KaitaiTreeNode {

    // The class of the value associated with the SimpleNode
    private final Class<?> valueType;

    /**
     * Constructor for creating a SimpleNode instance.
     *
     * @param name      The name of the SimpleNode.
     * @param parent    The parent node of the SimpleNode.
     * @param value     The value associated with the SimpleNode.
     * @param span      The ByteRange representing the span of the value in the original data.
     * @param valueType The Class object representing the type of the value.
     */
    public SimpleNode(String name, TreeNode parent, Object value, ByteRange span, Class<?> valueType) {
        super(name, parent, value, span);
        this.valueType = valueType;
    }

    /**
     * Get the type of the value associated with the SimpleNode.
     *
     * @return The Class object representing the type of the value.
     */
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * Get the child node at the specified childIndex.
     * Since SimpleNode is a leaf node, it throws an IndexOutOfBoundsException as it has no children.
     *
     * @param childIndex The index of the child node to retrieve.
     * @return The child node at the specified index.
     * @throws IndexOutOfBoundsException Always thrown since SimpleNode has no children.
     */
    @Override
    public TreeNode getChildAt(int childIndex) {
        throw new IndexOutOfBoundsException("SimpleNode has no children!");
    }

    /**
     * Get the number of children nodes of the SimpleNode.
     * Since SimpleNode is a leaf node, it always returns 0.
     *
     * @return The number of children nodes (always 0 for SimpleNode).
     */
    @Override
    public int getChildCount() {
        return 0;
    }

    /**
     * Get the index of the specified child node.
     * <p>
     * Since SimpleNode has no children, it always returns -1, indicating
     * that the node is not a child of SimpleNode.
     *
     * @param node The child node for which to find the index.
     * @return The index of the specified child node (always -1 for SimpleNode).
     */
    @Override
    public int getIndex(TreeNode node) {
        return -1;
    }

    /**
     * Check if the SimpleNode is a leaf node.
     * Since SimpleNode has no children, it always returns true.
     *
     * @return True, indicating that SimpleNode is a leaf node.
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

    /**
     * Get an enumeration of SimpleNode's children nodes.
     * Since SimpleNode has no children, it returns an empty enumeration.
     *
     * @return An enumeration of SimpleNode's children nodes (always empty).
     */
    @Override
    public Enumeration<? extends TreeNode> children() {
        return Collections.emptyEnumeration();
    }

    /**
     * Check if SimpleNode allows children nodes.
     * Since SimpleNode is a leaf node and has no children, it always returns false.
     *
     * @return False, indicating that SimpleNode does not allow children.
     */
    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Get the type of the SimpleNode, either VALUE or ENUM based on the type of the value associated with it.
     *
     * @return The Type of the SimpleNode (VALUE or ENUM).
     */
    @Override
    public Type getType() {
        // Determine the Type of the SimpleNode based on the type of the value
        if (value instanceof Enum) {
            return Type.ENUM;
        }
        return Type.VALUE;
    }
}
