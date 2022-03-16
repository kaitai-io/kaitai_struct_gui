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

    private final JTree JTREE = new JTree();
    private final DefaultTreeModel TREE_MODEL = new DefaultTreeModel(null);
    private final JHexView HEX_EDITOR = new JHexView();
    private final JSplitPane SPLIT_PANE;
    private final MainWindow MAIN_WINDOW;

    private ByteBufferKaitaiStream binaryStreamToParse;
    private Class<? extends KaitaiStruct> kaitaiStructClass;
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

    public void compileKsyFile(String ksyFileName) {
        /*
        There are two steps:
         (1) compile KSY file into Java source code
         (2) compile Java source code into an instance of java.lang.Class
         */

        SwingWorker<Class<? extends KaitaiStruct>, String> worker = new SwingWorker<
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
                /*
                TODO: get the user params out of this SwingWorker and into getKaitaiStructInstance()

                 */
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
                // This method runs on the Swing Event Dispatch Thread
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

                // if we have already set the binary file we want to parse, then parse it now
                if (binaryStreamToParse != null) {
                    try {
                        parseFileAndUpdateGui();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }


            }


        };
        worker.execute();

    }

    public void parseFileAndUpdateGui() throws Exception {
        if (kaitaiStructClass != null && kaitaiStructInstance != null) {
            kaitaiStructInstance = getKaitaiStructInstance(kaitaiStructClass, binaryStreamToParse);
            invokeReadMethod();
            loadKaitaiStruct();
        }
    }

    public boolean isParserReady() {
        return kaitaiStructClass != null;
    }

    private void loadKaitaiStruct() {
        kaitaiStructInstance._io().seek(0);
        byte[] buf = kaitaiStructInstance._io().readBytesFull();

        HEX_EDITOR.setData(new SimpleDataProvider(buf));
        HEX_EDITOR.setDefinitionStatus(JHexView.DefinitionStatus.DEFINED);

        final DataNode root = new DataNode(0, kaitaiStructInstance, "[root]");
        TREE_MODEL.setRoot(root);
        root.explore(TREE_MODEL, null);
    }

    public JSplitPane getSplitPane() {
        return SPLIT_PANE;
    }

    /**
     * Compiles a given .ksy file into Java class source.
     *
     * @param ksyFileName
     * @return Java class source code as a string
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

    @SuppressWarnings("unchecked")
    private Class<? extends KaitaiStruct> compileJavaSourceCodeToJavaClass(String sourceCode, String className) throws Exception {
        final String fullyQualifiedClassName = DEST_PACKAGE + "." + className;
        final Class<?> ksyClassWithWildcardType = InMemoryJavaCompiler.newInstance().compile(fullyQualifiedClassName, sourceCode);

        if (!KaitaiStruct.class.isAssignableFrom(ksyClassWithWildcardType)) {
            throw new RuntimeException(String.format(
                    "the compiled class is not assignable from \"%s\". The compiled class is \"%s\", and its superclass is \"%s\".",
                    KaitaiStruct.class, ksyClassWithWildcardType, ksyClassWithWildcardType.getSuperclass()
            ));
        }

        // If the isAssignableFrom check passed, then this unchecked cast should work.
        return (Class<? extends KaitaiStruct>) ksyClassWithWildcardType;
    }

    private void invokeReadMethod() throws ReflectiveOperationException {
        // Find and run "_read" that does actual parsing
        Method readMethod = kaitaiStructClass.getMethod("_read");
        readMethod.invoke(kaitaiStructInstance);
    }


    private static KaitaiStruct getKaitaiStructInstance(Class<? extends KaitaiStruct> ksyClass, /*List<String> paramNames,*/ ByteBufferKaitaiStream streamToParse) throws ReflectiveOperationException {
        final Constructor<? extends KaitaiStruct> ctor = findConstructor(ksyClass);
        final Class<?>[] types = ctor.getParameterTypes();
        final Object[] argsToPassIntoConstructor = new Object[types.length];
        argsToPassIntoConstructor[0] = streamToParse;
        for (int i = 3; i < argsToPassIntoConstructor.length; ++i) {
            argsToPassIntoConstructor[i] = getDefaultValue(types[i]);
        }
        // TODO: get parameters from user
        return ctor.newInstance(argsToPassIntoConstructor);
    }

    @SuppressWarnings("unchecked")
    private static Constructor<? extends KaitaiStruct> findConstructor(Class<? extends KaitaiStruct> ksyClass) {
        /*
         The getConstructors() method only returns public constructors.
         The getDeclaredConstructors() method returns all constructors (private, etc).
         See https://stackoverflow.com/a/8249415/7376577.
         The TOP_CLASS_NAME_AND_PARAMETERS regex searches for a public constructor,
         so we can use getConstructors().

        Once we've added support for user parameters, we can use the getConstructor() method and specify the
         user param types.
         Java does not support arrays of bounded wildcards, so:
           - the getConstructors() method returns Constructor<?>[]
           - the getConstructor()  method returns Constructor<T>
         So we have to cast it here.



         */
        for (final Constructor<?> ctor : ksyClass.getConstructors()) {
            final Class<?>[] types = ctor.getParameterTypes();
            if (types.length >= 3
                    && types[0] == KaitaiStream.class
                    && types[1] == KaitaiStruct.class
                    && types[2] == ksyClass
            ) {
                return (Constructor<? extends KaitaiStruct>) ctor;
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
