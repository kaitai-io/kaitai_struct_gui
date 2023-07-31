package io.github.proto4j.kaitai.vis.tree; //@date 28.07.2023

import cms.rendner.hexviewer.common.ranges.ByteRange;

import javax.swing.tree.TreeNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * The ArrayNode class represents an array node in the Kaitai Struct visualization
 * tool's tree hierarchy that contains a collection of elements.
 */
public class ArrayNode extends KaitaiTreeNode {

    // The class of the elements in the array
    private final Class<?> valueType;

    // Start offsets for each element in the array
    private final Integer[] startOffsets;

    // End offsets for each element in the array
    private final Integer[] endOffsets;

    // Cached child nodes created from the array elements
    private KaitaiTreeNode[] children;

    /**
     * Constructor for creating an ArrayNode instance.
     *
     * @param name         The name of the ArrayNode.
     * @param parent       The parent node of the ArrayNode.
     * @param value        The value associated with the ArrayNode (a list representing the array).
     * @param span         The ByteRange representing the span of the array in the original data.
     * @param valueType    The Class object representing the type of the elements in the array.
     * @param startOffsets The start offsets for each element in the array.
     * @param endOffsets   The end offsets for each element in the array.
     */
    public ArrayNode(String name,
                     TreeNode parent,
                     Object value,
                     ByteRange span,
                     Class<?> valueType,
                     Integer[] startOffsets,
                     Integer[] endOffsets) {
        super(name, parent, value, span);
        this.valueType = valueType;
        this.startOffsets = startOffsets;
        this.endOffsets = endOffsets;
    }

    /**
     * Get the List representation of the array value associated with the ArrayNode.
     *
     * @return The List representation of the array value.
     */
    @Override
    public List<?> getValue() {
        return (List<?>) value;
    }

    /**
     * Get the type of the elements in the array associated with the ArrayNode.
     *
     * @return The Class object representing the type of the elements in the array.
     */
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * Get the child node at the specified childIndex.
     *
     * @param childIndex The index of the child node to retrieve.
     * @return The child node at the specified index.
     */
    @Override
    public KaitaiTreeNode getChildAt(int childIndex) {
        return getChildren()[childIndex];
    }

    /**
     * Get the number of children nodes of the ArrayNode.
     *
     * @return The number of children nodes.
     */
    @Override
    public int getChildCount() {
        return getChildren().length;
    }

    /**
     * Get the index of the specified child node.
     *
     * @param node The child node for which to find the index.
     * @return The index of the specified child node.
     */
    @Override
    public int getIndex(TreeNode node) {
        return Arrays.asList(getChildren()).indexOf((KaitaiTreeNode) node);
    }

    /**
     * Get an enumeration of ArrayNode's children nodes.
     *
     * @return An enumeration of ArrayNode's children nodes.
     */
    @Override
    public Enumeration<? extends TreeNode> children() {
        return Collections.enumeration(List.of(getChildren()));
    }

    /**
     * Check if ArrayNode allows children nodes.
     *
     * @return True, indicating that ArrayNode allows children.
     */
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * Check if the ArrayNode is a leaf node.
     *
     * @return True if the ArrayNode has no elements (is empty), indicating that it is a leaf node.
     */
    @Override
    public boolean isLeaf() {
        return getValue().isEmpty();
    }

    /**
     * Get the child nodes corresponding to the elements of the array.
     * <p>
     * If the child nodes have not been created yet, this method will create
     * them and cache them.
     *
     * @return An array of KaitaiTreeNode representing the child nodes of the ArrayNode.
     */
    public KaitaiTreeNode[] getChildren() {
        if (children == null) {
            // If the children have not been created yet, create them now and cache them
            children = new KaitaiTreeNode[getValue().size()];
            for (int i = 0; i < getValue().size(); i++) {
                int start = startOffsets[i];
                int end = endOffsets[i];
                final ByteRange current = getSpan();
                if (start < current.getStart()) {
                    start += current.getStart();
                    end += current.getStart();
                }

                final ByteRange span = new ByteRange(start, end);
                final String name = String.format("[%d]", i); // special name for array elements
                Object value = this.getValue().get(i);
                try {
                    // Create a child node based on the element value, valueType, and span
                    children[i] = create(name, value, valueType, span);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return children;
    }

    /**
     * Get the type of the ArrayNode, which is always ARRAY.
     *
     * @return The Type of the ArrayNode (ARRAY).
     */
    @Override
    public Type getType() {
        return Type.ARRAY;
    }
}
