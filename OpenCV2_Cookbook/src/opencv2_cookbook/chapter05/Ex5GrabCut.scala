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
 * Example from section "Extracting foreground objects with the GrabCut algorithm".
 */
object Ex5GrabCut extends App {

    // Open image
    val image = loadAndShowOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_COLOR)

    // Define bounding rectangle, pixels outside this rectangle will be labeled as background.
    val rectangle = new CvRect(10, 100, 380, 180)

    val result = IplImage.create(cvGetSize(image), IPL_DEPTH_8U, 1 /* channels */)
    val iterCount = 5
    val mode = GC_INIT_WITH_RECT

    // GrabCut segmentation
    grabCut(image, result, rectangle, null, null, iterCount, mode)

    // Extract foreground mask
    cvCmpS(result, GC_PR_FGD, result, CV_CMP_EQ)
    show(result, "Result foreground mask")
}