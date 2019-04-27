/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter05

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._


/**
 * Example from section "Segmenting images using watersheds".
 */
object Ex4WatershedSegmentation extends App {

  // Read input image
  val image  = loadAndShowOrExit(new File("data/group.jpg"), IMREAD_COLOR)
  val binary = loadAndShowOrExit(new File("data/binary.bmp"), IMREAD_GRAYSCALE)

  // Eliminate noise and smaller objects, repeat erosion 6 times
  val fg = new Mat()
  erode(binary, fg,
    new Mat() /* 3x3 square */ ,
    new Point(-1, -1),
    6 /* iterations */ ,
    BORDER_CONSTANT,
    morphologyDefaultBorderValue)
  show(fg, "Foreground")

  // Identify image pixels pixels objects
  val bg = new Mat()
  dilate(binary, bg,
    new Mat() /* 3x3 square */ ,
    new Point(-1, -1),
    6 /* iterations */ ,
    BORDER_CONSTANT,
    morphologyDefaultBorderValue)
  show(bg, "Dilated")

  threshold(bg, bg,
    1 /* threshold */ ,
    128 /* max value */ ,
    THRESH_BINARY_INV)
  show(bg, "Background")

  // Create marker image
  val markers = new Mat(binary.size(), CV_8U, new Scalar(0d))
  add(fg, bg, markers)
  show(markers, "Markers")

  val segmenter = new WatershedSegmenter()
  segmenter.setMarkers(markers)

  val segmentMarkers = segmenter.process(image)
  show(segmentMarkers, "segmentMarkers")

  val segmentation = segmenter.segmentation
  show(segmentation, "Segmentation")

  val watershed = segmenter.watersheds
  show(watershed, "Watersheds")
}