/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter03

import java.awt.Cursor._
import java.io.File

import javax.swing.WindowConstants
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import org.bytedeco.opencv.opencv_core._

import scala.swing.Dialog.Message.Error
import scala.swing.FileChooser.Result.Approve
import scala.swing._


/**
 * Example for sections "Using a controller to communicate with processing modules"
 * and "Using the Singleton design pattern" in Chapter 3.
 */
object Ex2ColorDetectorSimpleApplication extends SimpleSwingApplication {

  private lazy val fileChooser = new FileChooser(new File("./data"))
  private lazy val controller = ColorDetectorController

  def top = new MainFrame {
    title = "Color Detector"

    /**
     * Panel for holding action buttons.
     * Defines buttons and actions performed when buttons are pressed.
     */
    contents = new GridPanel(rows0 = 0, cols0 = 1) {

      var imageName = "?"

      // Action performed when "Open Image" button is pressed
      val openImageAction = Action("Open Image") {
        cursor = getPredefinedCursor(WAIT_CURSOR)
        try {
          // Ask user for the location of the image file
          if (fileChooser.showOpenDialog(this) == Approve) {
            // Load the image
            val path = fileChooser.selectedFile.getAbsolutePath
            if (controller.setInputImage(path)) {
              processAction.enabled = true
              imageName = path
              showImage("Input: " + imageName, controller.inputImage.get)
            } else {
              Dialog.showMessage(this, "Cannot open image file: " + path, title, Error)
            }
          }
        } finally {
          cursor = getPredefinedCursor(DEFAULT_CURSOR)
        }
      }

      // Action performed when "Process" button is pressed
      lazy val processAction = Action("Process") {
        cursor = getPredefinedCursor(WAIT_CURSOR)
        try {
          controller.process()
          showImage("Detected colors: " + imageName, controller.result.get)
        } finally {
          cursor = getPredefinedCursor(DEFAULT_CURSOR)
        }
      }
      processAction.enabled = false

      contents += new Button(openImageAction)
      contents += new Button(processAction)
      vGap = 5
    }


    // Mark for display in the center of the screen
    centerOnScreen()
  }

  /**
   * Display image in a window with given caption.
   */
  private def showImage(caption: String, image: Mat) {
    // Note that you need to indicate to CanvasFrame not to apply gamma correction,
    // by setting gamma to 1, otherwise the image will not look correct.
    val canvas = new CanvasFrame(caption, 1)
    canvas.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    val converter = new OpenCVFrameConverter.ToMat()
    canvas.showImage(converter.convert(image))
  }
}