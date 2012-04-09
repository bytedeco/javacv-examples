/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter01

import com.googlecode.javacv.CanvasFrame
import com.googlecode.javacv.cpp.opencv_highgui._
import javax.swing.JFrame._


/**
 * Example of loading and displaying and image  using JavaCV API,
 * corresponds to C++ example in Chapter 1 page 18.
 * Please note how in the Scala example code CanvasFrame from JavaCV API is used to display the image.
 */
object Ex1MyFirstOpenCVApp extends App {

    // read an image
    val image = cvLoadImage("data/boldt.jpg")

    // create image window named "My Image"
    val canvas = new CanvasFrame("My Image")

    // request closing of the application when the image window is closed
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)

    // show image on window
    canvas.showImage(image)
}