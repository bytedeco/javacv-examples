/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter03

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._

/**
 * Example for section "Using the Strategy pattern in algorithm design" in Chapter 3.
 * The pattern encapsulates an algorithm into a separate class,
 * in this case [[opencv_cookbook.chapter03.ColorDetector]].
 *
 * The original example in the book is using "C++ API". Calls here use "C API" supported by JavaCV.
 */
object Ex1ColorDetector extends App {

  // 1. Create image processor object
  val colorDetector = new ColorDetector

  // 2. Read input image
  val src = loadAndShowOrExit(new File("data/boldt.jpg"), IMREAD_COLOR)

  // 3. Set the input parameters
  colorDetector.colorDistanceThreshold = 100
  // here blue sky
  colorDetector.targetColor = new ColorRGB(130, 190, 230)

  // 4. Process that input image and display the result
  val dest = colorDetector.process(src)
  show(dest, "result")
}