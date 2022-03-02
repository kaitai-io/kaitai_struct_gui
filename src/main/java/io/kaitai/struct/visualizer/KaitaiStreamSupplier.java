package io.kaitai.struct.visualizer;

import io.kaitai.struct.ByteBufferKaitaiStream;

import java.io.IOException;

/**
 * Basically a <code>Supplier&lt;ByteBufferKaitaiStream&gt;</code> which throws an <code>IOException</code>.<br>
 *
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html</a>
 */
@FunctionalInterface
public interface KaitaiStreamSupplier {
    ByteBufferKaitaiStream getStream() throws IOException;
}
