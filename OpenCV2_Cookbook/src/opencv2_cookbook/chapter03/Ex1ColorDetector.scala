/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter03

import opencv2_cookbook.OpenCVUtils._
import com.googlecode.javacv.cpp.opencv_highgui._
import java.io.File

/**
 * Example for section "Using the Strategy pattern in algorithm design" in Chapter 3.
 * The pattern encapsulates an algorithm into a separate class,
 * in this case [[opencv2_cookbook.chapter03.ColorDetector]].
 *
 * The original example in the book is using "C++ API". Calls here use "C API" supported by JavaCV.
 */
object Ex1ColorDetector extends App {

    // 1. Create image processor object
    val colorDetector = new ColorDetector

    // 2. Read input image
    val src = loadAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)

    // 3. Set the input parameters
    colorDetector.colorDistanceThreshold = 100
    // here blue sky
    colorDetector.targetColor = new ColorRGB(130, 190, 230)

    // 4. Process that input image and display the result
    val dest = colorDetector.process(src)
    show(dest, "result")
}