/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter01

import javax.swing.WindowConstants
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/**
 * Example of reading, saving, displaying, and drawing on an image.
 */
object Ex3LoadAndSave extends App {

  // read the input image as a gray-scale image
  val image = imread("data/puppy.bmp", IMREAD_COLOR)

  if (image.empty()) {
    // error handling
    // no image has been created...
    // possibly display an error message
    // and quit the application
    println("Error reading image...")
    System.exit(0)
  }

  println("This image is " + image.rows + " x " + image.cols)

  // Create image window named "My Image".
  val canvas = new CanvasFrame("My Image", 1)

  // Request closing of the application when the image window is closed
  canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  // Show image on window
  val converter = new OpenCVFrameConverter.ToMat()
  canvas.showImage(converter.convert(image))


  // we create another empty image
  val result = new Mat(); // we create another empty image
  // positive for horizontal
  // 0 for vertical,
  // negative for both
  flip(image, result, 1)

  // the output window
  val canvas2 = new CanvasFrame("Output Image", 1)

  // Request closing of the application when the image window is closed
  canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  // Show image on window
  canvas2.showImage(converter.convert(result))

  // save result
  imwrite("output.bmp", result)

  // create another image window
  val canvas3 = new CanvasFrame("Drawing on an Image", 1)

  val image3 = image.clone()

  circle(image3, // destination image
    new Point(155, 110), // center coordinate
    65, // radius
    new Scalar(0), // color (here black)
    3, // thickness
    8, // 8-connected line
    0) // shift

  putText(image3, // destination image
    "This is a dog.", // text
    new Point(40, 200), // text position
    FONT_HERSHEY_PLAIN, // font type
    2.0, // font scale
    new Scalar(255), // text color (here white)
    2, // text thickness
    8, // Line type.
    false) //When true, the image data origin is at the bottom-left corner. Otherwise, it is at the top-left corner.

  canvas3.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  canvas3.showImage(converter.convert(image3))
}
