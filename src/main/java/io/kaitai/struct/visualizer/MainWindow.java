package io.kaitai.struct.visualizer;

import io.kaitai.struct.ByteBufferKaitaiStream;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

public class MainWindow extends JFrame {
    private static final String APP_NAME = "Kaitai Struct Visualizer";
    private static final String VERSION = "0.8";

    private final VisualizerPanel visualizerPanel;
    private JLabel jLabelSelectedKsyFileName;
    private JLabel jLabelSelectedBinaryFile;


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


        JFileChooser fileChooserKsyFile = new JFileChooser();
        fileChooserKsyFile.setMultiSelectionEnabled(false);
        fileChooserKsyFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooserKsyFile.setFileFilter(new FileNameExtensionFilter("KSY files", "ksy"));
        fileChooserKsyFile.setAcceptAllFileFilterUsed(true);
        fileChooserKsyFile.setDialogTitle("Choose KSY file");


        JFileChooser fileChooserBinaryFileToParse = new JFileChooser();
        fileChooserBinaryFileToParse.setMultiSelectionEnabled(false);
        fileChooserBinaryFileToParse.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooserBinaryFileToParse.setDialogTitle("Choose binary file to parse");


        JPanel fileChooserPanel = new JPanel();
        fileChooserPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridy = 0;
        gbc.gridx = 0;
        fileChooserPanel.add(new JLabel("KSY file:"), gbc);
        gbc.gridx = 1;
        JButton buttonChooseKsyFile = new JButton("Browse...");
        buttonChooseKsyFile.addActionListener(e -> {
            if (fileChooserKsyFile.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                setKsyFile(fileChooserKsyFile.getSelectedFile().getAbsolutePath());
            }
        });
        fileChooserPanel.add(buttonChooseKsyFile, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        jLabelSelectedKsyFileName = new JLabel("(no KSY file selected)");
        fileChooserPanel.add(jLabelSelectedKsyFileName, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridy = 1;
        gbc.gridx = 0;
        fileChooserPanel.add(new JLabel("Binary file to parse:"), gbc);
        gbc.gridx = 1;
        JButton buttonChooseBinaryFileToParse = new JButton("Browse...");
        buttonChooseBinaryFileToParse.addActionListener(e -> {
            if (fileChooserBinaryFileToParse.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                setBinaryFileToParse(fileChooserBinaryFileToParse.getSelectedFile().getAbsolutePath());
            }
        });
        fileChooserPanel.add(buttonChooseBinaryFileToParse, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        jLabelSelectedBinaryFile = new JLabel("(no binary file selected)");
        fileChooserPanel.add(jLabelSelectedBinaryFile, gbc);
        return fileChooserPanel;
    }

    private void setKsyFile(String pathToKsyFile) {
        try {
            visualizerPanel.setKsyFile(pathToKsyFile);
            jLabelSelectedKsyFileName.setText(pathToKsyFile);
        }catch (Exception ex){

            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "message", "title", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setBinaryFileToParse(String pathToBinaryFile) {
        try {
            visualizerPanel.setStreamToParse(new ByteBufferKaitaiStream(pathToBinaryFile));
            jLabelSelectedBinaryFile.setText(pathToBinaryFile);
        }catch (Exception ex){
            JOptionPane.showMessageDialog(null, "message", "title", JOptionPane.ERROR_MESSAGE);
        }
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
                    mainWindow.setBinaryFileToParse(binaryFileToParse);
                    mainWindow.setKsyFile(ksyFileName);
//                    mainWindow.visualizerPanel.loadAll(new ByteBufferKaitaiStream(binaryFileToParse), ksyFileName);
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
