/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter10

import javax.swing.JFrame
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacv.CanvasFrame

/** Video processor.
  *
  * @param capture video input.
  * @param frameProcessor frame processing method, by default simply copies the input
  * @param displayInput name for the window displaying input image, it empty input image will not be displayed.
  * @param displayOutput name for the window displaying output image, it empty output image will not be displayed.
  */
class VideoProcessor(val capture: CvCapture,
                     var frameProcessor: ((IplImage) => IplImage) = {src => src},
                     var displayInput: String = "Input",
                     var displayOutput: String = "Output") {
    require(capture != null, "`capture` cannot be `null`.")


    private case class WriterParams(fileName: String, codec: Int = 0, frameRate: Double = 0.0, isColor: Boolean = true)


    /** Delay between displaying input frames. */
    var delay: Long = 0


    /** If set to `false` input frames will not be processed, just copied to output. */
    var processFrames: Boolean = true


    /** Frame rate property of the video input, */
    def frameRate: Double = cvGetCaptureProperty(capture, CV_CAP_PROP_FPS)


    /** Size of the video frame */
    def frameSize: CvSize = {

        // get size of from the capture device
        val w = cvGetCaptureProperty(capture, CV_CAP_PROP_FRAME_WIDTH).toInt
        val h = cvGetCaptureProperty(capture, CV_CAP_PROP_FRAME_HEIGHT).toInt

      cvSize(w, h)
    }


    /** The codec of input video */
    def codec: Int = cvGetCaptureProperty(capture, CV_CAP_PROP_FOURCC).toInt


    private var writerParam: Option[WriterParams] = None


    /** Set the output video file.
      *
      * By default the same parameters as input video will be used.
      */
    def setOutput(fileName: String, codec: Int = 0, frameRate: Double = 0.0, isColor: Boolean = true) {
        writerParam = Some(WriterParams(fileName, codec, frameRate, isColor))
    }


    /** to grab (and process) the frames of the sequence */
    def run() {

        val writer = createWriter()

        // Create canvas if its name is not empty
        def createCanvas(title: String): Option[CanvasFrame] = if (title != null && !title.isEmpty) {
            val canvas = new CanvasFrame(title, 1)
            canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
            Some(canvas)
        } else None
        val inputCanvas = createCanvas(displayInput)
        val outputCanvas = createCanvas(displayOutput)

        // Capture, process, and display frames
        var frame: IplImage = null
        while ( {frame = readNextFrame(); frame} != null) {

            // Display input frame, if canvas was created
            inputCanvas.foreach(_.showImage(frame))

            // calling the process function or method
            val output = if (processFrames) frameProcessor(frame) else frame

            // write output sequence
            writeNextFrame(writer, output)

            // Display output frame, if canvas was created
            outputCanvas.foreach(_.showImage(output))

            // introduce a delay
            if (delay > 0) Thread.sleep(delay)
        }

        // Release writer (if created) to make sure that data is flushed to the output file, and file is closed.
        writer.foreach(cvReleaseVideoWriter)
    }

    private def createWriter(): Option[CvVideoWriter] = if (writerParam.isEmpty) {
        None
    } else {
        val actualFrameRate = if (writerParam.get.frameRate == 0.0) frameRate else writerParam.get.frameRate
        val actualCodec = if (writerParam.get.codec == 0) codec else writerParam.get.codec
        Some(cvCreateVideoWriter(writerParam.get.fileName, // filename
            actualCodec, // codec to be used
            actualFrameRate, // frame rate of the video !
            frameSize, // frame size
            if (writerParam.get.isColor) 1 else 0) // color video?
        )
    }


    /** Get the next frame */
    private def readNextFrame(): IplImage = if (cvGrabFrame(capture) != 0) cvRetrieveFrame(capture) else null


    /** Write the output frame. */
    private def writeNextFrame(writer: Option[CvVideoWriter], frame: IplImage) {
        if (writer.isDefined && cvWriteFrame(writer.get, frame) == 0) throw new Exception("Video writing failed.")
    }
}
