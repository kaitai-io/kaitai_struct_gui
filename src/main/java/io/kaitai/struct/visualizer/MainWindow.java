package io.kaitai.struct.visualizer;

import io.kaitai.struct.ByteBufferKaitaiStream;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class MainWindow extends JFrame {
    static final String APP_NAME = "Kaitai Struct Visualizer";
    private static final String VERSION = "0.8";

    private final VisualizerPanel visualizerPanel;
    private JLabel jLabelSelectedKsyFile;
    private JLabel jLabelSelectedBinaryFile;

    // use default privacy so that VisualizerPanel can access it
    JLabel jLabelStatus;
    JButton jButtonChooseKsyFile;


    public MainWindow() {
        super(APP_NAME + " v" + VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //noinspection ConstantConditions - suppress IntelliJ warning that getResource() might return null
        setIconImages(Arrays.asList(
                new ImageIcon(getClass().getResource("/kaitai-struct-icon-48.png")).getImage(),
                new ImageIcon(getClass().getResource("/kaitai-struct-icon-32.png")).getImage(),
                new ImageIcon(getClass().getResource("/kaitai-struct-icon-16.png")).getImage()
        ));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getFileChooserPanel(), BorderLayout.NORTH);
        visualizerPanel = new VisualizerPanel(this);
        getContentPane().add(visualizerPanel.getSplitPane(), BorderLayout.CENTER);
        getContentPane().add(getStatusBarPanel(), BorderLayout.SOUTH);

        pack();
        setVisible(true);
    }

    private JPanel getFileChooserPanel() {
        final JPanel retVal = new JPanel();
        retVal.setLayout(new GridBagLayout());
        retVal.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); //add padding

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(2, 5, 2, 5);
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        retVal.add(new JLabel("KSY file:"), constraints);

        constraints.gridy = 1;
        retVal.add(new JLabel("Binary file:"), constraints);

        final JFileChooser fileChooserKsyFile = new JFileChooser();
        fileChooserKsyFile.setMultiSelectionEnabled(false);
        fileChooserKsyFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooserKsyFile.setFileFilter(new FileNameExtensionFilter("KSY files", "ksy"));
        fileChooserKsyFile.setAcceptAllFileFilterUsed(true);
        fileChooserKsyFile.setDialogTitle("Choose KSY file");

        final JFileChooser fileChooserBinaryFile = new JFileChooser();
        fileChooserBinaryFile.setMultiSelectionEnabled(false);
        fileChooserBinaryFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooserKsyFile.setAcceptAllFileFilterUsed(true);
        fileChooserBinaryFile.setDialogTitle("Choose binary file to parse");

        jButtonChooseKsyFile = new JButton("Browse...");
        jButtonChooseKsyFile.addActionListener(e -> {
            final int buttonClicked = fileChooserKsyFile.showOpenDialog(null);
            if (buttonClicked == JFileChooser.APPROVE_OPTION) {
                setKsyFile(fileChooserKsyFile.getSelectedFile().getAbsolutePath());
            }
        });

        final JButton jButtonChooseBinaryFileToParse = new JButton("Browse...");
        jButtonChooseBinaryFileToParse.addActionListener(e -> {
            final int buttonClicked = fileChooserBinaryFile.showOpenDialog(null);
            if (buttonClicked == JFileChooser.APPROVE_OPTION) {
                setBinaryFileToParse(fileChooserBinaryFile.getSelectedFile().getAbsolutePath());
            }
        });

        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 1;
        constraints.gridy = 0;
        retVal.add(jButtonChooseKsyFile, constraints);

        constraints.gridy = 1;
        retVal.add(jButtonChooseBinaryFileToParse, constraints);

        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        jLabelSelectedKsyFile = new JLabel("(no KSY file selected)");
        retVal.add(jLabelSelectedKsyFile, constraints);

        constraints.gridy = 1;
        jLabelSelectedBinaryFile = new JLabel("(no binary file selected)");
        retVal.add(jLabelSelectedBinaryFile, constraints);

        return retVal;
    }

    private JPanel getStatusBarPanel() {
        jLabelStatus = new JLabel("Ready.");
        final JPanel statusBarPanel = new JPanel();
        statusBarPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        statusBarPanel.add(jLabelStatus);
        return statusBarPanel;
    }

    /**
     * Set the Kaitai Struct YAML file, and start compiling it into a Java class.
     * <p>
     * If the binary file has also been set, then the GUI will be updated when compilation is finished.
     *
     * @param pathToKsyFile location of the KSY file
     */
    private void setKsyFile(String pathToKsyFile) {
        jLabelSelectedKsyFile.setText(pathToKsyFile);
        jButtonChooseKsyFile.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // The compileKsyFile() method will re-enable the button and change the cursor when it's finished.
        visualizerPanel.compileKsyFile(pathToKsyFile);
    }

    /**
     * Set the binary file to be parsed with Kaitai Struct.
     * <p>
     * If the Kaitai Struct parser has also been compiled, then the GUI will also be updated.
     *
     * @param pathToBinaryFile location of the binary file to parse
     */
    private void setBinaryFileToParse(String pathToBinaryFile) {
        jLabelSelectedBinaryFile.setText(pathToBinaryFile);

        final ByteBufferKaitaiStream streamToParse;
        try {
            streamToParse = new ByteBufferKaitaiStream(pathToBinaryFile);
        } catch (IOException ex) {
            ex.printStackTrace();
            final String message = "<html>Couldn't open the selected file (\"" + pathToBinaryFile + "\") for parsing.<br>" +
                    "The exception was: " + ex + ".<br>" +
                    "See the console for the full stack trace.";
            JOptionPane.showMessageDialog(this, message, APP_NAME, JOptionPane.ERROR_MESSAGE);
            return;
        }
        visualizerPanel.setBinaryStreamToParse(streamToParse);

        if (visualizerPanel.isParserReady()) {
            try {
                visualizerPanel.parseFileAndUpdateGui();
            } catch (Exception ex) {
                ex.printStackTrace();
                final String message = "<html>There was an error initializing Kaitai Struct or parsing the file.<br>" +
                        "The exception was: " + ex + "<br>" +
                        "See the console for the full stack trace.";
                JOptionPane.showMessageDialog(this, message, APP_NAME, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // Swing stuff should be done on the Swing Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            final MainWindow mainWindow = new MainWindow();

            switch (args.length) {
                case 0:
                    // No command-line arguments, don't do anything else.
                    break;
                case 2:
                    final String binaryFileToParse = args[0];
                    final String ksyFileName = args[1];
                    if (!ksyFileName.endsWith(".ksy")) {
                        System.err.println("Warning: the second argument does not have file extension .ksy.");
                        printCommandLineUsage();
                    }
                    mainWindow.setBinaryFileToParse(binaryFileToParse);
                    mainWindow.setKsyFile(ksyFileName);
                    break;
                default:
                    printCommandLineUsage();
                    break;
            }
        });

    }

    private static void printCommandLineUsage() {
        System.out.println("Command-line usage: java -jar kaitai_struct_visualizer_java.jar binaryFile ksyFile");
    }
}
