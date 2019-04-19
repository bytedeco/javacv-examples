/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter09

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/** Example for section "Template Matching" in chapter 9, page 265, 2nd edition.
  *
  * Finds best match between a small patch from first image (template) and a second image.
  */
object Ex2TemplateMatching extends App {

  val image1 = loadAndShowOrExit(new File("data/church01.jpg"), IMREAD_GRAYSCALE)
  val image2 = loadAndShowOrExit(new File("data/church02.jpg"), IMREAD_GRAYSCALE)

  // define a template
  val target = new Mat(image1, new Rect(120, 40, 30, 30))
  show(target, "Template")


  // define search region
  val roi = new Mat(image2,
    // here top half of the image
    new Rect(0, 0, image2.cols, image2.rows / 2))

  // perform template matching
  val result = new Mat()
  matchTemplate(
    roi, // search region
    target, // template
    result, // result
    CV_TM_SQDIFF)
  // similarity measure

  // find most similar location
  val minVal = new DoublePointer(1)
  val maxVal = new DoublePointer(1)
  val minPt  = new Point()
  val maxPt  = new Point()
  minMaxLoc(result, minVal, maxVal, minPt, maxPt, null)

  println(s"minPt = ${minPt.x}, ${minPt.y}")


  // draw rectangle at most similar location
  // at minPt in this case
  rectangle(roi, new Rect(minPt.x, minPt.y, target.cols, target.rows), new Scalar(255, 255, 255, 0))

  show(roi, "Best match")
}
