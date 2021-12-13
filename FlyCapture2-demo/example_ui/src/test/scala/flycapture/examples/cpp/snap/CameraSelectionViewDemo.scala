/*
 * Copyright (c) 2011-2021 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import com.typesafe.scalalogging.Logger
import org.apache.log4j.Level
import org.bytedeco.flycapture.FlyCapture2._
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene

import scala.reflect.runtime.universe.typeOf

/**
 * @author Jarek Sacha
 */
object CameraSelectionViewDemo extends JFXApp3 {

  private val title  = "Fly Capture CameraSelectionView Demo"
  private val logger = Logger(this.getClass)
  initializeLogging(Level.INFO)
  setupUncaughtExceptionHandling(logger, title)

  override def start(): Unit = {
    try {
      val model = new CameraSelectionModel(new BusManager())

      val root = createFXMLView(model, typeOf[CameraSelectionModel], "CameraSelectionView.fxml")

      // Create UI
      stage = new PrimaryStage() {
        title = CameraSelectionViewDemo.title
        scene = new Scene(root)
      }

      // Initialize camera connections
      // Use worker thread for non-UI operations
      new Thread(new javafx.concurrent.Task[Unit] {

        override def call(): Unit = model.initialize()

        override def failed(): Unit = {
          super.failed()
          showException(
            stage,
            title,
            "Unexpected error when initializing UI. Application will terminate.",
            getException
          )
          Platform.exit()
        }
      }).start()
    } catch {
      case t: Throwable =>
        logger.error("Unexpected error. Application will terminate.", t)
        showException(null, title, "Unexpected error. Application will terminate.", t)

        Platform.exit()
    }
  }

}
