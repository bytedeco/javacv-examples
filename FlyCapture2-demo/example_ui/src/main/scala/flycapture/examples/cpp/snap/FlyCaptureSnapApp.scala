/*
 * Copyright (c) 2011-2021 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import com.typesafe.scalalogging.Logger
import org.apache.log4j.Level
import scalafx.Includes._
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene
import scalafx.scene.image.Image
import scalafxml.core.{DependenciesByType, FXMLView}

import java.io.IOException
import scala.reflect.runtime.universe.typeOf

/**
 * `FlyCaptureSnapApp` starts the FlyCaptureSnap application.
 *
 * First it will initialize FX, logging, and uncaught exception handling.
 * Then it will open the main application window `SnapView` and initialize its model.
 *
 * When application is closing, it will also attempt to close the SnapView model.
 *
 * @author Jarek Sacha
 */
object FlyCaptureSnapApp extends JFXApp3 {
  val title = "Fly Capture Snap"

  // Initialize logging before anything else, so logging in constructors is functional.
  private val logger = Logger(this.getClass)
  initializeLogging(Level.INFO)

  setupUncaughtExceptionHandling(logger, title)

  private var snapModel: Option[SnapModel] = None

  override def start(): Unit = {

    try {
      // Load main view
      val resourcePath = "SnapView.fxml"
      val resource     = getClass.getResource(resourcePath)
      if (resource == null) throw new IOException("Cannot load resource: '" + resourcePath + "'")

      snapModel = Option(new SnapModel())

      val root = FXMLView(resource, new DependenciesByType(Map(typeOf[SnapModel] -> snapModel.get)))

      // Create UI
      stage = new PrimaryStage() {
        title = "FlyCapture Snap Example"
        scene = new Scene(root) {
          icons += new Image("/flycapture/examples/cpp/snap/logo.png")
        }
      }

      snapModel.foreach(_.parent = stage)

      // Initialize camera connections
      // Use worker thread for non-UI operations
      new Thread(new javafx.concurrent.Task[Unit] {

        override def call(): Unit = {
          snapModel.foreach(_.initialize())
        }

        override def failed(): Unit = {
          showException(
            stage,
            title,
            "Unexpected error while initializing UI. Application will terminate.",
            getException
          )
          Platform.exit()
        }
      }).start()
    } catch {
      case t: Throwable =>
        logger.error("Unexpected error. Application will terminate.", t)
        showException(stage, title, "Unexpected error. Application will terminate.", t)
        Platform.exit()
    }
  }

  override def stopApp(): Unit = {
    snapModel.foreach(_.shutDown())
    super.stopApp()
  }
}
