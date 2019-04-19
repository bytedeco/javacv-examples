/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter01;


import org.bytedeco.opencv.opencv_core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static opencv_cookbook.OpenCVUtils.toBufferedImage;
import static org.bytedeco.opencv.global.opencv_core.flip;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2RGB;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;


/**
 * The last section in the chapter 1 of the Cookbook demonstrates how to create a simple GUI application.
 * The Cookbook is using Qt GUI Toolkit. This example is using Scala Swing to create an similar application.
 * <p/>
 * The application has two buttons on the left "Open Image" and "Process".
 * The opened image is displayed in the middle.
 * When "Process" button is pressed the image is flipped upside down and its red and blue channels are swapped.
 * <p/>
 * Unlike other examples this one is written in Java. It is equivalent to {@link Ex2MyFirstGUIApp} written in Scala.
 * It is provided for the sake of comparison of Java to Scala.
 */
public final class Ex2MyFirstGUIAppJava extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JFileChooser fileChooser = new JFileChooser(new File("."));

    /**
     * Component for displaying the image
     */
    private final JLabel imageView = new JLabel();

    /**
     * Variable for holding loaded image
     */
    private Mat image = null;


    private Ex2MyFirstGUIAppJava() throws HeadlessException {
        super("My First GUI Java App");

        //
        // Define actions
        //

        // Action performed when "Process" button is pressed
        final Action processAction = new AbstractAction("Process") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    // Process and update image display if image is loaded
                    if (image != null) {
                        processImage(image);
                        imageView.setIcon(new ImageIcon(toBufferedImage(image)));
                    } else {
                        showMessageDialog(Ex2MyFirstGUIAppJava.this, "Image not opened", getTitle(), ERROR_MESSAGE);
                    }
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        processAction.setEnabled(false);

        // Action performed when "Open Image" button is pressed
        final Action openImageAction = new AbstractAction("Open Image") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    // Load image and update display. If new image was not loaded do nothing.
                    final Mat img = openImage();

                    if (img != null) {
                        image = img;
                        imageView.setIcon(new ImageIcon(toBufferedImage(image)));
                        processAction.setEnabled(true);

                    }
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        //
        // Create UI
        //

        // Create button panel
        final JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        buttonsPanel.add(new JButton(openImageAction));
        buttonsPanel.add(new JButton(processAction));

        // Layout frame contents

        // Action buttons on the left
        final JPanel leftPane = new JPanel();
        leftPane.add(buttonsPanel);
        add(leftPane, BorderLayout.WEST);

        // Image display in the center
        final JScrollPane imageScrollPane = new JScrollPane(imageView);
        imageScrollPane.setPreferredSize(new Dimension(640, 480));
        add(imageScrollPane, BorderLayout.CENTER);
    }


    /**
     * Ask user for location and open new image.
     *
     * @return Opened image or {@code null} if image was not loaded.
     */
    private Mat openImage() {

        // Ask user for the location of the image file
        if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        // Load the image
        final String path = fileChooser.getSelectedFile().getAbsolutePath();
        final Mat newImage = imread(path);
        if (newImage != null) {
            return newImage;
        } else {
            showMessageDialog(this, "Cannot open image file: " + path, getTitle(), ERROR_MESSAGE);
            return null;
        }
    }


    /**
     * Process image in place
     *
     * @param src image to process.
     */
    private void processImage(final Mat src) {
        // Flip upside down
        flip(src, src, 0);
        // Swap red and blue channels
        cvtColor(src, src, COLOR_BGR2RGB);
    }


    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final Ex2MyFirstGUIAppJava frame = new Ex2MyFirstGUIAppJava();
                frame.pack();
                // Mark for display in the center of the screen
                frame.setLocationRelativeTo(null);
                // Exit application when frame is closed.
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}
