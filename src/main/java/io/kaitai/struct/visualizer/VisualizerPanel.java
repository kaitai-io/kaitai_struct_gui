package io.kaitai.struct.visualizer;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;

import tv.porst.jhexview.JHexView;
import tv.porst.jhexview.SimpleDataProvider;

public class VisualizerPanel extends JPanel {
    /** Package to generate classes in. */
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
    /** Regexp, used to get parameter names from the generated source. */
    private static final Pattern PARAMETER_NAME = Pattern.compile(", \\S+ ([^,\\s]+)");

    /** Color of hex editor section headers. */
    private static final Color HEADER = new Color(0x0000c0);
    /** Color of hex data in HEX and ASCII sections. */
    private static final Color UNMODIFIED = Color.BLACK;
    /** Background color selected hex data in HEX and ASCII sections. */
    private static final Color SELECTION = new Color(0xc0c0c0);

    private final JTree tree = new JTree();
    private final DefaultTreeModel model = new DefaultTreeModel(null);
    private final JHexView hexEditor = new JHexView();
    private final JSplitPane splitPane;

    private KaitaiStruct struct;

    public VisualizerPanel() throws IOException {
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

        tree.setShowsRootHandles(true);
        KaitaiTreeListener treeListener = new KaitaiTreeListener();
        tree.addTreeWillExpandListener(treeListener);
        tree.addTreeSelectionListener(treeListener);
        tree.setModel(model);
    }

    public void loadAll(String dataFileName, String ksyFileName) throws Exception {
        parseFileWithKSY(ksyFileName, dataFileName);
        loadStruct();
    }

    private void loadStruct() throws IOException {
        struct._io().seek(0);
        byte[] buf = struct._io().readBytesFull();
        hexEditor.setData(new SimpleDataProvider(buf));
        hexEditor.setDefinitionStatus(JHexView.DefinitionStatus.DEFINED);

        final DataNode root = new DataNode(0, struct, "[root]");
        model.setRoot(root);
        root.explore(model /*, progressListener */, null);
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    /**
     * Compiles a given .ksy file into Java class source.
     * @param ksyFileName
     * @return Java class source code as a string
     */
    private static String compileKSY(String ksyFileName) {
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

    /**
     * Compiles Java source (given as a string) into bytecode and loads it into current JVM.
     * @param javaSrc Java source as a string
     * @return Class reference, which can be used to instantiate the class, call its
     * static methods, etc.
     * @throws Exception
     */
    private void parseFileWithKSY(String ksyFileName, String binaryFileName) throws Exception {
        final String javaSrc = compileKSY(ksyFileName);
        final Matcher m = TOP_CLASS_NAME_AND_PARAMETERS.matcher(javaSrc);
        if (!m.find()) {
            throw new RuntimeException("Unable to find top-level class in generated .java");
        }
        // Parse parameter names
        final ArrayList<String> paramNames = new ArrayList<>();
        final Matcher p = PARAMETER_NAME.matcher(m.group(2));
        while (p.find()) {
            paramNames.add(p.group(1));
        }

        final Class<?> ksyClass = InMemoryJavaCompiler.newInstance().compile(DEST_PACKAGE + "." + m.group(1), javaSrc);
        struct = construct(ksyClass, paramNames, binaryFileName);

        // Find and run "_read" that does actual parsing
        // TODO: wrap this in try-catch block
        Method readMethod = ksyClass.getMethod("_read");
        readMethod.invoke(struct);
    }
    private static KaitaiStruct construct(Class<?> ksyClass, List<String> paramNames, String binaryFileName) throws Exception {
        final Constructor<?> c = findConstructor(ksyClass);
        final Class<?>[] types = c.getParameterTypes();
        final Object[] args = new Object[types.length];
        args[0] = new ByteBufferKaitaiStream(binaryFileName);
        for (int i = 3; i < args.length; ++i) {
            args[i] = getDefaultValue(types[i]);
        }
        // TODO: get parameters from user
        return (KaitaiStruct)c.newInstance(args);
    }
    private static <T> Constructor<T> findConstructor(Class<T> ksyClass) {
        for (final Constructor c : ksyClass.getDeclaredConstructors()) {
            final Class<?>[] types = c.getParameterTypes();
            if (types.length >= 3
             && types[0] == KaitaiStream.class
             && types[1] == KaitaiStruct.class
             && types[2] == ksyClass
            ) {
                return c;
            }
        }
        throw new IllegalArgumentException(ksyClass + " has no KaitaiStruct-generated constructor");
    }
    private static Object getDefaultValue(Class<?> clazz) {
        if (clazz == boolean.class) return false;
        if (clazz == char.class   ) return (char)0;
        if (clazz == byte.class   ) return (byte)0;
        if (clazz == short.class  ) return (short)0;
        if (clazz == int.class    ) return 0;
        if (clazz == long.class   ) return 0L;
        if (clazz == float.class  ) return 0.0f;
        if (clazz == double.class ) return 0.0;
        return null;
    }

    public class KaitaiTreeListener implements TreeWillExpandListener, TreeSelectionListener {
        @Override
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            TreePath path = event.getPath();
            if (path.getLastPathComponent() instanceof DataNode) {
                DataNode node = (DataNode) path.getLastPathComponent();
                node.explore(model /* , progressListener */, null);
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

                final DataNode node = (DataNode)selected;
                final Integer start = node.posStart();
                final Integer end   = node.posEnd();
                if (start == null || end == null) continue;
                // Selection in nibbles, so multiply by 2
                hexEditor.getSelectionModel().addSelectionInterval(2*start, 2*end-1);
            }
        }
    }
}
