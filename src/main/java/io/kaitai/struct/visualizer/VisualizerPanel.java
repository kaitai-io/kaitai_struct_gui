package io.kaitai.struct.visualizer;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.CompileLog;
import io.kaitai.struct.JavaRuntimeConfig;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;
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
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualizerPanel extends JPanel {
    /**
     * Name of the Java package where the Java source code is generated from KSY file
     */
    private static final String DEST_PACKAGE = "io.kaitai.struct.visualized";

    /**
     * Regular expression with two groups: (1) class name and (2) type parameters.
     * <p>
     * Type parameters must be parsed with the {@link #PARAMETER_NAME} regex.
     */
    private static final Pattern TOP_CLASS_NAME_AND_PARAMETERS = Pattern.compile(
            "public class (.+?) extends KaitaiStruct.*" +
                    "public \\1\\(KaitaiStream _io, KaitaiStruct _parent, \\1 _root(.*?)\\)",
            Pattern.DOTALL
    );

    /**
     * Regular expression used to get parameter names from the generated Java source code.
     * <p>
     * First use the {@link #TOP_CLASS_NAME_AND_PARAMETERS} regex, then pass the matched string into this regex.
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
     * Background color of selected hex data in HEX and ASCII sections.
     */
    private static final Color SELECTION = new Color(0xc0c0c0);

    private final JTree JTREE = new JTree();
    private final DefaultTreeModel TREE_MODEL = new DefaultTreeModel(null);
    private final JHexView HEX_EDITOR = new JHexView();
    private final JSplitPane SPLIT_PANE;
    private final MainWindow MAIN_WINDOW;

    private ByteBufferKaitaiStream binaryStreamToParse;

    /**
     * This field stores a reference to the Java class compiled in memory from a KSY file.
     */
    private Class<? extends KaitaiStruct> kaitaiStructClass;

    /**
     * This object does the actual parsing. It is an instance of {@link #kaitaiStructClass}.
     * <p>
     * This instance can only be created when both of these are true:
     * <ul>
     *     <li>a KSY file has been compiled to a Java class, and</li>
     *     <li>the binary stream to parse is known</li>
     * </ul>
     */
    private KaitaiStruct kaitaiStructInstance;


    public VisualizerPanel(MainWindow mainWindow) {
        super();
        MAIN_WINDOW = mainWindow;

        HEX_EDITOR.setSeparatorsVisible(false);
        HEX_EDITOR.setBytesPerColumn(1);
        HEX_EDITOR.setColumnSpacing(8);
        HEX_EDITOR.setHeaderFontStyle(Font.BOLD);

        HEX_EDITOR.setFontColorHeader(HEADER);
        HEX_EDITOR.setFontColorOffsetView(HEADER);

        HEX_EDITOR.setFontColorHexView1(UNMODIFIED);
        HEX_EDITOR.setFontColorHexView2(UNMODIFIED);
        HEX_EDITOR.setFontColorAsciiView(UNMODIFIED);

        HEX_EDITOR.setSelectionColor(SELECTION);

        HEX_EDITOR.setBackgroundColorOffsetView(HEX_EDITOR.getBackground());
        HEX_EDITOR.setBackgroundColorHexView(HEX_EDITOR.getBackground());
        HEX_EDITOR.setBackgroundColorAsciiView(HEX_EDITOR.getBackground());

        SPLIT_PANE = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(JTREE), HEX_EDITOR);
        SPLIT_PANE.setDividerLocation(200);

        JTREE.setShowsRootHandles(true);
        final KaitaiTreeListener treeListener = new KaitaiTreeListener();
        JTREE.addTreeWillExpandListener(treeListener);
        JTREE.addTreeSelectionListener(treeListener);
        JTREE.setModel(TREE_MODEL);
    }

    public void setBinaryStreamToParse(ByteBufferKaitaiStream stream) {
        binaryStreamToParse = stream;
    }

    /**
     * Compiles a Kaitai Struct YAML file into a Java class.
     * <p>
     * There are two steps:
     * <ol>
     *     <li>Compile the KSY file into Java source code.</li>
     *     <li>Compile the Java source code into an instance of {@code java.lang.Class}.</li>
     * </ol>
     * <p>
     * If compilation succeeds, then subsequent calls to the {@link #isParserReady} method return true.
     * </p>
     *
     * @param ksyFileName path to the Kaitai Struct YAML file
     */
    public void compileKsyFile(String ksyFileName) {

        new SwingWorker<
                Class<? extends KaitaiStruct>, //the type returned by the doInBackground() and get() methods
                String //the type passed to the publish() method and received by the process() method
                >() {
            @Override
            protected Class<? extends KaitaiStruct> doInBackground() throws Exception {
                publish("Compiling KSY file into Java source code...");
                final String javaSourceCode = compileKsyFileToJavaSourceCode(ksyFileName);

                final Matcher topLevelClassMatcher = TOP_CLASS_NAME_AND_PARAMETERS.matcher(javaSourceCode);
                if (!topLevelClassMatcher.find()) {
                    throw new RuntimeException("Unable to find top-level class in generated .java");
                }
                final String className = topLevelClassMatcher.group(1); //the group at index zero is the whole match
                parseUserParams(topLevelClassMatcher.group(2));

                publish("Compiling Java source code into a Java class...");
                return compileJavaSourceCodeToJavaClass(javaSourceCode, className);
            }

            private void parseUserParams(String paramsToParse) {
                // TODO: get the user parameters out of this SwingWorker so we can use them.
                final ArrayList<String> paramNames = new ArrayList<>();
                final ArrayList<String> paramTypes = new ArrayList<>();
                final Matcher paramMatcher = PARAMETER_NAME.matcher(paramsToParse);
                while (paramMatcher.find()) {
                    paramTypes.add(paramMatcher.group(1)); //the group at index zero is the whole match
                    paramNames.add(paramMatcher.group(2));
                }
            }


            @Override
            protected void process(List<String> chunks) {
                // This method runs on the Swing Event Dispatch Thread, so we can safely access the jLabel.
                final String newestChunk = chunks.get(chunks.size() - 1);
                MAIN_WINDOW.jLabelStatus.setText(newestChunk);
            }

            @Override
            protected void done() {
                // This method runs on the Swing Event Dispatch Thread.
                try {
                    kaitaiStructClass = get();
                } catch (CancellationException | InterruptedException ignore) {
                    return;
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                    final String message = "<html>Couldn't compile the KSY file.<br>" +
                            "The exception was: " + ex + ".<br>" +
                            "See the console for the full stack trace.";
                    JOptionPane.showMessageDialog(MAIN_WINDOW, message, MainWindow.APP_NAME, JOptionPane.ERROR_MESSAGE);
                    return;
                }

                MAIN_WINDOW.setCursor(Cursor.getDefaultCursor());
                MAIN_WINDOW.jLabelStatus.setText("Done compiling KSY file.");
                MAIN_WINDOW.jButtonChooseKsyFile.setEnabled(true);

                // If we have already set the binary file we want to parse, then parse it now.
                if (binaryStreamToParse != null) {
                    try {
                        parseFileAndUpdateGui();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        final String message = "<html>There was an error initializing Kaitai Struct or parsing the file.<br>" +
                                "The exception was: " + ex + "<br>" +
                                "See the console for the full stack trace.";
                        JOptionPane.showMessageDialog(MAIN_WINDOW, message, MainWindow.APP_NAME, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    /**
     * If the parser and binary stream have both been set, then this method parses the stream
     * and shows the result in the GUI.
     *
     * @throws ReflectiveOperationException if creating an instance of the Kaitai Struct class failed,
     *                                      or if parsing the binary stream failed
     */
    public void parseFileAndUpdateGui() throws ReflectiveOperationException {
        if (isParserReady() && binaryStreamToParse != null) {
            // the kaitai struct constructor needs the stream to open.
            kaitaiStructInstance = createKaitaiStructInstance(kaitaiStructClass, binaryStreamToParse);

            // the read method parses the whole file.
            kaitaiStructInstance._io().seek(0);
            final Method readMethod = kaitaiStructClass.getMethod("_read");
            readMethod.invoke(kaitaiStructInstance);

            updateHexEditorData();
            updateJTree();
        }
    }

    /**
     * Returns whether a KSY file has been compiled into a Java class.
     *
     * @return true if the application has a reference to a compiled Kaitai Struct parser; false otherwise.
     */
    public boolean isParserReady() {
        return kaitaiStructClass != null;
    }

    private void updateHexEditorData() {
        kaitaiStructInstance._io().seek(0);
        final byte[] allBytes = kaitaiStructInstance._io().readBytesFull(); //read all remaining bytes
        HEX_EDITOR.setData(new SimpleDataProvider(allBytes));
        HEX_EDITOR.setDefinitionStatus(JHexView.DefinitionStatus.DEFINED);
    }

    private void updateJTree(){
        final DataNode root = new DataNode(0, kaitaiStructInstance, "[root]");
        TREE_MODEL.setRoot(root);
        root.explore(TREE_MODEL, null);
    }

    public JSplitPane getSplitPane() {
        return SPLIT_PANE;
    }

    /**
     * Compiles a Kaitai Struct YAML file into Java source code.
     *
     * @param ksyFileName path to the KSY file to compile
     * @return a {@code String} of Java source code which is the result of compiling the KSY file
     */
    private static String compileKsyFileToJavaSourceCode(String ksyFileName) {
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

        io.kaitai.struct.Main.importAndPrecompile(specs, config).value();
        final CompileLog.SpecSuccess result = io.kaitai.struct.Main.compile(specs, spec, JavaCompiler$.MODULE$, config);
        return result.files().apply(0).contents();
    }

    /**
     * Compiles Java source code of a Kaitai Struct parser into a Java class.
     *
     * @param sourceCode Java source code of a Kaitai Struct parser
     * @param className  name of the Java class to create
     * @return a Java class compiled from {@code sourceCode}
     * @throws Exception if compilation failed
     */
    @SuppressWarnings("unchecked")
    private Class<? extends KaitaiStruct> compileJavaSourceCodeToJavaClass(String sourceCode, String className) throws Exception {
        final String fullyQualifiedClassName = DEST_PACKAGE + "." + className;
        final Class<?> classWithWildcardType = InMemoryJavaCompiler.newInstance().compile(fullyQualifiedClassName, sourceCode);

        if (KaitaiStruct.class.isAssignableFrom(classWithWildcardType)) {
            return (Class<? extends KaitaiStruct>) classWithWildcardType;
        } else {
            throw new RuntimeException(String.format(
                    "the compiled class is not assignable from \"%s\". The compiled class is \"%s\", and its superclass is \"%s\".",
                    KaitaiStruct.class, classWithWildcardType, classWithWildcardType.getSuperclass()
            ));
        }

    }


    /**
     * Instantiates a Kaitai Struct class so that the instance will read from the specified binary stream, and returns
     * the instance.
     *
     * @param ksClass       the Kaitai Struct Java class to instantiate
     * @param streamToParse the binary stream that the Kaitai Struct parser will parse
     * @return an instance of {@code ksClass} which will parse {@code streamToParse}
     * @throws ReflectiveOperationException if the instance could not be created
     */
    private static KaitaiStruct createKaitaiStructInstance(
            Class<? extends KaitaiStruct> ksClass,
            /*List<String> paramNames,*/
            ByteBufferKaitaiStream streamToParse)
            throws ReflectiveOperationException {
        final Constructor<? extends KaitaiStruct> ctor = findConstructor(ksClass);
        final Class<?>[] paramTypes = ctor.getParameterTypes();
        final Object[] argsToPassIntoConstructor = new Object[paramTypes.length];
        argsToPassIntoConstructor[0] = streamToParse;
        for (int i = 3; i < argsToPassIntoConstructor.length; ++i) {
            argsToPassIntoConstructor[i] = getDefaultValue(paramTypes[i]);
        }
        // TODO: get parameters from user
        return ctor.newInstance(argsToPassIntoConstructor);
    }

    /**
     * Returns a constructor from the given Kaitai Struct class.
     * <p>
     * TODO: search for a constructor which accepts user parameters.
     *
     * @param ksClass a Kaitai Struct Java class from which to find a constructor
     * @return a {@code Constructor} object
     */
    @SuppressWarnings("unchecked")
    private static Constructor<? extends KaitaiStruct> findConstructor(Class<? extends KaitaiStruct> ksClass) {
        /*
         Difference between getConstructors and getDeclaredConstructors:
         The getConstructors() method only returns public constructors.
         The getDeclaredConstructors() method returns all constructors (private, etc).
         See https://stackoverflow.com/a/8249415/7376577.
         The TOP_CLASS_NAME_AND_PARAMETERS regex searches for a public constructor,
         so we can use getConstructors() here.

         Right now we have to get all the constructors and search for the right one.
         Once we've added support for user parameters, we can use the getConstructor() method
         and specify the user param types.
         Java does not support arrays of bounded wildcards. As a result:
           * ksClass.getConstructors() returns Constructor<?>[]
           * ksClass.getConstructor()  returns Constructor<? extends KaitaiStruct>
         We're using getConstructors() so we have to manually cast the result.
         */
        for (final Constructor<?> ctor : ksClass.getConstructors()) {
            final Class<?>[] types = ctor.getParameterTypes();
            if (types.length >= 3
                    && types[0] == KaitaiStream.class
                    && types[1] == KaitaiStruct.class
                    && types[2] == ksClass
            ) {
                return (Constructor<? extends KaitaiStruct>) ctor;
            }
        }
        throw new IllegalArgumentException(ksClass + " has no KaitaiStruct-generated constructor");
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
            final TreePath path = event.getPath();
            if (path.getLastPathComponent() instanceof DataNode) {
                DataNode node = (DataNode) path.getLastPathComponent();
                node.explore(TREE_MODEL, null);
            }
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        }

        @Override
        public void valueChanged(TreeSelectionEvent event) {
            HEX_EDITOR.getSelectionModel().clearSelection();
            for (final TreePath path : JTREE.getSelectionPaths()) {
                final Object selected = path.getLastPathComponent();
                if (!(selected instanceof DataNode)) continue;

                final DataNode node = (DataNode) selected;
                final Integer start = node.posStart();
                final Integer end = node.posEnd();
                if (start == null || end == null) continue;
                // Selection in nibbles, so multiply by 2
                HEX_EDITOR.getSelectionModel().addSelectionInterval(2 * start, 2 * end - 1);
            }
        }
    }
}
