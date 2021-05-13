/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.swing.*;
import java.awt.image.BufferedImage;

import static opencv_cookbook.OpenCVUtilsJava.toBufferedImage;
import static org.bytedeco.opencv.global.opencv_core.BORDER_DEFAULT;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.Laplacian;

public class MyFirstOpenCVAppInJava {

    public static void main(String[] args) {

        // Read an image.
        final Mat src = imread("data/boldt.jpg");
        display(src, "Input");

        // Apply Laplacian filter
        final Mat dest = new Mat();
        Laplacian(src, dest, src.depth(), 1, 3, 0, BORDER_DEFAULT);
        display(dest, "Laplacian");
    }

    //---------------------------------------------------------------------------

    static void display(Mat image, String caption) {
        // Create image window named "My Image".
        final CanvasFrame canvas = new CanvasFrame(caption, 1.0);

        // Request closing of the application when the image window is closed.
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Convert from OpenCV Mat to Java Buffered image for display
        final BufferedImage bi = toBufferedImage(image);

        // Show image on window.
        canvas.showImage(bi);
    }
}


