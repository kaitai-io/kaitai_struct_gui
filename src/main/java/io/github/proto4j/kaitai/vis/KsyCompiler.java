package io.github.proto4j.kaitai.vis; //@date 30.07.2023

import io.kaitai.struct.*;
import io.kaitai.struct.format.ClassSpec;
import io.kaitai.struct.format.KSVersion;
import io.kaitai.struct.formats.JavaClassSpecs;
import io.kaitai.struct.formats.JavaKSYParser;
import io.kaitai.struct.languages.JavaCompiler$;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The KsyCompiler class is responsible for compiling Kaitai Struct (.ksy) files
 * into Java classes and creating instances of those classes.
 * <p>
 * Below are some usage examples of this class:
 * <ol>
 *     <li>Compiling a Kaitai Struct (.ksy) file to Java source code and obtaining
 *         the generated code:
 *         <pre>{@code
 *          String ksyFilePath = "path/to/your/file.ksy";
 *          String javaSourceCode = KsyCompiler.compileToJava(ksyFilePath);
 *          System.out.println(javaSourceCode);
 *         }</pre>
 *     </li>
 *     <li>Creating a new class from Java source code:
 *     <pre>{@code
 *          String javaSourceCode = "public class MyKaitaiStruct extends KaitaiStruct { ... }";
 *          Class<? extends KaitaiStruct> kaitaiClass = KsyCompiler.createClass(javaSourceCode);
 *     }</pre>
 *     </li>
 *     <li>Extracting the class name from Java source code:
 *     <pre>{@code
 *          Class<? extends KaitaiStruct> kaitaiClass = ...; // Get the class object somehow
 *          KaitaiStream stream = ...; // Provide the KaitaiStream
 *          KaitaiStruct instance = KsyCompiler.newInstance(kaitaiClass, stream);
 *     }</pre>
 *     </li>
 * </ol>
 *
 * @see KaitaiStruct
 * @see InMemoryJavaCompiler
 */
public final class KsyCompiler {
    // The default package name for the generated Java classes
    public static final String PACKAGE = "io.kaitai.struct.visualized";

    /**
     * Compile the given Kaitai Struct (.ksy) file into Java source code and return it as a string.
     *
     * @param ksyFilePath The path to the Kaitai Struct (.ksy) file to be compiled.
     * @return The Java source code generated from the .ksy file.
     */
    public static String compileToJava(String ksyFilePath) {
        KSVersion.current_$eq(Version.version());
        ClassSpec spec = JavaKSYParser.fileNameToSpec(ksyFilePath);
        JavaClassSpecs javaClassSpecs = new JavaClassSpecs(null, null, spec);

        final RuntimeConfig config = new RuntimeConfig(
                false,// autoRead - do not call `_read` automatically in constructor
                true, // readStoresPos - enable generation of a position info which is accessed in DebugAids later
                true, // opaqueTypes
                null, // cppConfig
                null, // goPackage
                new JavaRuntimeConfig(
                        PACKAGE,
                        // Class to be invoked in `fromFile` helper methods
                        "io.kaitai.struct.ByteBufferKaitaiStream",
                        // Exception class expected to be thrown on end-of-stream errors
                        "java.nio.BufferUnderflowException"
                ),
                null, // dotNetNamespace
                null, // phpNamespace
                null, // pythonPackage
                null, // nimModule
                null  // nimOpaque
        );

        Main.importAndPrecompile(javaClassSpecs, config);
        CompileLog.SpecSuccess result = Main.compile(javaClassSpecs, spec, JavaCompiler$.MODULE$, config);
        CompileLog.FileSuccess file = result.files().apply(0);
        return file.contents();
    }

    /**
     * Create a new class from the given Java source code and return its class object.
     *
     * @param javaSourceCode The Java source code for the class to be created.
     * @return The Class object representing the newly created class.
     * @throws Exception If there is an error during the class creation process.
     */
    public static Class<? extends KaitaiStruct> createClass(String javaSourceCode)
            throws Exception {
        return createClass(javaSourceCode, getClassName(javaSourceCode));
    }

    public static Class<? extends KaitaiStruct> createClass(String javaSourceCode,  String simpleName)
            throws Exception {
        return createClass(javaSourceCode, PACKAGE, simpleName);
    }

    /**
     * Create a new class from the given Java source code with the specified simple name
     * and return its class object.
     *
     * @param javaSourceCode The Java source code for the class to be created.
     * @param simpleName     The simple name to be used for the newly created class.
     * @return The Class object representing the newly created class.
     * @throws Exception If there is an error during the class creation process.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends KaitaiStruct> createClass(String javaSourceCode, String packageName, String simpleName)
            throws Exception {
        final String name = String.join(".", packageName, simpleName);
        final Class<?> cls = InMemoryJavaCompiler.newInstance().compile(name, javaSourceCode);

        if (KaitaiStruct.class.isAssignableFrom(cls)) {
            return (Class<? extends KaitaiStruct>) cls;
        } else {
            throw new IllegalClassFormatException(String.format(
                    "the compiled class is not assignable from \"%s\". The compiled class is \"%s\", and its superclass is \"%s\".",
                    KaitaiStruct.class, cls, cls.getSuperclass()
            ));
        }
    }

    /**
     * Create a new instance of the given class using the provided KaitaiStream.
     *
     * @param cls    The class for which to create a new instance.
     * @param stream The KaitaiStream to be used for initializing the instance.
     * @param <T>    The type of the KaitaiStruct class.
     * @return A new instance of the KaitaiStruct class.
     * @throws ReflectiveOperationException If there is an error during class instantiation.
     */
    public static <T extends KaitaiStruct> T newInstance(Class<T> cls, KaitaiStream stream)
            throws ReflectiveOperationException {
        Constructor<? extends T> ctor = cls.getDeclaredConstructor(KaitaiStream.class);
        return ctor.newInstance(stream);
    }

    /**
     * Get the class name from the given Java source code.
     *
     * @param javaSourceCode The Java source code from which to extract the class name.
     * @return The class name extracted from the Java source code.
     */
    public static String getClassName(final String javaSourceCode) {
        final Pattern pattern = Pattern.compile("public class (.+?) extends KaitaiStruct.*", Pattern.DOTALL);

        Matcher matcher = pattern.matcher(javaSourceCode);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}
