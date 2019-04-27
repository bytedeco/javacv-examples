/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter01

import java.awt.Cursor._
import java.io.File

import javax.swing.ImageIcon
import org.bytedeco.javacv.{Java2DFrameConverter, OpenCVFrameConverter}
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

import scala.swing.Dialog.Message.Error
import scala.swing.FileChooser.Result.Approve
import scala.swing._


/**
 * The last section in the chapter 1 of the Cookbook demonstrates how to create a simple GUI application.
 *
 * The Cookbook is using Qt GUI Toolkit. This example is using Scala Swing to create an similar application.
 *
 * The application has two buttons on the left "Open Image" and "Process".
 * The opened image is displayed in the middle.
 * When "Process" button is pressed the image is flipped upside down and its red and blue channels are swapped.
 *
 * Unlike most of other examples in this module, this example is done the Scala way,
 * without regard to for direct porting to Java. A Java equivalent is in [[opencv_cookbook.chapter01.Ex2MyFirstGUIAppJava]].
 */
object Ex2MyFirstGUIApp extends SimpleSwingApplication {

  private lazy val fileChooser = new FileChooser(new File("."))

  def top: Frame = new MainFrame {
    title = "My First GUI Scala App"

    // Variable for holding loaded image
    var image: Option[Mat] = None


    //
    // Define actions
    //

    // Action performed when "Open Image" button is pressed
    val openImageAction = Action("Open Image") {
      cursor = getPredefinedCursor(WAIT_CURSOR)
      try {
        // Load image and update display. If new image was not loaded do nothing.
        openImage() match {
          case Some(x) =>
            val converter = new OpenCVFrameConverter.ToMat()
            val frame = converter.convert(x)
            val bi = new Java2DFrameConverter().convert(frame)
            image = Some(x)
            imageView.icon = new ImageIcon(bi)
            processAction.enabled = true
          case None =>
        }
      } finally {
        cursor = getPredefinedCursor(DEFAULT_CURSOR)
      }
    }

    // Action performed when "Process" button is pressed
    lazy val processAction = Action("Process") {
      cursor = getPredefinedCursor(WAIT_CURSOR)
      try {
        // Process and update image display if image is loaded
        image match {
          case Some(x) =>
            processImage(x)
            val converter = new OpenCVFrameConverter.ToMat()
            val frame = converter.convert(x)
            val bi = new Java2DFrameConverter().convert(frame)
            imageView.icon = new ImageIcon(bi)
          case None =>
            Dialog.showMessage(null, "Image not opened", title, Error)
        }
      } finally {
        cursor = getPredefinedCursor(DEFAULT_CURSOR)
      }
    }
    processAction.enabled = false

    //
    // Create UI
    //

    // Component for displaying the image
    lazy val imageView = new Label

    // Create button panel
    val buttonsPanel = new GridPanel(rows0 = 0, cols0 = 1) {
      contents += new Button(openImageAction)
      contents += new Button(processAction)
      vGap = 5
    }

    // Layout frame contents
    contents = new BorderPanel() {
      // Action buttons on the left
      add(new FlowPanel(buttonsPanel), BorderPanel.Position.West)
      // Image display in the center
      val imageScrollPane = new ScrollPane(imageView) {
        preferredSize = new Dimension(640, 480)
      }
      add(imageScrollPane, BorderPanel.Position.Center)
    }

    // Mark for display in the center of the screen
    centerOnScreen()
  }


  /** Ask user for location and open new image. */
  private def openImage(): Option[Mat] = {
    // Ask user for the location of the image file
    if (fileChooser.showOpenDialog(null) != Approve) {
      return None
    }

    // Load the image
    val path = fileChooser.selectedFile.getAbsolutePath
    val newImage = imread(path)
    if (!newImage.empty()) {
      Some(newImage)
    } else {
      Dialog.showMessage(null, "Cannot open image file: " + path, top.title, Error)
      None
    }
  }


  /** Process image in place.  */
  private def processImage(src: Mat) {
    // Flip upside down
    flip(src, src, 0)
    // Swap red and blue channels
    cvtColor(src, src, COLOR_BGR2RGB)
  }
}
