package io.github.proto4j.kaitai.vis; //@date 28.07.2023

import cms.rendner.hexviewer.common.ranges.ByteRange;
import cms.rendner.hexviewer.model.data.IDataModel;
import cms.rendner.hexviewer.model.data.file.MappedFileData;
import cms.rendner.hexviewer.view.JHexViewer;
import cms.rendner.hexviewer.view.components.caret.CaretEvent;
import cms.rendner.hexviewer.view.components.caret.ICaret;
import cms.rendner.hexviewer.view.components.caret.ICaretListener;
import cms.rendner.hexviewer.view.components.damager.DefaultDamager;
import cms.rendner.hexviewer.view.components.highlighter.DefaultHighlighter;
import cms.rendner.hexviewer.view.components.highlighter.IHighlighter;
import io.github.proto4j.kaitai.vis.tree.KaitaiTreeNode;
import io.github.proto4j.kaitai.vis.tree.StructTreeCellRenderer;
import io.github.proto4j.kaitai.vis.tree.StructTreeModel;
import io.kaitai.struct.KaitaiStruct;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class JVis extends JPanel {

    private final JTree tree;
    private final JHexViewer hexView;

    private CaretListener caretListener;
    private FlatLafTheme theme;

    public JVis() {
        this.hexView = new JHexViewer();
        this.setupHexView();

        this.tree = new JTree(new DefaultMutableTreeNode("<root>"));
        this.setupTree();

        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(tree),
                new JScrollPane(hexView)
        );
        splitPane.setDividerLocation(300);
        add(splitPane);
    }

    public JHexViewer getHexView() {
        return hexView;
    }

    public FlatLafTheme getTheme() {
        return theme;
    }

    public JTree getTree() {
        return tree;
    }

    public CaretListener getCaretListener() {
        return caretListener;
    }

    public void display(final KaitaiStruct struct, final String filePath) throws ReflectiveOperationException, IOException {
        struct._io().seek(0); // reset the stream
        Method method = struct.getClass().getDeclaredMethod("_read");
        method.setAccessible(true);
        method.invoke(struct);

        StructTreeModel treeModel = new StructTreeModel("<root>", struct);
        tree.setModel(treeModel);
        // reset the stream again to read all bytes
        struct._io().seek(0);
        IDataModel dataModel = new MappedFileData(new File(filePath));//new RawDataModel(struct._io().readBytesFull());
        hexView.setDataModel(dataModel);
        // Populate map for the hex viewer selection listener
        Map<TreePath, ByteRange> spans = new HashMap<>();
        populateTreeSpans(spans, treeModel.getRoot(), new TreePath(treeModel.getRoot()));
        IHighlighter highlighter = new DefaultHighlighter();
        hexView.setDamager(new DefaultDamager());
        hexView.setHighlighter(highlighter);
        caretListener.spans = spans;
    }

    protected void populateTreeSpans(Map<TreePath, ByteRange> destination, KaitaiTreeNode node, TreePath path) {
        if (node.isLeaf()) {
            ByteRange absSpan = node.getSpan();
            if (absSpan != null) {
                destination.put(path, absSpan);
                System.out.println(node.getName() + " " + absSpan);
            }
        } else {
            final Enumeration<? extends TreeNode> children = node.children();
            while (children.hasMoreElements()) {
                final TreeNode child = children.nextElement();
                if (child instanceof KaitaiTreeNode) {
                    populateTreeSpans(destination, (KaitaiTreeNode) child, path.pathByAddingChild(child));
                }
            }
        }
    }

    protected void setupHexView() {
        theme = new FlatLafTheme();
        theme.applyTo(hexView);

        hexView.setShowOffsetCaretIndicator(true);
        hexView.setBytesPerRow(16);
        hexView.setPreferredVisibleRowCount(23);
        hexView.getCaret().ifPresent(c -> c.addCaretListener(caretListener = new CaretListener()));
    }

    protected void setupTree() {
        tree.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new JVisTreeCellRenderer());
        tree.addTreeSelectionListener(new SelectionListener());
    }

    private static final class JVisTreeCellRenderer extends StructTreeCellRenderer {

        @Override
        protected Icon getIconForNode(KaitaiTreeNode node) {
            switch (node.getType()) {
                case ARRAY:
                    return AllIcons.DataTypes.Array;
                case VALUE:
                    return AllIcons.DataTypes.Value;
                case STRUCT:
                    return AllIcons.DataTypes.Struct;
                case ENUM:
                    return AllIcons.DataTypes.Enum;
                default:
                    return AllIcons.DataTypes.Unknown;
            }
        }
    }

    protected final class SelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (tree.getSelectionPaths() == null) {
                return;
            }

            hexView.getHighlighter().ifPresent(IHighlighter::removeAllHighlights);
            for (TreePath path : tree.getSelectionPaths()) {
                Object node = path.getLastPathComponent();
                if (node instanceof KaitaiTreeNode) {
                    ByteRange span = ((KaitaiTreeNode) node).getSpan();
                    if (span != null && span.getStart() != span.getEnd()) {
                        hexView.getHighlighter().ifPresent(h -> {
                            // The end index should be excluded
                            h.addHighlight(span.getStart(), span.getEnd() - 1, new FlatLafTheme.HighlightPainter(((KaitaiTreeNode) node).getColor()));
                        });
                        hexView.getCaret().ifPresent(c -> {
                            c.moveCaret(
                                    span.getStart(), // offset
                                    false, // withSelection
                                    true // scrollToCaret
                            );
                        });
                    }
                }
            }
        }
    }

    protected final class CaretListener implements ICaretListener {

        private Map<TreePath, ByteRange> spans;

        @Override
        public void caretPositionChanged(CaretEvent caretEvent) {
            if (this.spans == null) {
                return;
            }
            // TODO...

            ICaret caret = hexView.getCaret().orElseThrow();
            long start = caret.getDot();
            long end = caret.getDot();
            if (caret.hasSelection()) {
                start = caret.getSelectionStart();
                end = caret.getSelectionEnd();
            }

            for (Map.Entry<TreePath, ByteRange> entry : spans.entrySet()) {
                TreePath path = entry.getKey();
                ByteRange span = entry.getValue();

                final long spanStart = span.getStart();
                final long spanEnd = span.getEnd();

                boolean select;
                if (start < spanStart) {
                    /*
                    The left edge of the selection is before the span.

                    Select the tree node only if the right edge of the selection is either
                    inside the span or goes past the right edge of the span.

                    (1) The right edge of the selection is inside the span or goes past the
                        right edge of the span.

                        Example:

                        selection   v-------v
                           bytes  0 1 2 3 4 5 6 7 8 9
                            span          ^---^

                        or

                        selection    v-------------v
                            bytes  0 1 2 3 4 5 6 7 8 9
                             span          ^---^

                    (2) The right edge of the span is to the left of the span.

                        Example:

                        selection    v---v
                            bytes  0 1 2 3 4 5 6 7 8 9
                             span          ^---^
                     */
                    select = end > spanStart;
                } else {
                    /*
                    The left edge of the selection is either inside the span
                    or past the right edge of the span.

                    Select the tree node only if the left edge of the selection is inside
                    the span.

                    (1) The left edge of the selection is inside the span.

                        Example:

                        selection         v-----v
                           bytes  0 1 2 3 4 5 6 7 8 9
                            span      ^-----^

                    (2) The left edge of the selection is to the right of the span.

                        Example:

                        selection                v---v
                            bytes  0 1 2 3 4 5 6 7 8 9
                             span      ^-----^
                    */
                    select = start < spanEnd;
                }

                if (select) {
                    tree.getSelectionModel().setSelectionPath(path);
                    break;
                }
            }
        }
    }
}
