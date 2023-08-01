package io.kaitai.struct.visualizer; //@date 28.07.2023

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import io.github.proto4j.kaitai.vis.JVis;
import io.github.proto4j.kaitai.vis.KsyCompiler;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.RandomAccessFileKaitaiStream;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main implements Runnable {
    @Parameter(names = "-ksy", description = "Path to the .ksy file")
    String ksyPath;

    @Parameter(description = "Path to the binary", required = true)
    String binaryPath;

    @Parameter(names = "-java", description = "Path to generated Java file")
    String javaPath;

    @Parameter(names = "-dark", description = "Enables dark layout.")
    boolean darkLaf;

    @Parameter(names = {"-h", "--help"}, description = "Show this help message.")
    boolean showHelp;

    public static void main(String[] args) {
        Main main = new Main();
        JCommander commander = JCommander.newBuilder()
                .addObject(main)
                .build();

        try {
            commander.parse(args);
        } catch (Exception e) {
            if (!main.showHelp) {
                System.out.println(e.toString());
            }
            commander.usage();
            System.exit(1);
        }

        if (main.showHelp) {
            commander.usage();
            System.exit(0);
        }

        SwingUtilities.invokeLater(main);
    }


    @Override
    public void run() {
        verifyArgs();
        setupLaf();

        String javaSourceCode = null;
        if (this.ksyPath != null) {
            javaSourceCode = KsyCompiler.compileToJava(this.ksyPath);
        }

        if (this.javaPath != null) try {
            javaSourceCode = Files.readString(Path.of(this.javaPath));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        JVis vis = new JVis();
        if (javaSourceCode != null) try {
            Class<? extends KaitaiStruct> cls = KsyCompiler.createClass(javaSourceCode);
            KaitaiStream stream = new RandomAccessFileKaitaiStream(this.binaryPath);
            KaitaiStruct struct = KsyCompiler.newInstance(cls, stream);

            vis.display(struct, this.binaryPath);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException) {
                ((InvocationTargetException) e).getTargetException().printStackTrace();
            } else {
                e.printStackTrace();
            }
            System.exit(1);
        }

        show(vis);
    }

    private void verifyArgs() {
        if (this.javaPath == null && this.ksyPath == null) {
            throw new IllegalArgumentException("Either Ksy-File or Java-File has to be provided!");
        }
    }

    private void setupLaf() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            if (this.darkLaf) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void show(JVis vis) {
        final JFrame jframe = new JFrame("Kaitai Struct Visualizer");

        jframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jframe.getContentPane().setLayout(new BorderLayout());
        jframe.getContentPane().add(vis);
        jframe.pack();
        jframe.setVisible(true);
    }
}
