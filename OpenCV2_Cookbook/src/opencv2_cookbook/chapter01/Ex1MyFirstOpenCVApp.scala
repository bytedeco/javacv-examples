/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter01

import com.googlecode.javacv.CanvasFrame
import com.googlecode.javacv.cpp.opencv_highgui._
import javax.swing.JFrame._


/** Example of loading and displaying and image  using JavaCV API,
  * corresponds to C++ example in Chapter 1 page 18.
  * Please note how in the Scala example code CanvasFrame from JavaCV API is used to display the image.
  */
object Ex1MyFirstOpenCVApp extends App {

    // Read an image
    val image = cvLoadImage("data/boldt.jpg")

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
