package io.kaitai.struct.visualizer;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.CompileLog;
import io.kaitai.struct.JavaRuntimeConfig;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.Main;
import io.kaitai.struct.RuntimeConfig;
import io.kaitai.struct.Version;
import io.kaitai.struct.format.ClassSpec;
import io.kaitai.struct.format.KSVersion;
import io.kaitai.struct.formats.JavaClassSpecs;
import io.kaitai.struct.formats.JavaKSYParser;
import io.kaitai.struct.languages.JavaCompiler$;
import org.mdkt.compiler.InMemoryJavaCompiler;
import tv.porst.jhexview.JHexView;
import tv.porst.jhexview.SimpleDataProvider;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualizerPanel extends JPanel {
    /**
     * Package to generate classes in.
     */
    private static final String DEST_PACKAGE = "io.kaitai.struct.visualized";
    /**
     * Regexp with 2 groups: class name and type parameters. Type parameters
     * must be parsed with {@link #PARAMETER_NAME}.
     */
    private static final Pattern TOP_CLASS_NAME_AND_PARAMETERS = Pattern.compile(
            "public class (.+?) extends KaitaiStruct.*" +
                    "public \\1\\(KaitaiStream _io, KaitaiStruct _parent, \\1 _root(.*?)\\)",
            Pattern.DOTALL
    );
    /**
     * Regexp, used to get parameter names from the generated source.
     */
    private static final Pattern PARAMETER_NAME = Pattern.compile(", (\\S+) ([^,\\s]+)");

    /**
     * Color of hex editor section headers.
     */
    private static final Color HEADER = new Color(0x0000c0);
    /**
     * Color of hex data in HEX and ASCII sections.
     */
    private static final Color UNMODIFIED = Color.BLACK;
    /**
     * Background color selected hex data in HEX and ASCII sections.
     */
    private static final Color SELECTION = new Color(0xc0c0c0);

    private final JTree tree = new JTree();
    private final DefaultTreeModel treeModel = new DefaultTreeModel(null);
    private final JHexView hexEditor = new JHexView();
    private final JSplitPane splitPane;

    private ByteBufferKaitaiStream streamToParse;
    private Class<? extends KaitaiStruct> ksClass;
    private KaitaiStruct ksInstance;

    public VisualizerPanel() {
        super();
        JScrollPane treeScroll = new JScrollPane(tree);

        hexEditor.setSeparatorsVisible(false);
        hexEditor.setBytesPerColumn(1);
        hexEditor.setColumnSpacing(8);
        hexEditor.setHeaderFontStyle(Font.BOLD);

        hexEditor.setFontColorHeader(HEADER);
        hexEditor.setFontColorOffsetView(HEADER);

        hexEditor.setFontColorHexView1(UNMODIFIED);
        hexEditor.setFontColorHexView2(UNMODIFIED);
        hexEditor.setFontColorAsciiView(UNMODIFIED);

        hexEditor.setSelectionColor(SELECTION);

        hexEditor.setBackgroundColorOffsetView(hexEditor.getBackground());
        hexEditor.setBackgroundColorHexView(hexEditor.getBackground());
        hexEditor.setBackgroundColorAsciiView(hexEditor.getBackground());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, hexEditor);
        splitPane.setDividerLocation(200);

        tree.setShowsRootHandles(true);
        KaitaiTreeListener treeListener = new KaitaiTreeListener();
        tree.addTreeWillExpandListener(treeListener);
        tree.addTreeSelectionListener(treeListener);
        tree.setModel(treeModel);
    }

    public void setStreamToParse(ByteBufferKaitaiStream stream) throws Exception {
        streamToParse = stream;
        if (ksClass != null) {
            go();
        }
    }

    public void setKsyFile(String ksyFileName) throws Exception {
        ksClass = compileKsyFileToJavaClass(ksyFileName);
        if (ksClass != null) {
            go();
        }
    }

    private void go() throws Exception {
        ksInstance = getKaitaiStructInstance(ksClass, streamToParse);
        invokeReadMethod();
        loadKaitaiStruct();
    }

    /*
    public void loadAll(ByteBufferKaitaiStream streamToParse, String ksyFileName) throws Exception {
        this.streamToParse = streamToParse;
        ksClass = compileKsyFileToJavaClass(ksyFileName);
        go();
    }
     */

    private void loadKaitaiStruct() {
        ksInstance._io().seek(0);
        byte[] buf = ksInstance._io().readBytesFull();
        hexEditor.setData(new SimpleDataProvider(buf));
        hexEditor.setDefinitionStatus(JHexView.DefinitionStatus.DEFINED);

        final DataNode root = new DataNode(0, ksInstance, "[root]");
        treeModel.setRoot(root);
        root.explore(treeModel /*, progressListener */, null);
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    /**
     * Compiles a given .ksy file into Java class source.
     *
     * @param ksyFileName
     * @return Java class source code as a string
     */
    private static String compileKsyFileToJavaSource(String ksyFileName) {
        KSVersion.current_$eq(Version.version());
        final ClassSpec spec = JavaKSYParser.fileNameToSpec(ksyFileName);
        final JavaClassSpecs specs = new JavaClassSpecs(null, null, spec);

        final RuntimeConfig config = new RuntimeConfig(
                false,// autoRead - do not call `_read` automatically in constructor
                true, // readStoresPos - enable generation of a position info which is accessed in DebugAids later
                true, // opaqueTypes
                null, // cppConfig
                null, // goPackage
                new JavaRuntimeConfig(
                        DEST_PACKAGE,
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

        Main.importAndPrecompile(specs, config).value();
        final CompileLog.SpecSuccess result = Main.compile(specs, spec, JavaCompiler$.MODULE$, config);
        return result.files().apply(0).contents();
    }

    private Class<? extends KaitaiStruct> compileJavaSourceToJavaClass(String javaSrc, String className) throws Exception {
        String fullyQualifiedClassName = DEST_PACKAGE + "." + className;
        final Class<?> ksyClassWithWildcardType = InMemoryJavaCompiler.newInstance().compile(fullyQualifiedClassName, javaSrc);

        if (!KaitaiStruct.class.isAssignableFrom(ksyClassWithWildcardType)) {
            throw new Exception(String.format(
                    "the compiled class is not assignable from \"%s\". The compiled class is \"%s\", and the superclass is \"%s\".",
                    KaitaiStruct.class, ksyClassWithWildcardType, ksyClassWithWildcardType.getSuperclass()
            ));
        }

        // If the isAssignableFrom check passed, I think this unchecked cast should work.
        return (Class<? extends KaitaiStruct>) ksyClassWithWildcardType;
    }

    private void invokeReadMethod() throws ReflectiveOperationException {
        // Find and run "_read" that does actual parsing
        Method readMethod = ksClass.getMethod("_read");
        readMethod.invoke(ksInstance);
    }

    private Class<? extends KaitaiStruct> compileKsyFileToJavaClass(String ksyFileName) throws Exception {

        String javaSrc = compileKsyFileToJavaSource(ksyFileName);

        final Matcher topMatcher = TOP_CLASS_NAME_AND_PARAMETERS.matcher(javaSrc);
        if (!topMatcher.find()) {
            throw new RuntimeException("Unable to find top-level class in generated .java");
        }
        // Parse user params
        // group at index zero is the whole match
        String className = topMatcher.group(1);

        //TODO: get the user params out of this method and into parseFileWithKSY
        String paramsToParse = topMatcher.group(2);
        final ArrayList<String> paramNames = new ArrayList<>();
        final ArrayList<String> paramTypes = new ArrayList<>();
        final Matcher paramMatcher = PARAMETER_NAME.matcher(paramsToParse);
        while (paramMatcher.find()) {
            // group at index zero is the whole match
            paramTypes.add(paramMatcher.group(1));
            paramNames.add(paramMatcher.group(2));
        }

        return compileJavaSourceToJavaClass(javaSrc, className);


    }

    private static KaitaiStruct getKaitaiStructInstance(Class<?> ksyClass, /*List<String> paramNames,*/ ByteBufferKaitaiStream streamToParse) throws Exception {
        final Constructor<?> ctor = findConstructor(ksyClass);
        final Class<?>[] types = ctor.getParameterTypes();
        final Object[] argsToPassIntoConstructor = new Object[types.length];
        argsToPassIntoConstructor[0] = streamToParse;
        for (int i = 3; i < argsToPassIntoConstructor.length; ++i) {
            argsToPassIntoConstructor[i] = getDefaultValue(types[i]);
        }
        // TODO: get parameters from user
        return (KaitaiStruct) ctor.newInstance(argsToPassIntoConstructor);
    }

    private static <T> Constructor<T> findConstructor(Class<T> ksyClass) {
        /*
          For some reason, getConstructors() (plural) returns a Constructor<?> but
          getConstructor() (singular) returns an array of Constructor<T>.
          https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html
          Later I might try using the user param types
         */
        /*
         The getConstructors() method only returns public constructors.
         The getDeclaredConstructors() method returns all constructors (private, etc).
         See https://stackoverflow.com/a/8249415/7376577.

         The TOP_CLASS_NAME_AND_PARAMETERS regex searches for a public constructor,
         so we can use getConstructors().
         */
        for (final Constructor ctor : ksyClass.getDeclaredConstructors()) {
            final Class<?>[] types = ctor.getParameterTypes();
            if (types.length >= 3
                    && types[0] == KaitaiStream.class
                    && types[1] == KaitaiStruct.class
                    && types[2] == ksyClass
            ) {
                return ctor;
            }
        }
        throw new IllegalArgumentException(ksyClass + " has no KaitaiStruct-generated constructor");
    }

    private static Object getDefaultValue(Class<?> clazz) {
        if (clazz == boolean.class) return false;
        if (clazz == char.class) return (char) 0;
        if (clazz == byte.class) return (byte) 0;
        if (clazz == short.class) return (short) 0;
        if (clazz == int.class) return 0;
        if (clazz == long.class) return 0L;
        if (clazz == float.class) return 0.0f;
        if (clazz == double.class) return 0.0;
        return null;
    }

    public class KaitaiTreeListener implements TreeWillExpandListener, TreeSelectionListener {
        @Override
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            TreePath path = event.getPath();
            if (path.getLastPathComponent() instanceof DataNode) {
                DataNode node = (DataNode) path.getLastPathComponent();
                node.explore(treeModel /* , progressListener */, null);
            }
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        }

        @Override
        public void valueChanged(TreeSelectionEvent event) {
            hexEditor.getSelectionModel().clearSelection();
            for (final TreePath path : tree.getSelectionPaths()) {
                final Object selected = path.getLastPathComponent();
                if (!(selected instanceof DataNode)) continue;

                final DataNode node = (DataNode) selected;
                final Integer start = node.posStart();
                final Integer end = node.posEnd();
                if (start == null || end == null) continue;
                // Selection in nibbles, so multiply by 2
                hexEditor.getSelectionModel().addSelectionInterval(2 * start, 2 * end - 1);
            }
        }
    }
}
