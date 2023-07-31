package io.github.proto4j.kaitai.vis.tree;//@date 29.07.2023

import java.awt.*;

/**
 * The CategorizedNode interface represents categorized nodes, providing information
 * about their type and color.
 *
 * @see Type
 */
public interface CategorizedNode {

    /**
     * Get the type of the categorized node.
     *
     * @return The {@link Type} of the categorized node (STRUCT, ENUM, VALUE, ARRAY).
     */
    Type getType();

    /**
     * Get the color associated with the categorized node.
     *
     * @return The {@link Color} of the categorized node.
     */
    Color getColor();

    /**
     * Specifies the possible types of categorized nodes.
     */
    enum Type {
        /**
         * Represents a structured node, typically containing other nodes.
         */
        STRUCT,

        /**
         * Represents an enumeration node, which contains a set of named values.
         */
        ENUM,

        /**
         * Represents a single value node, such as a primitive value or a constant.
         * For example, a numeric value or a string.
         */
        VALUE,

        /**
         * Represents an array node, which contains a collection of elements.
         * For example, an array or a list.
         */
        ARRAY
    }
}

