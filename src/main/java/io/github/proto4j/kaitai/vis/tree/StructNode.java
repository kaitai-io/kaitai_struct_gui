package io.github.proto4j.kaitai.vis.tree; //@date 28.07.2023

import cms.rendner.hexviewer.common.ranges.ByteRange;
import io.kaitai.struct.KaitaiStruct;

import javax.swing.tree.TreeNode;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * A node that corresponds to a parsed Kaitai Struct.
 */
public class StructNode extends KaitaiTreeNode {

    // The Kaitai Struct object associated with the StructNode
    private final KaitaiStruct kaitaiStruct;

    private final List<Method> fields;
    private final List<Method> instances;

    private final Map<String, Integer> attributeStart;
    private final Map<String, Integer> attributeEnd;

    private final Map<String, ? extends List<Integer>> arrayStart;
    private final Map<String, ? extends List<Integer>> arrayEnd;

    // Cached child nodes created from fields and instances of the structure
    private List<KaitaiTreeNode> children;
    // State of the StructNode in the StructTreeModel (unused)
    private StructTreeModel.State state;

    /**
     * Constructor for creating a StructNode instance representing the entire structure.
     *
     * @param name         The name of the StructNode.
     * @param parent       The parent node of the StructNode.
     * @param kaitaiStruct The Kaitai Struct object associated with the StructNode.
     * @param state        The state of the StructNode in the StructTreeModel.
     * @throws ReflectiveOperationException If an error occurs while initializing the StructNode.
     */
    public StructNode(String name, TreeNode parent, KaitaiStruct kaitaiStruct, StructTreeModel.State state)
            throws ReflectiveOperationException {
        this(name, parent, new ByteRange(0L, kaitaiStruct._io().pos()), kaitaiStruct, state);
    }

    /**
     * Constructor for creating a StructNode instance representing a specific portion of the structure.
     *
     * @param name         The name of the StructNode.
     * @param parent       The parent node of the StructNode.
     * @param span         The ByteRange representing the span of the structure in the original data.
     * @param kaitaiStruct The Kaitai Struct object associated with the StructNode.
     * @param state        The state of the StructNode in the StructTreeModel.
     * @throws ReflectiveOperationException If an error occurs while initializing the StructNode.
     */
    @SuppressWarnings("unchecked")
    public StructNode(String name, TreeNode parent, ByteRange span, KaitaiStruct kaitaiStruct, StructTreeModel.State state)
            throws ReflectiveOperationException {
        super(name, parent, kaitaiStruct, span);
        this.kaitaiStruct = kaitaiStruct;
        this.state = state;

        final Class<?> type = kaitaiStruct.getClass();
        final String[] names = (String[]) type.getField("_seqFields").get(null);

        // Separate methods representing fields and instances
        List<String> order = Arrays.asList(names);
        this.instances = new ArrayList<>();
        this.fields = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            // Skip static methods, i.e., "fromFile"
            // Skip all internal methods, i.e., "_io", "_parent", "_root"
            if (Modifier.isStatic(method.getModifiers()) || method.getName().charAt(0) == '_') {
                continue;
            }

            if (order.contains(method.getName())) {
                fields.add(method);
            } else {
                instances.add(method);
            }
        }

        // Sort fields based on the order in the structure
        fields.sort((o1, o2) -> {
            final int pos1 = order.indexOf(o1.getName());
            final int pos2 = order.indexOf(o2.getName());
            return pos1 - pos2;
        });

