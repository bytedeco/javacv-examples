/*
 * Copyright (c) 2011-2021 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter01

import opencv_cookbook.OpenCVUtils.toFXImage
import org.bytedeco.opencv.global.opencv_core.flip
import org.bytedeco.opencv.global.opencv_imgcodecs.imread
import org.bytedeco.opencv.global.opencv_imgproc.{COLOR_BGR2RGB, cvtColor}
import org.bytedeco.opencv.opencv_core.Mat
import org.scalafx.extras.image.ImageDisplay
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.beans.property.ObjectProperty
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, Button}
import scalafx.scene.layout.{BorderPane, VBox}
import scalafx.stage.FileChooser

/**
 * The last section in the chapter 1 of the Cookbook demonstrates how to create a simple GUI application.
 *
 * The Cookbook is using Qt GUI Toolkit. This example is using ScalaFX/JavaFX to create an similar application.
 *
 * The application has two buttons on the left "Open Image" and "Process".
 * The opened image is displayed in the middle.
 * When "Process" button is pressed the image is flipped upside down and its red and blue channels are swapped.
 */
object Ex2MyFirstGUIFXApp extends JFXApp3 {

  // Variable for holding loaded image
  private val imageMat = ObjectProperty[Option[Mat]](None)

  // Image display component, it will be initialized in `start()`
  private var imageDisplay: ImageDisplay = _

  private val fileChooser = new FileChooser()

  override def start(): Unit = {

    imageDisplay = new ImageDisplay {
      zoomToFit.value = true
    }

    // Define UI layout and connect button actions
    stage = new JFXApp3.PrimaryStage {
      title = "My First GUI FX"
      scene = new Scene(640, 480) {
        root = new BorderPane() {
          left = new VBox {
            children = Seq(
              new Button("Open Image") {
                onAction = () => onOpenImage()
                maxWidth = Double.MaxValue
              },
              new Button("Process") {
                onAction = () => onProcess()
                disable <== imageMat.isEqualTo(None)
                maxWidth = Double.MaxValue
              }
            )
          }
          center = imageDisplay.view
        }
      }
    }
  }

  def onOpenImage(): Unit = {
    openImage().foreach { mat =>
      imageMat.value = Option(mat)
      imageDisplay.image.value = Option(toFXImage(mat))
    }
  }

  def onProcess(): Unit = {
    imageMat.value match {
      case Some(mat) =>
        processImage(mat)
        imageDisplay.image.value = Option(toFXImage(mat))
      case None =>
        new Alert(AlertType.Error) {
          initOwner(stage)
          title = "Process"
          headerText = "Image not opened"
        }.showAndWait()
    }
  }

  /** Ask user for location and open new image. */
  private def openImage(): Option[Mat] = {
    // Ask user for the location of the image file
    val file = fileChooser.showOpenDialog(stage)
    Option(file).flatMap { f =>
      // Load the image
      val path     = f.getCanonicalPath
      val newImage = imread(path)
      if (!newImage.empty()) {
        Option(newImage)
      } else {
        new Alert(AlertType.Error) {
          initOwner(stage)
          title = "Open Image"
          headerText = s"Cannot open image file: $path"
        }.showAndWait()
        None
      }
    }
  }

  /** Process image in place. */
  private def processImage(src: Mat): Unit = {
    // Flip upside down
    flip(src, src, 0)
    // Swap red and blue channels
    cvtColor(src, src, COLOR_BGR2RGB)
  }
}
