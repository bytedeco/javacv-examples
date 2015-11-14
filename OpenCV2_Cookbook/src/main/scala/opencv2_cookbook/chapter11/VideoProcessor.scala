/*
 * Copyright (c) 2011-2015 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter11

import javax.swing.JFrame

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_videoio._
import org.bytedeco.javacv.CanvasFrame

/** Video processor.
  *
  * @param frameProcessor frame processing method, by default simply copies the input
  * @param displayInput name for the window displaying input image.
  *                     If empty, input image will not be displayed.
  * @param displayOutput name for the window displaying output image,
  *                      If empty, output image will not be displayed.
  */
class VideoProcessor(var frameProcessor: ((Mat, Mat) => Unit) = { (src, dest) => dest.copyTo(dest) },
                     var displayInput: String = "Input",
                     var displayOutput: String = "Output") {


  require(frameProcessor != null, "Argument `frameProcessor` cannot be `null`.")
  require(displayInput != null, "Argument `displayInput` cannot be `null`.")
  require(displayOutput != null, "Argument `displayOutput` cannot be `null`.")

  private case class WriterParams(fileName: String, codec: Int = 0, frameRate: Double = 0.0, isColor: Boolean = true)

  // If non-negative, stop at this frame number. If negative, no limits on frames.
  var stopAtFrameNo: Long = -1

  /** Delay between displaying input frames. */
  var delay: Long = 0


  /** If set to `false` input frames will not be processed, just copied to output. */
  var processFrames: Boolean = true

  private var _input  : Option[String]       = None
  private var _capture: Option[VideoCapture] = None

  def input: String = _input.orNull
  def input_=(v: String) = {
    _capture.foreach(_.release())
    _input = Option(v)
    _capture = Option(new VideoCapture(v))
  }


  /** Frame rate property of the video input, */
  def frameRate: Double = _capture.get.get(CAP_PROP_FPS)


  /** Size of the video frame */
  def frameSize: Size = {

    // get size of from the capture device
    val w = _capture.get.get(CAP_PROP_FRAME_WIDTH).toInt
    val h = _capture.get.get(CAP_PROP_FRAME_HEIGHT).toInt

    new Size(w, h)
  }


  /** The codec of input video */
  def codec: Int = _capture.get.get(CAP_PROP_FOURCC).toInt


  private var writerParam: Option[WriterParams] = None


  /** Set the output video file.
    *
    * By default the same parameters as input video will be used.
    */
  def setOutput(fileName: String, codec: Int = 0, frameRate: Double = 0.0, isColor: Boolean = true) {
    writerParam = Some(WriterParams(fileName, codec, frameRate, isColor))
  }

  def isOpened: Boolean = _capture.forall(_.isOpened)

  private var _stop: Boolean = false
  def isStopped: Boolean = _stop

  /** to grab (and process) the frames of the sequence */
  def run() {

    if (!isOpened) return

    val writer = createWriter()

    val inputCanvas = createCanvas(displayInput)
    val outputCanvas = createCanvas(displayOutput)

    // Capture, process, and display frames
    val inputFrame = new Mat()
    val outputFrame = new Mat()
    var frameNumber: Long = 0
    while (!isStopped && readNextFrame(inputFrame)) {

      // Display input frame, if canvas was created
      inputCanvas.foreach(_.showImage(toBufferedImage(inputFrame)))

      if (processFrames) {
        frameProcessor(inputFrame, outputFrame)
        frameNumber += 1
      } else {
        inputFrame.copyTo(outputFrame)
      }

      // write output sequence
      writeNextFrame(writer, outputFrame)

      // Display output frame, if canvas was created
      outputCanvas.foreach(_.showImage(toBufferedImage(outputFrame)))

      // introduce a delay
      if (delay > 0) Thread.sleep(delay)

      // check if we should stop
      _stop = stopAtFrameNo >= 0 && frameNumber >= stopAtFrameNo
    }

    // Release writer (if created) to make sure that data is flushed to the output file, and file is closed.
    writer.foreach(_.release())
  }

  /* Create canvas if its name is not empty */
  private def createCanvas(title: String): Option[CanvasFrame] =
    if (title != null && !title.isEmpty) {
      val canvas = new CanvasFrame(title, 1)
      canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
      Some(canvas)
    } else None


  private def createWriter(): Option[VideoWriter] = if (writerParam.isEmpty) {
    None
  } else {
    val writer = new VideoWriter()
    val wp = writerParam.get
    val actualFrameRate = if (writerParam.get.frameRate == 0.0) frameRate else writerParam.get.frameRate
    val actualCodec = if (writerParam.get.codec == 0) codec else writerParam.get.codec
    if (writer.open(wp.fileName, actualCodec, actualFrameRate, frameSize, writerParam.get.isColor)) {
      Some(writer)
    } else {
      None
    }
  }


  /** Get the next frame */
  private def readNextFrame(frame: Mat): Boolean = _capture.get.read(frame)

  /** Write the output frame. */
  private def writeNextFrame(writer: Option[VideoWriter], frame: Mat) {
    if (writer.isDefined) writer.foreach(_.write(frame))
  }
}