        this.children = null;
        this.attributeStart = (Map<String, Integer>) type.getDeclaredField("_attrStart").get(kaitaiStruct);
        this.attributeEnd = (Map<String, Integer>) type.getDeclaredField("_attrEnd").get(kaitaiStruct);
        this.arrayStart = (Map<String, ? extends List<Integer>>) type.getDeclaredField("_arrStart").get(kaitaiStruct);
        this.arrayEnd = (Map<String, ? extends List<Integer>>) type.getDeclaredField("_arrEnd").get(kaitaiStruct);
    }

    /**
     * Get the type of the StructNode, which is always STRUCT.
     *
     * @return The Type of the StructNode (STRUCT).
     */
    @Override
    public Type getType() {
        return Type.STRUCT;
    }

    /**
     * Get the state of the StructNode in the StructTreeModel.
     *
     * @return The state of the StructNode.
     */
    public StructTreeModel.State getState() {
        return state;
    }

    /**
     * Set the state of the StructNode in the StructTreeModel.
     *
     * @param state The new state to be set for the StructNode.
     */
    public void setState(StructTreeModel.State state) {
        this.state = state;
    }

    /**
     * Get the Kaitai Struct object associated with the StructNode.
     *
     * @return The Kaitai Struct object associated with the StructNode.
     */
    @Override
    public KaitaiStruct getValue() {
        return kaitaiStruct;
    }

    /**
     * Get the child node at the specified childIndex.
     *
     * @param childIndex The index of the child node to retrieve.
     * @return The child node at the specified index.
     */
    @Override
    public KaitaiTreeNode getChildAt(int childIndex) {
        return this.getChildren().get(childIndex);
    }

    /**
     * Get the number of children nodes of the StructNode.
     *
     * @return The number of children nodes.
     */
    @Override
    public int getChildCount() {
        return this.getChildren().size();
    }

    /**
     * Get the index of the specified child node.
     *
     * @param node The child node for which to find the index.
     * @return The index of the specified child node.
     */
    @Override
    public int getIndex(TreeNode node) {
        return this.getChildren().indexOf((KaitaiTreeNode) node);
    }

    /**
     * Check if StructNode allows children nodes.
     *
     * @return True, indicating that StructNode allows children.
     */
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * Check if the StructNode is a leaf node.
     *
     * @return True if the StructNode has no children (fields and instances), indicating that it is a leaf node.
     */
    @Override
    public boolean isLeaf() {
        return this.getChildren().isEmpty();
    }

    /**
     * Get an enumeration of StructNode's children nodes.
     *
     * @return An enumeration of StructNode's children nodes.
     */
    @Override
    public Enumeration<? extends TreeNode> children() {
        return Collections.enumeration(this.getChildren());
    }

    /**
     * Get the child nodes corresponding to the fields and instances within the structure.
     *
     * @return A list of KaitaiTreeNode representing the child nodes of the StructNode.
     */
    public List<KaitaiTreeNode> getChildren() {
        if (this.children == null) {
            children = new ArrayList<>();
            try {
                // Create child nodes for fields
                for (Method method : this.fields) {
                    children.add(createNode(method));
                }
                // Create child nodes for instances
                for (Method method : this.instances) {
                    children.add(createNode(method));
                }
            } catch (ReflectiveOperationException e) {
                // REVISIT: Handle any reflective operation exception
            }
        }
        return children;
    }

    /**
     * Create a child node based on the given accessor method representing a field or instance.
     *
     * @param accessor The method representing the field or instance accessor.
     * @return A KaitaiTreeNode representing the created child node.
     * @throws ReflectiveOperationException If an error occurs during reflective operations.
     */
    private KaitaiTreeNode createNode(Method accessor) throws ReflectiveOperationException {
        // Invoke the accessor method to get the value of the field or instance
        Object value = accessor.invoke(kaitaiStruct);
        String name = accessor.getName();

        // Optional field could be not presented in the maps if it's missing in input
        // "value" instances don't present in the maps
        Integer start = attributeStart.get(name);
        Integer end = attributeEnd.get(name);
        boolean present = start != null && end != null;

        ByteRange current = getSpan();
        if (current != null && present) {
            if (current.getStart() > start) {
                start += Long.valueOf(current.getStart()).intValue();
                end += Long.valueOf(current.getStart()).intValue();
            }
        }

        ByteRange span = present ? new ByteRange(start, end) : null;

        if (present && List.class.isAssignableFrom(accessor.getReturnType())) {
            // Handle arrays or lists
            Integer[] startOffsets = arrayStart.get(name).toArray(new Integer[0]);
            Integer[] endOffsets = arrayEnd.get(name).toArray(new Integer[0]);

            // We are sure that the return type is a generic with one Class parameter,
            // because KaitaiStruct java generator generates fields/methods with an ArrayList<XXX> static type
            ParameterizedType returnType = (ParameterizedType) accessor.getGenericReturnType();
            java.lang.reflect.Type elementType = returnType.getActualTypeArguments()[0];

            return new ArrayNode(name, this, value, span, (Class<?>) elementType, startOffsets, endOffsets);
        }

        // For regular fields or instances, create a SimpleNode or StructNode as child node
        return create(name, value, accessor.getReturnType(), span);
    }
}
