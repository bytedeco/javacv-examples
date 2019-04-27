/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter01

import javax.swing.WindowConstants
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import org.bytedeco.opencv.global.opencv_imgcodecs._


/**
 * Example of loading and displaying and image  using JavaCV API,
 * corresponds to C++ example in Chapter 1 page 18.
 * Please note how in the Scala example code CanvasFrame from JavaCV API is used to display the image.
 */
object Ex1MyFirstOpenCVApp extends App {

  // Read an image
  val image = imread("data/boldt.jpg")
  if (image.empty()) {
    // error handling
    // no image has been created...
    // possibly display an error message
    // and quit the application
    println("Error reading image...")
    System.exit(0)
  }

  // Create image window named "My Image".
  //
  // Note that you need to indicate to CanvasFrame not to apply gamma correction,
  // by setting gamma to 1, otherwise the image will not look correct.
  val canvas = new CanvasFrame("My Image", 1)

  // Request closing of the application when the image window is closed
  canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  // Convert from OpenCV Mat to Java Buffered image for display
  val converter = new OpenCVFrameConverter.ToMat()
  // Show image on window
  canvas.showImage(converter.convert(image))
}
