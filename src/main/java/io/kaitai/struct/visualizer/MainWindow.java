package io.kaitai.struct.visualizer;

import io.kaitai.struct.ByteBufferKaitaiStream;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private static final String APP_NAME = "Kaitai Struct Visualizer";
    private static final String VERSION = "0.8";

    private final VisualizerPanel visualizerPanel;

    public MainWindow() {
        super(APP_NAME + " v" + VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getFileChooserPanel(), BorderLayout.NORTH);

        visualizerPanel = new VisualizerPanel();
        getContentPane().add(visualizerPanel.getSplitPane(), BorderLayout.CENTER);
        pack();
        setVisible(true);
    }

    private JPanel getFileChooserPanel() {

        JPanel fileChooserPanel = new JPanel();
        fileChooserPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridy = 0;
        gbc.gridx = 0;
        fileChooserPanel.add(new JLabel("KSY grammar file:"), gbc);
        gbc.gridx = 1;
        fileChooserPanel.add(new JButton("Browse..."), gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        fileChooserPanel.add(new JLabel("(no file selected)"), gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridy = 1;
        gbc.gridx = 0;
        fileChooserPanel.add(new JLabel("Binary file to parse:"), gbc);
        gbc.gridx = 1;
        fileChooserPanel.add(new JButton("Browse..."), gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        fileChooserPanel.add(new JLabel("(no file selected)"), gbc);
        return fileChooserPanel;
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
