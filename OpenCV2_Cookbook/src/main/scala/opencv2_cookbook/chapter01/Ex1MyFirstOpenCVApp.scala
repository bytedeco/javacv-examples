/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter01

import javax.swing.JFrame._

import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacv.CanvasFrame


/** Example of loading and displaying and image  using JavaCV API,
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
  canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)

  // Show image on window
  canvas.showImage(image)
}
