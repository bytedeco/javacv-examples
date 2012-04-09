/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import opencv2_cookbook.OpenCVUtils._

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_video._

import java.awt.Rectangle
import java.io.File

/**
 * Uses histogram of region in an color image to create 'template',
 * uses mean shift algorithm to find best matching location of the 'template' in another image.
 * Example for section "Backprojecting a histogram to detect specific image content" in Chapter 4.
 */
object Ex8MeanShiftDetector extends App {

    //
    // Prepare 'template'
    //

    // Load image as a color
    val templateImage = loadAndShowOrExit(new File("data/baboon1.jpg"), CV_LOAD_IMAGE_COLOR)

    // Display image with marked ROI
    val rect = new Rectangle(110, 260, 35, 40)
    show(drawOnImage(templateImage, rect), "Input template")

    // Define ROI for sample histogram
    templateImage.roi(toIplROI(rect))

    // Compute histogram within the ROI
    val minSaturation = 65
    val templateHueHist = new ColorHistogram().getHueHistogram(templateImage, minSaturation)

    //
    //  Search a target image for best match to the 'template'
    //

    // Load the second image where we want to locate a new baboon face
    val targetImage = loadAndShowOrExit(new File("data/baboon3.jpg"), CV_LOAD_IMAGE_COLOR)

    // Convert to HSV color space
    val hsvTargetImage = cvCreateImage(cvGetSize(targetImage), targetImage.depth, 3)
    cvCvtColor(targetImage, hsvTargetImage, CV_BGR2HSV)

    // Identify pixels with low saturation
    val saturationChannel = ColorHistogram.splitChannels(hsvTargetImage)(1)
    cvThreshold(saturationChannel, saturationChannel, minSaturation, 255, CV_THRESH_BINARY)
    show(saturationChannel, "Target saturation mask")

    // Get back-projection of the hue histogram of the 'template'
    val finder = new ContentFinder()
    finder.histogram = templateHueHist
    val result = finder.find(hsvTargetImage)
    show(result, "Back-projection.")

    // Eliminate low saturation pixels, to reduce noise abd improve search quality
    cvAnd(result, saturationChannel, result, null)
    show(result, "Back-projection with reduced saturation pixels.")

    // Starting position for the search
    val targetRect = new CvRect()
    targetRect.x(rect.x)
    targetRect.y(rect.y)
    targetRect.width(rect.width)
    targetRect.height(rect.height)

    // Search termination criteria
    val termCriteria = new CvTermCriteria()
    termCriteria.max_iter(10)
    termCriteria.epsilon(0.01)
    termCriteria.`type`(CV_TERMCRIT_ITER)

    // Search using mean shift algorithm.
    val searchResults = new CvConnectedComp()
    val iterations = cvMeanShift(result, targetRect, termCriteria, searchResults)
    show(drawOnImage(targetImage, toRectangle(searchResults.rect())), "Output in " + iterations + " iterations.")
}
