package io.kaitai.struct.visualizer;

import javax.swing.*;
import java.io.IOException;

public class MainWindow extends JFrame {
    private static final String APP_NAME = "Kaitai Struct Visualizer";
    private static final String VERSION = "0.5";

    private VisualizerPanel vis;

    public MainWindow() throws IOException {
        super(APP_NAME + " v" + VERSION);
        vis = new VisualizerPanel();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().add(vis.getSplitPane());
        pack();
        setVisible(true);
    }

    public static void main(String arg[]) throws IOException {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    new MainWindow();
                } catch (ClassNotFoundException |
                        UnsupportedLookAndFeelException |
                        IllegalAccessException |
                        InstantiationException |
                        IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
