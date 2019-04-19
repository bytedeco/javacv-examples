/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.opencv.opencv_core.Mat;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.bytedeco.opencv.global.opencv_imgproc.calcHist;

/**
 * Created by john on 16/08/16.
 */
public class Histogram1DJava {
    private int numberOfBins = 256;
    private IntPointer channels = new IntPointer(1);
    private Float _minRange = 0.0f;
    private Float _maxRange = 255.0f;

    public void setRanges(Float minRange, Float maxRange) {
        _minRange = minRange;
        _maxRange = maxRange;
    }

    public BufferedImage getHistogramImage(Mat image) {
        int width = this.numberOfBins;
        int height = this.numberOfBins;
        double[] hist = getHistogramAsArray(image);
        double scale = 0.9 / max(hist) * height;
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();

        // Paint background
        g.setPaint(Color.WHITE);
        g.fillRect(0, 0, width, height);


        // Draw a vertical line for each bin
        g.setPaint(Color.BLUE);

        for (int bin = 0; bin < numberOfBins; bin++) {
            int h = Long.valueOf(Math.round(hist[bin] * scale)).intValue();
            g.drawLine(bin, height - 1, bin, height - h - 1);
        }

        g.dispose();
        return canvas;
    }

    public double[] getHistogramAsArray(Mat image) {
        Mat hist = getHistogram(image);
        double[] dest = new double[numberOfBins];
        Indexer indexer = hist.createIndexer();
        for (int i = 0; i < numberOfBins; i++) {
            dest[i] = indexer.getDouble(i);
        }
        return dest;
    }


    private Mat getHistogram(Mat image) {
        return getHistogram(image, new Mat());
    }

    private Mat getHistogram(Mat image, Mat mask) {
        IntPointer histSize = new IntPointer(1);
        histSize.put(0, numberOfBins);
        FloatPointer ranges = new FloatPointer(_minRange, _maxRange);

        Mat hist = new Mat();
        calcHist(image, 1, channels, mask, hist, 1, histSize, ranges);
        return hist;
    }


    private double max(double[] dest) {
        double max = 0.0;
        for (double value : dest) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
}
