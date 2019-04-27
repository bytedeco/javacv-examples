/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.opencv_video._
import org.bytedeco.opencv.opencv_core._


/** Uses the mean shift algorithm to find best matching location of the 'template' in another image.
  *
  * Matching is done using the hue channel of the input image converted to HSV color space.
  * Histogram of a region in the hue channel is used to create a 'template'.
  *
  * The target image, where we want to find a matching region, is also converted to HSV.
  * Histogram of the template is back projected in the hue channel.
  * The mean shift algorithm searches in the back projected image to find best match to the template.
  *
  * Example for section "Using the mean shift algorithm to find an object" in Chapter 4.
  */
object Ex8MeanShiftDetector extends App {

  val Red = new Scalar(0, 0, 255, 128)

  //
  // Prepare 'template'
  //

  // Load image as a color
  val templateImage = loadAndShowOrExit(new File("data/baboon1.jpg"), IMREAD_COLOR)

  // Display image with marked ROI
  val rect = new Rect(110, 260, 35, 40)
  show(drawOnImage(templateImage, rect, Red), "Input template")

  // Define ROI for sample histogram
  val imageROI = templateImage(rect)

  // Compute histogram within the ROI
  val minSaturation = 65
  val templateHueHist = new ColorHistogram().getHueHistogram(imageROI, minSaturation)

  val finder = new ContentFinder()
  finder.histogram = templateHueHist

  //
  //  Search a target image for best match to the 'template'
  //

  // Load the second image where we want to locate a new baboon face
  val targetImage = loadAndShowOrExit(new File("data/baboon3.jpg"), IMREAD_COLOR)

  // Convert to HSV color space
  val hsvTargetImage = new Mat()
  cvtColor(targetImage, hsvTargetImage, COLOR_BGR2HSV)

  // Get back-projection of hue histogram
  finder.threshold = -1f
  val hueBackProjectionImage = finder.find(hsvTargetImage, 0f, 180f, Array(0))
  show(hueBackProjectionImage, "Backprojection of second image")

  // Search for object with mean-shift
  val criteria = new TermCriteria(TermCriteria.MAX_ITER, 10, 0.01)
  val r = meanShift(hueBackProjectionImage, rect, criteria)
  println("meanshift = " + r)

  show(drawOnImage(targetImage, rect, Red), s"Image 2 result in $r iterations")
}
