/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter05

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Example from section "Segmenting images using watersheds".
 */
object Ex4WatershedSegmentation extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_COLOR)
    val binary = loadAndShowOrExit(new File("data/binary.bmp"))

    // Eliminate noise and smaller objects, repeat erosion 6 times
    val fg = IplImage.create(cvGetSize(binary), binary.depth, 1 /* channels */)
    cvErode(binary, fg, null /* 3x3 square */ , 6 /* iterations */)
    show(fg, "Foreground")

    // Identify image pixels pixels objects
    val bg = IplImage.create(cvGetSize(binary), binary.depth, 1 /* channels */)
    cvDilate(binary, bg, null /* 3x3 square */ , 6 /* iterations */)
    show(bg, "Dilated")

    cvThreshold(bg, bg, 1 /* threshold */ , 128 /* max value */ , CV_THRESH_BINARY_INV)
    show(bg, "Background")

    // Create marker image
    val markers = IplImage.create(cvGetSize(binary), IPL_DEPTH_8U, 1 /* channels */)
    cvAdd(fg, bg, markers, null)
    show(markers, "Markers")

    val segmenter = new WatershedSegmenter
    segmenter.setMarkers(markers)

    val segmentMarkers = segmenter.process(image)
    show(segmentMarkers, "segmentMarkers")

    val segmentation = segmenter.getSegmentation
    show(segmentation, "Segmentation")

    val watershed = segmenter.getWatersheds
    show(watershed, "Watersheds")
}