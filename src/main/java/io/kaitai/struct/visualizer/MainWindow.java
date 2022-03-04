package io.kaitai.struct.visualizer;

import io.kaitai.struct.ByteBufferKaitaiStream;

import javax.swing.*;
import java.io.IOException;

public class MainWindow extends JFrame {
    private static final String APP_NAME = "Kaitai Struct Visualizer";
    private static final String VERSION = "0.8";

    private VisualizerPanel vis;

    public MainWindow() throws IOException {
        super(APP_NAME + " v" + VERSION);
        vis = new VisualizerPanel();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().add(vis.getSplitPane());
        pack();
        setVisible(true);
    }

    public static void main(final String arg[]) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        MainWindow mw = new MainWindow();


        if (arg.length == 2) {
            String binaryFileToParse = arg[0];
            String ksyFileName = arg[1];
            mw.vis.loadAll(new ByteBufferKaitaiStream(binaryFileToParse), ksyFileName);
        }
    }
}
