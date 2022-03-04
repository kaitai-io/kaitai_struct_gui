package io.kaitai.struct.visualizer;

import io.kaitai.struct.ByteBufferKaitaiStream;

import javax.swing.*;
import java.io.IOException;

public class MainWindow extends JFrame {
    private static final String APP_NAME = "Kaitai Struct Visualizer";
    private static final String VERSION = "0.8";

    private final VisualizerPanel visualizerPanel;

    public MainWindow() throws IOException {
        super(APP_NAME + " v" + VERSION);
        visualizerPanel = new VisualizerPanel();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().add(visualizerPanel.getSplitPane());
        pack();
        setVisible(true);
    }

    public static void main(final String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        MainWindow mainWindow = new MainWindow();


        switch (args.length) {
            case 0:
                // just show the GUI window
                break;
            case 2:
                String binaryFileToParse = args[0];
                String ksyFileName = args[1];
                if (!ksyFileName.endsWith(".ksy")) {
                    System.err.println("Warning: the second argument does not have file extension .ksy.");
                    System.err.println("Command-line usage: java -jar binaryFileToParse ksyFileName");
                }
                try {
                    mainWindow.visualizerPanel.loadAll(new ByteBufferKaitaiStream(binaryFileToParse), ksyFileName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            default:
                System.out.println("Command-line usage: java -jar binaryFileToParse ksyFileName");
                break;
        }

    }
}
