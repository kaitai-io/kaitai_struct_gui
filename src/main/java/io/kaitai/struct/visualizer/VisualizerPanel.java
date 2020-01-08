package io.kaitai.struct.visualizer;

import at.HexLib.library.HexLib;
import at.HexLib.library.HexLibSelectionModel;
import io.kaitai.struct.CompileLog;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.Main;
import io.kaitai.struct.RuntimeConfig;
import io.kaitai.struct.format.ClassSpec;
import io.kaitai.struct.formats.JavaClassSpecs;
import io.kaitai.struct.formats.JavaKSYParser;
import io.kaitai.struct.languages.JavaCompiler$;
import org.mdkt.compiler.InMemoryJavaCompiler;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualizerPanel extends JPanel {
    private JTree tree;
    private DefaultTreeModel model;
    private HexLib hexEditor;
    private JSplitPane splitPane;

    private KaitaiStruct struct;
    private Map<String, Integer> attrStart;
    private Map<String, Integer> attrEnd;

    public VisualizerPanel() throws IOException {
        super();

        initialize();
    }

    private void initialize() {
        tree = new JTree();
        hexEditor = new HexLib(new byte[] {});

        JScrollPane treeScroll = new JScrollPane(tree);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, hexEditor);

        model = new DefaultTreeModel(null);
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

        DataNode root = new DataNode(0, struct, null, "[root]");
        model.setRoot(root);
        root.explore(model /*, progressListener */, null);
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public static final String DEST_PACKAGE = "io.kaitai.struct.visualized";

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

    private final static Pattern TOP_CLASS_NAME = Pattern.compile("public class (.*?) extends KaitaiStruct");

    /**
     * Compiles Java source (given as a string) into bytecode and loads it into current JVM.
     * @param javaSrc Java source as a string
     * @return Class reference, which can be used to instantiate the class, call its
     * static methods, etc.
     * @throws Exception
     */
    private static Class<?> compileAndLoadJava(String javaSrc) throws Exception {
        Matcher m = TOP_CLASS_NAME.matcher(javaSrc);
        if (!m.find())
            throw new RuntimeException("Unable to find top-level class in compiled .java");
        String className = m.group(1);
        return InMemoryJavaCompiler.compile(DEST_PACKAGE + "." + className, javaSrc);
    }

    private void parseFileWithKSY(String ksyFileName, String binaryFileName) throws Exception {
        String javaSrc = compileKSY(ksyFileName);
        Class<?> ksyClass = compileAndLoadJava(javaSrc);

        // Find and run "fromFile" helper method to
        Method fromFileMethod = ksyClass.getMethod("fromFile", String.class);
        Object kso = fromFileMethod.invoke(null, binaryFileName);
        struct = (KaitaiStruct) kso;

        // Find and run "_read" that does actual parsing
        // TODO: wrap this in try-catch block
        Method readMethod = ksyClass.getMethod("_read");
        readMethod.invoke(struct);
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
                ArrayList<Point> intervals = new ArrayList<Point>();
                intervals.add(new Point(node.posStart(), node.posEnd()));
                select.setSelectionIntervals(intervals);
                System.out.println("" + node.posStart() + " - " + node.posEnd());
            }
        }
    }
}
