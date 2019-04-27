/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter11

import javax.swing.WindowConstants
import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacv._
import org.bytedeco.opencv.opencv_core._

/** Video processor.
  *
  * @param frameProcessor frame processing method, by default simply copies the input
  * @param displayInput   name for the window displaying input image.
  *                       If empty, input image will not be displayed.
  * @param displayOutput  name for the window displaying output image,
  *                       If empty, output image will not be displayed.
  */
class VideoProcessor(var frameProcessor: (Mat, Mat) => Unit = { (src, dest) => src.copyTo(dest) },
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

  private var _input: Option[String] = None
  private var _grabber: Option[FFmpegFrameGrabber] = None

  def input: String = _input.orNull

  def input_=(filename: String): Unit = {
    _grabber.foreach(_.release())
    _input = Option(filename)
    _grabber = Option(new FFmpegFrameGrabber(filename))
    grabber.start()
  }


  /** Frame rate property of the video input, */
  def frameRate: Double = grabber.getFrameRate


  /** Size of the video frame */
  def frameSize: Size = {

    // get size of from the grabber device
    val w = grabber.getImageWidth
    val h = grabber.getImageHeight

    new Size(w, h)
  }

  /** The codec of input video */
  def codec: Int = _grabber.get.getVideoCodec

  private var writerParam: Option[WriterParams] = None


  /** Set the output video file.
    *
    * By default the same parameters as input video will be used.
    */
  def setOutput(fileName: String, codec: Int = 0, frameRate: Double = 0.0, isColor: Boolean = true) {
    writerParam = Some(WriterParams(fileName, codec, frameRate, isColor))
  }

  private var _stop: Boolean = false

  def isStopped: Boolean = _stop

  /** to grab (and process) the frames of the sequence */
  def run() {

    val recorder = createRecorder()

    val inputCanvas = createCanvas(displayInput)
    val outputCanvas = createCanvas(displayOutput)

    // Capture, process, and display frames
    //    val inputFrame = new Mat()
    val outputFrame = new Mat()
    var frameNumber: Long = 0
    val frameConverter = new OpenCVFrameConverter.ToMat()
    for (frame <- Iterator.continually(grabber.grab()).takeWhile(_ != null)
         if !isStopped) {

      val inputFrame = frameConverter.convert(frame)

      // Display input frame, if canvas was created
      inputCanvas.foreach(_.showImage(toBufferedImage(inputFrame)))

      if (processFrames) {
        frameProcessor(inputFrame, outputFrame)
        frameNumber += 1
      } else {
        inputFrame.copyTo(outputFrame)
      }

      // write output sequence
      writeNextFrame(recorder, outputFrame)

      // Display output frame, if canvas was created
      outputCanvas.foreach(_.showImage(toBufferedImage(outputFrame)))

      // introduce a delay
      if (delay > 0) Thread.sleep(delay)

      // check if we should stop
      _stop = stopAtFrameNo >= 0 && frameNumber >= stopAtFrameNo
    }

    // Release writer (if created) to make sure that data is flushed to the output file, and file is closed.
    recorder.foreach(_.stop())
  }

  private def grabber: FFmpegFrameGrabber =
    _grabber.getOrElse(
      throw new Exception("Grabber not initialized. Did you set the input?")
    )

  /* Create canvas if its name is not empty */
  private def createCanvas(title: String): Option[CanvasFrame] =
    if (title != null && !title.isEmpty) {
      val canvas = new CanvasFrame(title, 1)
      canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      Some(canvas)
    } else None


  private def createRecorder(): Option[FrameRecorder] = writerParam.map { wp =>
    val recorder = new FFmpegFrameRecorder(wp.fileName, frameSize.width(), frameSize.height())
    val actualFrameRate = if (wp.frameRate == 0.0) frameRate else wp.frameRate
    recorder.setFrameRate(actualFrameRate)
    recorder.setVideoCodec(wp.codec)
    recorder.start()
    recorder
  }

  /** Write the output frame. */
  private def writeNextFrame(writer: Option[FrameRecorder], frame: Mat) {
    val converter = new OpenCVFrameConverter.ToIplImage()
    val f = converter.convert(frame)
    if (writer.isDefined) writer.foreach(_.record(f))
  }
}
