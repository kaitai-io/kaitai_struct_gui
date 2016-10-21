package io.kaitai.struct.visualizer;

import at.HexLib.library.HexLib;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.formats.Wmf;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.io.IOException;

public class VisualizerPanel extends JPanel {
    private JTree tree;
    private HexLib hexEditor;
    private JSplitPane splitPane;

    public VisualizerPanel() throws IOException {
        super();

        initialize();

        KaitaiStruct ks = Wmf.fromFile("wmf_src/RedBags.wmf");
        loadStruct(ks);
    }

    private void initialize() {
        tree = new JTree();
        hexEditor = new HexLib(new byte[] {});

        JScrollPane treeScroll = new JScrollPane(tree);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, hexEditor);
    }

    public void loadStruct(KaitaiStruct struct) throws IOException {
        struct._io().seek(0);
        byte[] buf = struct._io().readBytesFull();
        hexEditor.setByteContent(buf);

        DataNode root = new DataNode(0, struct, null, "[root]");

        final DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setShowsRootHandles(true);
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
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
        });
        tree.setModel(model);
        root.explore(model /*, progressListener */, null);
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }
}
