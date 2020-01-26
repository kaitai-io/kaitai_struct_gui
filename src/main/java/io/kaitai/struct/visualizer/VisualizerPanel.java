package io.kaitai.struct.visualizer;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.HexLib.library.HexLib;
import at.HexLib.library.HexLibSelectionModel;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.CompileLog;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.Main;
import io.kaitai.struct.RuntimeConfig;
import io.kaitai.struct.format.ClassSpec;
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

public class VisualizerPanel extends JPanel {
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

    private final JTree tree = new JTree();
    private final DefaultTreeModel model = new DefaultTreeModel(null);
    private final HexLib hexEditor = new HexLib(new byte[0]);
    private final JSplitPane splitPane;

    private KaitaiStruct struct;

    public VisualizerPanel() throws IOException {
        super();
        JScrollPane treeScroll = new JScrollPane(tree);

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
        hexEditor.setByteContent(buf);

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
        final ClassSpec spec = JavaKSYParser.fileNameToSpec(ksyFileName);
        final JavaClassSpecs specs = new JavaClassSpecs(null, null, spec);

        final RuntimeConfig config = new RuntimeConfig(
            true, // debug - required for existing _attrStart/_attrEnd/_arrStart/_arrEnd fields
            true, // opaqueTypes
            null, // goPackage
            DEST_PACKAGE,
            "io.kaitai.struct.ByteBufferKaitaiStream",
            null, // dotNetNamespace
            null, // phpNamespace
            null  // pythonPackage
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
            TreePath path = event.getPath();
            if (path.getLastPathComponent() instanceof DataNode) {
                DataNode node = (DataNode) path.getLastPathComponent();
                if (node.posStart() == null || node.posEnd() == null)
                    return;
                HexLibSelectionModel select = hexEditor.getSelectionModel();
                ArrayList<Point> intervals = new ArrayList<>();
                intervals.add(new Point(node.posStart(), node.posEnd()));
                select.setSelectionIntervals(intervals);
                System.out.println(node.posStart() + " - " + node.posEnd());
            }
        }
    }
}
