/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04;


import org.bytedeco.opencv.opencv_core.Mat;

import java.awt.image.BufferedImage;
import java.io.File;

import static opencv_cookbook.OpenCVUtilsJava.loadAndShowOrExit;
import static opencv_cookbook.OpenCVUtilsJava.show;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE;


/**
 * The second example for section "Computing the image histogram" in Chapter 4, page 92.
 * Displays a graph of a histogram created using utility class [[opencv_cookbook.chapter04.Histogram1D]].
 */
public class Ex2ComputeHistogramGraphJava {

    public static void main(final String[] args) {
        Mat src = loadAndShowOrExit(new File("data/group.jpg"), IMREAD_GRAYSCALE);

        // Calculate histogram
        Histogram1DJava h = new Histogram1DJava();
        BufferedImage histogram = h.getHistogramImage(src);
        // Display the graph
        show(histogram, "Histogram");
    }
}