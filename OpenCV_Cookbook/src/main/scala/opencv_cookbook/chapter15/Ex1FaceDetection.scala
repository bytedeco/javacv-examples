package opencv_cookbook.chapter15

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.indexer._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_dnn._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc.rectangle
import org.bytedeco.opencv.opencv_core._

/**
  * This is an example of detecting faces in an image using a pre-trained deep learning neural network model.
  */
object Ex1FaceDetection extends App {

  // Set up configuration
  val confidenceThreshold = 0.5
  val modelConfiguration = new File("models/face_detection/deploy.prototxt")
  val modelBinary = new File("models/face_detection/res10_300x300_ssd_iter_140000.caffemodel")
  val inWidth = 300
  val inHeight = 300
  val inScaleFactor = 1.0
  val meanVal = new Scalar(104.0, 177.0, 123.0, 128)

  val markerColor = new Scalar(0, 255, 255, 0)

  if (!modelConfiguration.exists()) {
    println(s"Cannot find model configuration: ${modelConfiguration.getCanonicalPath}")
  }
  if (!modelBinary.exists()) {
    println(s"Cannot find model file: ${modelConfiguration.getCanonicalPath}")
  }

  // Load network parameters
  val net = readNetFromCaffe(modelConfiguration.getCanonicalPath, modelBinary.getCanonicalPath)

  // Load image for testing
  val image = loadAndShowOrExit(new File("data/family-of-three.jpg"), IMREAD_COLOR)
  // We will need to scale results for display on the input image, we need its width and height
  val imageWidth = image.size(1)
  val imageHeight = image.size(0)

  // Convert image to format suitable for using with the net
  val inputBlob = blobFromImage(
    image, inScaleFactor, new Size(inWidth, inHeight), meanVal, false, false, CV_32F)

  // Set the network input
  net.setInput(inputBlob)

  // Make forward pass, compute output
  val detections = net.forward()

  println(s"Number of detections: ${detections.size(2)}")
  println(s" Considering only confidence above threshold: $confidenceThreshold")

  // Decode detected face locations
  val di = detections.createIndexer().asInstanceOf[FloatIndexer]
  val faceRegions = {
    for (i <- 0 until detections.size(2)) yield {
      val confidence = di.get(0, 0, i, 2)
      if (confidence > confidenceThreshold) {
        println(s"$i confidence = $confidence")

        val x1 = (di.get(0, 0, i, 3) * imageWidth).toInt
        val y1 = (di.get(0, 0, i, 4) * imageHeight).toInt
        val x2 = (di.get(0, 0, i, 5) * imageWidth).toInt
        val y2 = (di.get(0, 0, i, 6) * imageHeight).toInt

        Option(new Rect(new Point(x1, y1), new Point(x2, y2)))
      } else {
        None
      }
    }
  }.flatten

  println(s"Detected ${faceRegions.length} face regions")

  // Display detected face locations
  for (rect <- faceRegions) {
    rectangle(image, rect, markerColor)
  }
  show(image, "Face Detections")
}
